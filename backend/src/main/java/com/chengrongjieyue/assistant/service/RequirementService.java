package com.chengrongjieyue.assistant.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chengrongjieyue.assistant.common.BizException;
import com.chengrongjieyue.assistant.common.ErrorCode;
import com.chengrongjieyue.assistant.dto.RequirementAnswersRequest;
import com.chengrongjieyue.assistant.dto.RequirementAnswerItem;
import com.chengrongjieyue.assistant.dto.RequirementCreateRequest;
import com.chengrongjieyue.assistant.entity.Conversation;
import com.chengrongjieyue.assistant.entity.ConversationVersion;
import com.chengrongjieyue.assistant.entity.Message;
import com.chengrongjieyue.assistant.entity.ModelConfig;
import com.chengrongjieyue.assistant.entity.RequirementAnswer;
import com.chengrongjieyue.assistant.entity.RequirementDocument;
import com.chengrongjieyue.assistant.entity.RequirementDocumentVersion;
import com.chengrongjieyue.assistant.entity.RequirementProject;
import com.chengrongjieyue.assistant.llm.LlmMessage;
import com.chengrongjieyue.assistant.llm.LlmProvider;
import com.chengrongjieyue.assistant.llm.LlmProviderFactory;
import com.chengrongjieyue.assistant.llm.LlmRequest;
import com.chengrongjieyue.assistant.llm.LlmResponse;
import com.chengrongjieyue.assistant.llm.LlmStreamCallback;
import com.chengrongjieyue.assistant.mapper.ConversationMapper;
import com.chengrongjieyue.assistant.mapper.ConversationVersionMapper;
import com.chengrongjieyue.assistant.mapper.MessageMapper;
import com.chengrongjieyue.assistant.mapper.RequirementAnswerMapper;
import com.chengrongjieyue.assistant.mapper.RequirementDocumentMapper;
import com.chengrongjieyue.assistant.mapper.RequirementDocumentVersionMapper;
import com.chengrongjieyue.assistant.mapper.RequirementProjectMapper;
import com.chengrongjieyue.assistant.vo.RequirementDocumentVO;
import com.chengrongjieyue.assistant.vo.RequirementProjectVO;
import com.chengrongjieyue.assistant.vo.RequirementQuestionVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * 需求助手三步链：提交需求 → 生成最多 5 个澄清问题 → 生成 Markdown 需求文档。
 * 问题生成走同步调用（需要 JSON 解析），文档生成走 SSE 流式
 */
@Slf4j
@Service
public class RequirementService {

    private static final int MAX_QUESTIONS = 5;

    private static final String CLARIFY_SYSTEM_PROMPT = """
            你是需求澄清助手。用户会给出一段原始需求，请判断要产出一份高质量需求文档还缺少哪些关键信息。
            只输出一个 JSON 字符串数组，最多 5 个问题，不要输出数组以外的任何内容。
            示例：["这个工具面向哪些用户？","是否需要账号登录功能？"]
            """;

    private static final String DOCUMENT_SYSTEM_PROMPT = """
            你是一名资深需求分析师。请根据用户提供的原始需求与澄清问答，输出一份结构化的 Markdown 需求文档。
            文档需包含：项目概述、目标用户、功能需求（编号列表）、非功能需求、边界与限制、验收标准。
            未被回答的问题按合理默认假设处理，并在文档末尾列出这些假设。
            只输出 Markdown 内容，不要额外说明。
            """;

    private final RequirementProjectMapper projectMapper;
    private final RequirementAnswerMapper answerMapper;
    private final RequirementDocumentMapper documentMapper;
    private final RequirementDocumentVersionMapper documentVersionMapper;
    private final ConversationMapper conversationMapper;
    private final MessageMapper messageMapper;
    private final ConversationVersionMapper conversationVersionMapper;
    private final ModelConfigService modelConfigService;
    private final LlmProviderFactory providerFactory;
    private final ObjectMapper objectMapper;
    private final Executor llmExecutor;

