package com.chengrongjieyue.assistant.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chengrongjieyue.assistant.common.BizException;
import com.chengrongjieyue.assistant.entity.Conversation;
import com.chengrongjieyue.assistant.entity.ConversationVersion;
import com.chengrongjieyue.assistant.entity.Message;
import com.chengrongjieyue.assistant.entity.ModelConfig;
import com.chengrongjieyue.assistant.llm.LlmMessage;
import com.chengrongjieyue.assistant.llm.LlmProvider;
import com.chengrongjieyue.assistant.llm.LlmProviderFactory;
import com.chengrongjieyue.assistant.llm.LlmRequest;
import com.chengrongjieyue.assistant.llm.LlmStreamCallback;
import com.chengrongjieyue.assistant.mapper.ConversationMapper;
import com.chengrongjieyue.assistant.mapper.ConversationVersionMapper;
import com.chengrongjieyue.assistant.mapper.MessageMapper;
import com.chengrongjieyue.assistant.ratelimit.LlmRateLimitService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * 提示词生成：组装系统提示词 → 流式调用模型 → 完成后落消息与版本快照。
 * SSE 推送与业务持久化全部发生在独立线程池，不占用容器工作线程
 */
@Slf4j
@Service
public class PromptService {

    private static final String MODULE = "prompt";

    private static final String SYSTEM_PROMPT = """
            你是一名资深的 AI 提示词工程师。用户会给你一段模糊的需求或想法，
            你的任务是把它加工成一段高质量、结构化、可直接复制使用的提示词（Prompt）。
            输出要求：
            1. 只输出提示词本身，不要任何解释或寒暄；
            2. 提示词需包含：角色设定、任务目标、输入说明、输出格式、约束条件；
            3. 使用简体中文。
            """;

    private final ConversationMapper conversationMapper;
    private final MessageMapper messageMapper;
    private final ConversationVersionMapper versionMapper;
    private final ModelConfigService modelConfigService;
    private final LlmProviderFactory providerFactory;
    private final LlmRateLimitService rateLimitService;
    private final ObjectMapper objectMapper;
    private final Executor llmExecutor;

    public PromptService(ConversationMapper conversationMapper,
                         MessageMapper messageMapper,
                         ConversationVersionMapper versionMapper,
                         ModelConfigService modelConfigService,
                         LlmProviderFactory providerFactory,
                         LlmRateLimitService rateLimitService,
                         ObjectMapper objectMapper,
                         @Qualifier("llmExecutor") Executor llmExecutor) {
        this.conversationMapper = conversationMapper;
        this.messageMapper = messageMapper;
        this.versionMapper = versionMapper;
        this.modelConfigService = modelConfigService;
        this.providerFactory = providerFactory;
        this.rateLimitService = rateLimitService;
        this.objectMapper = objectMapper;
        this.llmExecutor = llmExecutor;
    }

    public void generateStream(Long userId, String input, SseEmitter emitter) {
        if (!rateLimitService.tryAcquire(userId)) {
            sendError(emitter, "请求过于频繁，每分钟最多发起 10 次生成，请稍后再试");
            emitter.complete();
            return;
        }
        ModelConfig config;
        try {
            config = modelConfigService.getActive();
        } catch (BizException e) {
            sendError(emitter, e.getMessage());
            emitter.complete();
            return;
        }

        Conversation conversation = new Conversation();
        conversation.setUserId(userId);
        conversation.setTitle(abbreviate(input));
        conversation.setModule(MODULE);
        conversationMapper.insert(conversation);

        Message userMessage = new Message();
        userMessage.setConversationId(conversation.getId());
        userMessage.setRole("user");
        userMessage.setContent(input);
        messageMapper.insert(userMessage);

        LlmProvider provider = providerFactory.get(config.getProvider());
        LlmRequest llmRequest = modelConfigService.buildLlmRequest(config,
                List.of(new LlmMessage("system", SYSTEM_PROMPT), new LlmMessage("user", input)), true);
        Long conversationId = conversation.getId();

        llmExecutor.execute(() -> provider.chatStream(llmRequest, new LlmStreamCallback() {
            @Override
            public void onChunk(String delta) {
                send(emitter, "message", Map.of("delta", delta));
            }

            @Override
            public void onComplete(String fullContent) {
                persistAssistant(conversationId, fullContent);
                Integer version = saveVersion(conversationId);
                Map<String, Object> done = new HashMap<>();
                done.put("conversationId", conversationId);
                done.put("version", version);
                send(emitter, "done", done);
                emitter.complete();
            }

            @Override
            public void onError(String message) {
                log.warn("提示词生成中断: {}", message);
                sendError(emitter, message);
                emitter.complete();
            }
        }));
    }

    private void persistAssistant(Long conversationId, String content) {
        if (content == null || content.isBlank()) {
            return;
        }
        Message assistant = new Message();
        assistant.setConversationId(conversationId);
        assistant.setRole("assistant");
        assistant.setContent(content);
        messageMapper.insert(assistant);
    }

    private Integer saveVersion(Long conversationId) {
        try {
            List<Message> messages = messageMapper.selectList(new LambdaQueryWrapper<Message>()
                    .eq(Message::getConversationId, conversationId)
                    .orderByAsc(Message::getId));
            List<Map<String, String>> simplified = messages.stream()
                    .map(m -> Map.of("role", m.getRole(), "content", m.getContent()))
                    .toList();
            String snapshot = objectMapper.writeValueAsString(Map.of("messages", simplified));
            Long count = versionMapper.selectCount(new LambdaQueryWrapper<ConversationVersion>()
                    .eq(ConversationVersion::getConversationId, conversationId));
            ConversationVersion version = new ConversationVersion();
            version.setConversationId(conversationId);
            version.setVersion((int) (count == null ? 1 : count + 1));
            version.setSnapshot(snapshot);
            versionMapper.insert(version);
            return version.getVersion();
        } catch (Exception e) {
            log.error("保存会话版本失败: {}", e.toString());
            return null;
        }
    }

    private String abbreviate(String input) {
        String compact = input.strip().replaceAll("\\s+", " ");
        return compact.length() > 40 ? compact.substring(0, 40) + "..." : compact;
    }

    private void send(SseEmitter emitter, String event, Object data) {
        try {
            emitter.send(SseEmitter.event().name(event).data(data, MediaType.APPLICATION_JSON));
        } catch (Exception e) {
            // 客户端提前断开属于常态，静默结束本次推送
        }
    }

    private void sendError(SseEmitter emitter, String message) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("message", message);
        send(emitter, "error", payload);
    }
}
