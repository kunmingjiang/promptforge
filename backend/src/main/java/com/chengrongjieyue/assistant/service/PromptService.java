package com.chengrongjieyue.assistant.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chengrongjieyue.assistant.common.BizException;
import com.chengrongjieyue.assistant.common.ErrorCode;
import com.chengrongjieyue.assistant.dto.PromptOptimizeRequest;
import com.chengrongjieyue.assistant.dto.PromptClarifyRequest;
import com.chengrongjieyue.assistant.dto.PromptGenerateRequest;
import com.chengrongjieyue.assistant.dto.PromptSelfCheckRequest;
import com.chengrongjieyue.assistant.dto.PromptTestRequest;
import com.chengrongjieyue.assistant.dto.AgentKitRequest;
import com.chengrongjieyue.assistant.entity.Conversation;
import com.chengrongjieyue.assistant.entity.ConversationVersion;
import com.chengrongjieyue.assistant.entity.Message;
import com.chengrongjieyue.assistant.entity.ModelConfig;
import com.chengrongjieyue.assistant.llm.LlmMessage;
import com.chengrongjieyue.assistant.llm.LlmProvider;
import com.chengrongjieyue.assistant.llm.LlmProviderFactory;
import com.chengrongjieyue.assistant.llm.LlmRequest;
import com.chengrongjieyue.assistant.llm.LlmResponse;
import com.chengrongjieyue.assistant.llm.LlmStreamCallback;
import com.chengrongjieyue.assistant.mapper.ConversationMapper;
import com.chengrongjieyue.assistant.mapper.ConversationVersionMapper;
import com.chengrongjieyue.assistant.mapper.MessageMapper;
import com.chengrongjieyue.assistant.ratelimit.LlmRateLimitService;
import com.chengrongjieyue.assistant.vo.AgentKitVO;
import com.chengrongjieyue.assistant.vo.PromptTestResult;
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

    private static final String SELF_CHECK_PROMPT = """
            你是一名严格的 AI 提示词评审专家。你会拿到一条提示词、一段测试输入和对应的试运行输出。
            你的职责是客观评审这条提示词的实际效果，重点是：
            1. 判断输出是否达成了提示词设定的任务目标；
            2. 诚实列出你（作为执行该提示词的 AI）不确定、靠假设补齐、或缺少背景信息而无法判断的点，交给人工确认；
            3. 指出输出缺陷并给出提示词层面的修改建议。
            不要粉饰，发现问题直说；输出使用简体中文 Markdown。
            """;

    /** 用户附图时的补充指令：流程图 / 草图 / 设计稿要读进提示词里 */
    private static final String VISION_APPENDIX = """
            4. 用户可能随文附上图片（流程图、草图、界面设计稿、表格截图等）：
               先仔细读图，把图中体现的业务流程、模块结构、字段与交互纳入提示词；
               图与文字冲突时以文字为准，并在提示词的约束条件里注明你的取舍。
            """;

    private static final String CLARIFY_PROMPT = """
            你是一名资深的 AI 提示词工程师。用户给你的需求描述可能非常零散、口语化，
            甚至他自己也说不清楚想要什么。你的任务是找出「生成高质量提示词所必需、
            但用户没有讲清楚」的关键信息，提出最多 3 个澄清问题。
            要求：
            1. 只问最关键的，能靠合理默认值补齐的不要问；
            2. 每个问题一句话，具体、好回答，必要时给出选项示例；
            3. 只输出一个 JSON 字符串数组，例如 ["问题1","问题2"]，不要输出其他任何内容。
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

    public void generateStream(Long userId, PromptGenerateRequest request, SseEmitter emitter) {
        String input = request.input();
        List<String> images = sanitizeImages(request.images());
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
                List.of(new LlmMessage("system", images.isEmpty() ? SYSTEM_PROMPT : SYSTEM_PROMPT + VISION_APPENDIX),
                        new LlmMessage("user", input, images)), true);
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

    /**
     * 澄清梳理：需求零散、用户自己也说不清时，让 AI 先提最多 3 个关键澄清问题，
     * 用户回答后再带着完整信息去生成。不落库，轻量临时调用
     */
    public List<String> clarify(Long userId, PromptClarifyRequest request) {
        if (!rateLimitService.tryAcquire(userId)) {
            throw new BizException(ErrorCode.RATE_LIMITED, "请求过于频繁，请稍后再试");
        }
        ModelConfig config = modelConfigService.getActive();
        List<String> images = sanitizeImages(request.images());
        LlmRequest llmRequest = modelConfigService.buildLlmRequest(config,
                List.of(new LlmMessage("system", CLARIFY_PROMPT),
                        new LlmMessage("user", request.input(), images)), false);
        LlmResponse response = providerFactory.get(config.getProvider()).chat(llmRequest);
        return parseQuestions(response.content());
    }

    /** 解析模型返回的澄清问题：优先按 JSON 数组解析，失败则按行拆分兜底 */
    private List<String> parseQuestions(String content) {
        if (content == null || content.isBlank()) {
            return List.of();
        }
        String text = content.trim();
        try {
            int start = text.indexOf('[');
            int end = text.lastIndexOf(']');
            if (start >= 0 && end > start) {
                List<String> questions = objectMapper.readValue(text.substring(start, end + 1),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
                return questions.stream().filter(q -> q != null && !q.isBlank()).limit(3).toList();
            }
        } catch (Exception ignored) {
            // 落入按行兜底
        }
        return java.util.Arrays.stream(text.split("\n"))
                .map(line -> line.replaceAll("^[\\s\\d.、\\-*>]+", "").trim())
                .filter(line -> line.endsWith("？") || line.endsWith("?") || line.contains("？"))
                .limit(3)
                .toList();
    }

    /** 图片白名单校验：仅 data URL，单张 ≤ 4MB，最多 3 张 */
    private List<String> sanitizeImages(List<String> images) {
        if (images == null || images.isEmpty()) {
            return List.of();
        }
        if (images.size() > 3) {
            throw new BizException(ErrorCode.PARAM_ERROR, "最多上传 3 张图片");
        }
        for (String image : images) {
            if (image == null || !image.startsWith("data:image/") || image.length() > 6 * 1024 * 1024) {
                throw new BizException(ErrorCode.PARAM_ERROR, "仅支持 4MB 以内的 png / jpg / webp 图片");
            }
        }
        return images;
    }

    /**
     * 试运行：把生成的提示词当作系统指令，喂一段测试输入，看真实模型效果。
     * 不落库——试运行只是临时验证，审计由切面记录
     */
    public PromptTestResult testRun(Long userId, PromptTestRequest request) {
        ModelConfig config = modelConfigService.getActive();
        LlmRequest llmRequest = modelConfigService.buildLlmRequest(config,
                List.of(new LlmMessage("system", request.prompt()),
                        new LlmMessage("user", request.testInput())), false);
        long start = System.currentTimeMillis();
        LlmResponse response = providerFactory.get(config.getProvider()).chat(llmRequest);
        return new PromptTestResult(response.content(), System.currentTimeMillis() - start);
    }

    /**
     * AI 自检：以独立评审者身份检查「提示词 + 测试输入 + 试运行输出」，
     * 输出达标判断、AI 不确定需人工确认的点、以及改进建议。
     * 提示词自身无法发现自己缺失的背景信息，这部分如实列为待人工确认项
     */
    public PromptTestResult selfCheck(Long userId, PromptSelfCheckRequest request) {
        ModelConfig config = modelConfigService.getActive();
        StringBuilder user = new StringBuilder();
        user.append("【提示词】\n").append(request.prompt()).append("\n\n");
        if (request.testInput() != null && !request.testInput().isBlank()) {
            user.append("【测试输入】\n").append(request.testInput()).append("\n\n");
        }
        user.append("【试运行输出】\n").append(request.output()).append("\n\n");
        user.append("""
                请以上述材料评审这条提示词的实际效果，严格按以下结构输出（Markdown）：
                ## 总体判断
                一句话：输出是否达到提示词预期的任务目标（达标 / 基本达标但有缺陷 / 未达标），并说明理由。
                ## 需人工确认的点
                列出你作为 AI 在理解提示词或产出结果时**不确定、依赖假设、或缺少背景信息**的地方，每条格式：
                - [不确定点] 我的假设是 xxx —— 请人工确认是否正确
                如果没有不确定点，写「无」。
                ## 缺陷与改进建议
                列出输出中的具体缺陷（遗漏、幻觉、格式偏差等）与对应提示词修改建议；没有则写「无」。
                只输出评审内容，不要寒暄。""");

        LlmRequest llmRequest = modelConfigService.buildLlmRequest(config,
                List.of(new LlmMessage("system", SELF_CHECK_PROMPT),
                        new LlmMessage("user", user.toString())), false);
        long start = System.currentTimeMillis();
        LlmResponse response = providerFactory.get(config.getProvider()).chat(llmRequest);
        return new PromptTestResult(response.content(), System.currentTimeMillis() - start);
    }

    /**
     * 优化迭代：当前版本提示词 + 试运行暴露的问题 + 用户反馈 → 流式生成下一版。
     * 新版本作为同会话的后续消息落库，并打新的会话版本快照，可随时回看/回滚
     */
    public void optimizeStream(Long userId, PromptOptimizeRequest request, SseEmitter emitter) {
        if (!rateLimitService.tryAcquire(userId)) {
            sendError(emitter, "请求过于频繁，每分钟最多发起 10 次生成，请稍后再试");
            emitter.complete();
            return;
        }
        Conversation conversation;
        try {
            conversation = conversationMapper.selectById(request.conversationId());
            if (conversation == null) {
                throw new BizException(ErrorCode.NOT_FOUND, "会话不存在");
            }
            if (!conversation.getUserId().equals(userId)) {
                throw new BizException(ErrorCode.FORBIDDEN, "无权访问该会话");
            }
        } catch (BizException e) {
            sendError(emitter, e.getMessage());
            emitter.complete();
            return;
        }
        String currentPrompt = latestAssistant(conversation.getId());
        if (currentPrompt == null) {
            sendError(emitter, "该会话还没有可优化的提示词");
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

        String optimizeInstruction = buildOptimizeInstruction(
                currentPrompt, request.feedback(), request.testInput(), request.testOutput());

        Message userMessage = new Message();
        userMessage.setConversationId(conversation.getId());
        userMessage.setRole("user");
        userMessage.setContent("[优化迭代] " + request.feedback());
        messageMapper.insert(userMessage);

        LlmProvider provider = providerFactory.get(config.getProvider());
        LlmRequest llmRequest = modelConfigService.buildLlmRequest(config,
                List.of(new LlmMessage("system", OPTIMIZE_SYSTEM_PROMPT),
                        new LlmMessage("user", optimizeInstruction)), true);
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
                log.warn("提示词优化中断: {}", message);
                sendError(emitter, message);
                emitter.complete();
            }
        }));
    }

    /** 标记定稿：客户确认当前版本满意，作为最终交付 */
    public void finalize(Long userId, Long conversationId) {
        Conversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "会话不存在");
        }
        if (!conversation.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权访问该会话");
        }
        conversation.setFinalized(true);
        conversationMapper.updateById(conversation);
    }

    /**
     * 智能体上架套件：定稿提示词 + 目标平台 → 开场白（AI 生成）+ 真实平台创建步骤 + 调用示例。
     * 各智能体平台没有公开的"创建智能体"API，因此交付的是可直接照做的创建材料包
     */
    public AgentKitVO agentKit(Long userId, AgentKitRequest request) {
        Platform platform = PLATFORMS.get(request.platform());
        if (platform == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "不支持的平台，可选：" + String.join(" / ", PLATFORMS.keySet()));
        }
        String greeting = request.greeting();
        if (greeting == null || greeting.isBlank()) {
            greeting = generateGreeting(request.agentName(), request.prompt());
        }
        String callExample = null;
        if ("custom".equals(request.platform())) {
            callExample = """
                    curl -X POST "{你的模型服务地址}/chat/completions" \\
                      -H "Authorization: Bearer {你的 API Key}" \\
                      -H "Content-Type: application/json" \\
                      -d '{
                        "model": "{模型名}",
                        "messages": [
                          {"role": "system", "content": "<把左侧系统提示词粘贴到这里>"},
                          {"role": "user", "content": "用户输入"}
                        ]
                      }'""";
        }
        return new AgentKitVO(request.agentName(), request.prompt(), greeting,
                request.platform(), platform.label(), platform.site(), platform.steps(), platform.tip(), callExample);
    }

    private static final String OPTIMIZE_SYSTEM_PROMPT = """
            你是一名资深的 AI 提示词工程师。你会拿到：一段现成的提示词、它在试运行中的表现（可选）、以及用户的优化反馈。
            你的任务是输出改进后的新版本提示词。要求：
            1. 只输出新提示词本身，不要解释改动点，不要寒暄；
            2. 保留原版本中已经有效的部分，针对性修复用户指出的问题；
            3. 新版本必须在结构（角色/目标/输入/输出格式/约束）上完整自洽；
            4. 使用简体中文。
            """;

    private String buildOptimizeInstruction(String currentPrompt, String feedback, String testInput, String testOutput) {
        StringBuilder sb = new StringBuilder();
        sb.append("【当前提示词】\n").append(currentPrompt).append("\n\n");
        if (testInput != null && !testInput.isBlank()) {
            sb.append("【试运行输入】\n").append(testInput).append("\n\n");
        }
        if (testOutput != null && !testOutput.isBlank()) {
            sb.append("【试运行输出】\n").append(testOutput).append("\n\n");
        }
        sb.append("【优化反馈】\n").append(feedback);
        return sb.toString();
    }

    private String generateGreeting(String agentName, String prompt) {
        try {
            ModelConfig config = modelConfigService.getActive();
            LlmRequest llmRequest = modelConfigService.buildLlmRequest(config,
                    List.of(new LlmMessage("system", "你是智能体文案助手。为智能体生成一句开场白：说明它能帮用户做什么，语气友好专业，40 字以内，只输出开场白本身。"),
                            new LlmMessage("user", "智能体名称：" + agentName + "\n它的提示词职责如下：\n" + prompt)), false);
            LlmResponse response = providerFactory.get(config.getProvider()).chat(llmRequest);
            if (response.content() != null && !response.content().isBlank()) {
                return response.content().strip();
            }
        } catch (Exception e) {
            log.warn("开场白生成失败，使用默认文案: {}", e.toString());
        }
        return "你好，我是「" + agentName + "」，告诉我你的需求，我来帮你完成。";
    }

    private String latestAssistant(Long conversationId) {
        List<Message> messages = messageMapper.selectList(new LambdaQueryWrapper<Message>()
                .eq(Message::getConversationId, conversationId)
                .eq(Message::getRole, "assistant")
                .orderByDesc(Message::getId)
                .last("LIMIT 1"));
        return messages.isEmpty() ? null : messages.get(0).getContent();
    }

    /** 智能体平台登记表：真实可用的创建入口与步骤（上架引导，非 API 集成） */
    private record Platform(String label, String site, List<String> steps, String tip) {
    }

    private static final Map<String, Platform> PLATFORMS = Map.of(
            "coze", new Platform("扣子 Coze", "https://www.coze.cn", List.of(
                    "打开 coze.cn 并登录，进入工作台",
                    "点「创建」→「创建智能体」，填写名称与简介",
                    "把系统提示词完整粘贴到「人设与回复逻辑」输入框",
                    "右侧预览区实测几轮对话，确认行为符合预期",
                    "点「发布」，按需勾选扣子商店、微信客服等渠道"),
                    "国内平台，免费额度充足，支持一键发布到微信/飞书等渠道，推荐首发"),
            "dify", new Platform("Dify", "https://cloud.dify.ai", List.of(
                    "打开 cloud.dify.ai（或你的私有化 Dify 控制台）并登录",
                    "点「创建空白应用」→ 选「聊天助手」",
                    "把系统提示词粘贴到「编排」页的「系统提示词」框",
                    "右上角调试预览确认效果后，点「发布」→「运行」",
                    "如需 API 接入：发布后在「访问 API」页拿到密钥"),
                    "适合团队私有化部署与 API 深度集成，开源可自建"),
            "gpts", new Platform("ChatGPT GPTs", "https://chatgpt.com/gpts/editor", List.of(
                    "打开 chatgpt.com/gpts/editor（需 ChatGPT Plus/Team）",
                    "点「Create a GPT」，在 Configure 页填写名称与描述",
                    "把系统提示词完整粘贴到「Instructions」框",
                    "右侧预览实测后，点「Create」保存并按需公开",
                    "注意：Instructions 建议用英文提示词时效果更稳，可让我先翻译"),
                    "面向海外用户与 ChatGPT 生态，支持 GPT 商店分发"),
            "yuanqi", new Platform("腾讯元器", "https://yuanqi.tencent.com", List.of(
                    "打开 yuanqi.tencent.com 并用微信/QQ 登录",
                    "点「创建智能体」，填写名称与头像",
                    "把系统提示词粘贴到「设定」（详细设定）输入框",
                    "右侧调试预览确认后，点「发布」",
                    "可发布到腾讯元器商店、微信生态等渠道"),
                    "微信生态分发顺畅，适合面向 C 端用户的轻量智能体"),
            "custom", new Platform("自建 API 接入", "", List.of(
                    "把系统提示词作为 messages 里 role=system 的内容",
                    "通过 OpenAI 兼容接口（/chat/completions）调用任意供应商模型",
                    "下方调用示例可直接改造接入你自己的业务系统",
                    "用户侧无需任何配置，提示词完全由你的服务端掌控"),
                    "最灵活：提示词留在你的服务端，前端/小程序/企业系统都能接"));

    private void persistAssistant(Long conversationId, String content) {        if (content == null || content.isBlank()) {
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