    public RequirementService(RequirementProjectMapper projectMapper,
                              RequirementAnswerMapper answerMapper,
                              RequirementDocumentMapper documentMapper,
                              RequirementDocumentVersionMapper documentVersionMapper,
                              ConversationMapper conversationMapper,
                              MessageMapper messageMapper,
                              ConversationVersionMapper conversationVersionMapper,
                              ModelConfigService modelConfigService,
                              LlmProviderFactory providerFactory,
                              ObjectMapper objectMapper,
                              @Qualifier("llmExecutor") Executor llmExecutor) {
        this.projectMapper = projectMapper;
        this.answerMapper = answerMapper;
        this.documentMapper = documentMapper;
        this.documentVersionMapper = documentVersionMapper;
        this.conversationMapper = conversationMapper;
        this.messageMapper = messageMapper;
        this.conversationVersionMapper = conversationVersionMapper;
        this.modelConfigService = modelConfigService;
        this.providerFactory = providerFactory;
        this.objectMapper = objectMapper;
        this.llmExecutor = llmExecutor;
    }

    public RequirementProjectVO create(Long userId, RequirementCreateRequest request) {
        RequirementProject project = new RequirementProject();
        project.setUserId(userId);
        project.setTitle(request.title());
        project.setRawRequirement(request.rawRequirement());
        project.setStatus("PENDING");
        projectMapper.insert(project);
        return new RequirementProjectVO(project.getId(), project.getTitle(), project.getStatus(),
                project.getCreatedAt());
    }

    public List<RequirementQuestionVO> questions(Long userId, Long projectId) {
        RequirementProject project = owned(projectId, userId);
        List<RequirementAnswer> existing = answerMapper.selectList(new LambdaQueryWrapper<RequirementAnswer>()
                .eq(RequirementAnswer::getProjectId, projectId)
                .orderByAsc(RequirementAnswer::getId));
        if (!existing.isEmpty()) {
            return toQuestionVOs(existing);
        }

        ModelConfig config = modelConfigService.getActive();
        LlmProvider provider = providerFactory.get(config.getProvider());
        LlmRequest llmRequest = modelConfigService.buildLlmRequest(config,
                List.of(new LlmMessage("system", CLARIFY_SYSTEM_PROMPT),
                        new LlmMessage("user", "原始需求：" + project.getRawRequirement())),
                false);
        LlmResponse response = provider.chat(llmRequest);
        List<String> questions = parseQuestions(response.content());
        if (questions.isEmpty()) {
            throw new BizException(ErrorCode.LLM_ERROR, "澄清问题生成失败，请重试");
        }

        List<RequirementAnswer> answers = new ArrayList<>();
        for (String question : questions) {
            RequirementAnswer answer = new RequirementAnswer();
            answer.setProjectId(projectId);
            answer.setQuestion(question);
            answer.setSkipped(false);
            answerMapper.insert(answer);
            answers.add(answer);
        }
        project.setStatus("CLARIFYING");
        projectMapper.updateById(project);
        return toQuestionVOs(answers);
    }

    public List<RequirementQuestionVO> submitAnswers(Long userId, Long projectId,
                                                     RequirementAnswersRequest request) {
        RequirementProject project = owned(projectId, userId);
        for (RequirementAnswerItem item : request.answers()) {
            RequirementAnswer answer = answerMapper.selectById(item.answerId());
            if (answer == null || !answer.getProjectId().equals(projectId)) {
                throw new BizException(ErrorCode.FORBIDDEN, "回答与项目不匹配");
            }
            answer.setAnswer(item.answer());
            answer.setSkipped(Boolean.TRUE.equals(item.skipped())
                    || item.answer() == null || item.answer().isBlank());
            answerMapper.updateById(answer);
        }
        project.setStatus("ANSWERED");
        projectMapper.updateById(project);
        return getQuestions(projectId);
    }

    public void generateDocument(Long userId, Long projectId, SseEmitter emitter) {
        RequirementProject project;
        try {
            project = owned(projectId, userId);
        } catch (BizException e) {
            sendError(emitter, e.getMessage());
            emitter.complete();
            return;
        }
        List<RequirementAnswer> answers = answerMapper.selectList(new LambdaQueryWrapper<RequirementAnswer>()
                .eq(RequirementAnswer::getProjectId, projectId)
                .orderByAsc(RequirementAnswer::getId));
        if (answers.isEmpty()) {
            sendError(emitter, "请先生成并回答澄清问题，再生成需求文档");
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

        StringBuilder qa = new StringBuilder();
        for (RequirementAnswer answer : answers) {
            qa.append("问题：").append(answer.getQuestion()).append('\n');
            qa.append("回答：").append(Boolean.TRUE.equals(answer.getSkipped())
                    ? "（用户未回答）" : answer.getAnswer()).append("\n\n");
        }
        String userPrompt = "原始需求：\n" + project.getRawRequirement() + "\n\n澄清问答：\n" + qa;

        LlmProvider provider = providerFactory.get(config.getProvider());
        LlmRequest llmRequest = modelConfigService.buildLlmRequest(config,
                List.of(new LlmMessage("system", DOCUMENT_SYSTEM_PROMPT),
                        new LlmMessage("user", userPrompt)), true);
        Long pid = project.getId();

        Long uid = userId;
        llmExecutor.execute(() -> provider.chatStream(llmRequest, new LlmStreamCallback() {
            @Override
            public void onChunk(String delta) {
                send(emitter, "message", Map.of("delta", delta));
            }

            @Override
            public void onComplete(String fullContent) {
                Integer version = saveDocument(pid, fullContent);
                RequirementProject latest = projectMapper.selectById(pid);
                if (latest != null) {
                    latest.setStatus("DOCUMENTED");
                    projectMapper.updateById(latest);
                    saveConversationRecord(uid, latest, fullContent);
                }
                Map<String, Object> done = new HashMap<>();
                done.put("projectId", pid);
                done.put("version", version);
                send(emitter, "done", done);
                emitter.complete();
            }

            @Override
            public void onError(String message) {
                log.warn("需求文档生成中断: {}", message);
                sendError(emitter, message);
                emitter.complete();
            }
        }));
    }

    public RequirementDocumentVO document(Long userId, Long projectId, Integer version) {
        owned(projectId, userId);
        RequirementDocument doc = documentMapper.selectOne(new LambdaQueryWrapper<RequirementDocument>()
                .eq(RequirementDocument::getProjectId, projectId));
        if (doc == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "尚未生成需求文档");
        }
        Integer viewVersion = doc.getVersion();
        String content = doc.getContent();
        if (version != null && !version.equals(doc.getVersion())) {
            RequirementDocumentVersion target = documentVersionMapper.selectOne(
                    new LambdaQueryWrapper<RequirementDocumentVersion>()
                            .eq(RequirementDocumentVersion::getDocumentId, doc.getId())
                            .eq(RequirementDocumentVersion::getVersion, version));
            if (target == null) {
                throw new BizException(ErrorCode.NOT_FOUND, "文档版本不存在");
            }
            viewVersion = target.getVersion();
            content = target.getContent();
        }
        List<RequirementDocumentVO.DocumentVersionVO> versions = documentVersionMapper.selectList(
                        new LambdaQueryWrapper<RequirementDocumentVersion>()
                                .eq(RequirementDocumentVersion::getDocumentId, doc.getId())
                                .orderByDesc(RequirementDocumentVersion::getVersion))
                .stream()
                .map(v -> new RequirementDocumentVO.DocumentVersionVO(v.getId(), v.getVersion(), v.getCreatedAt()))
                .toList();
        return new RequirementDocumentVO(projectId, viewVersion, content, versions);
    }

    private RequirementProject owned(Long projectId, Long userId) {
        RequirementProject project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "需求项目不存在");
        }
        if (!project.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权访问该需求项目");
        }
        return project;
    }

    private List<RequirementQuestionVO> getQuestions(Long projectId) {
        return toQuestionVOs(answerMapper.selectList(new LambdaQueryWrapper<RequirementAnswer>()
                .eq(RequirementAnswer::getProjectId, projectId)
                .orderByAsc(RequirementAnswer::getId)));
    }

    private List<RequirementQuestionVO> toQuestionVOs(List<RequirementAnswer> answers) {
        return answers.stream()
                .map(a -> new RequirementQuestionVO(a.getId(), a.getQuestion(), a.getAnswer(), a.getSkipped()))
                .toList();
    }

    private List<String> parseQuestions(String content) {
        if (content == null || content.isBlank()) {
            return List.of();
        }
        String text = content.strip();
        int start = text.indexOf('[');
        int end = text.lastIndexOf(']');
        if (start < 0 || end <= start) {
            return List.of();
        }
        try {
            List<String> questions = objectMapper.readValue(
                    text.substring(start, end + 1), new TypeReference<List<String>>() {
                    });
            return questions.stream()
                    .filter(q -> q != null && !q.isBlank())
                    .limit(MAX_QUESTIONS)
                    .toList();
        } catch (Exception e) {
            log.warn("澄清问题 JSON 解析失败: {}", e.toString());
            return List.of();
        }
    }

    private Integer saveDocument(Long projectId, String content) {
        RequirementDocument doc = documentMapper.selectOne(new LambdaQueryWrapper<RequirementDocument>()
                .eq(RequirementDocument::getProjectId, projectId));
        int nextVersion;
        if (doc == null) {
            doc = new RequirementDocument();
            doc.setProjectId(projectId);
            nextVersion = 1;
        } else {
            nextVersion = doc.getVersion() + 1;
        }
        doc.setContent(content);
        doc.setVersion(nextVersion);
        if (doc.getId() == null) {
            documentMapper.insert(doc);
        } else {
            documentMapper.updateById(doc);
        }
        RequirementDocumentVersion version = new RequirementDocumentVersion();
        version.setDocumentId(doc.getId());
        version.setVersion(nextVersion);
        version.setContent(content);
        documentVersionMapper.insert(version);
        return nextVersion;
    }

    /**
     * 需求文档同步归档一份会话记录，让「对话历史」页能统一回看两个模块
     */
    private void saveConversationRecord(Long userId, RequirementProject project, String document) {
        try {
            Conversation conversation = new Conversation();
            conversation.setUserId(userId);
            conversation.setTitle("[需求] " + project.getTitle());
            conversation.setModule("requirement");
            conversationMapper.insert(conversation);

            Message userMessage = new Message();
            userMessage.setConversationId(conversation.getId());
            userMessage.setRole("user");
            userMessage.setContent("需求：" + project.getRawRequirement());
            messageMapper.insert(userMessage);

            Message assistantMessage = new Message();
            assistantMessage.setConversationId(conversation.getId());
            assistantMessage.setRole("assistant");
            assistantMessage.setContent(document);
            messageMapper.insert(assistantMessage);

            List<Message> messages = List.of(userMessage, assistantMessage);
            List<Map<String, String>> simplified = messages.stream()
                    .map(m -> Map.of("role", m.getRole(), "content", m.getContent()))
                    .toList();
            ConversationVersion version = new ConversationVersion();
            version.setConversationId(conversation.getId());
            version.setVersion(1);
            version.setSnapshot(objectMapper.writeValueAsString(Map.of("messages", simplified)));
            conversationVersionMapper.insert(version);
        } catch (Exception e) {
            log.error("需求会话归档失败: {}", e.toString());
        }
    }

    private void send(SseEmitter emitter, String event, Object data) {
        try {
            emitter.send(SseEmitter.event().name(event).data(data, MediaType.APPLICATION_JSON));
        } catch (Exception e) {
            // 客户端断开时静默处理
        }
    }

    private void sendError(SseEmitter emitter, String message) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("message", message);
        send(emitter, "error", payload);
    }
}
