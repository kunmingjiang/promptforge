package com.chengrongjieyue.assistant.llm;

import com.chengrongjieyue.assistant.common.BizException;
import com.chengrongjieyue.assistant.common.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAI 兼容协议通用适配器：DeepSeek / 智谱 GLM / Kimi(Moonshot) / MiniMax
 * 及任意 OpenAI 兼容网关（OneAPI、vLLM、中转站等）共用此实现。
 * 流式走 /chat/completions SSE，每次调用按请求携带的 Base URL 现场构建客户端，
 * 支持后台随时切换供应商与网关地址
 */
@Component
@RequiredArgsConstructor
public class OpenAiCompatProvider implements LlmProvider {

    private static final String CHAT_PATH = "/chat/completions";
    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int ERROR_SNIPPET_LENGTH = 300;

    /** 各家协议一致，同一实现按别名响应多个 provider 标识 */
    private static final List<String> ALIASES =
            List.of("deepseek", "glm", "zhipu", "kimi", "moonshot", "minimax", "custom");

    private final ObjectMapper objectMapper;

    @Override
    public String name() {
        return "openai-compat";
    }

    @Override
    public List<String> aliases() {
        return ALIASES;
    }

    @Override
    public LlmResponse chat(LlmRequest request) {
        String raw;
        try {
            raw = client(request).post()
                    .uri(CHAT_PATH)
                    .header(HttpHeaders.AUTHORIZATION, bearer(request.apiKey()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(buildBody(request, false))
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException e) {
            throw new BizException(ErrorCode.LLM_ERROR,
                    "模型返回 HTTP " + e.getStatusCode().value() + "：" + snippet(e.getResponseBodyAsString()));
        } catch (ResourceAccessException e) {
            throw new BizException(ErrorCode.LLM_ERROR, "连接模型服务失败（网络异常或超时）");
        }
        try {
            JsonNode root = objectMapper.readTree(raw);
            String content = root.path("choices").path(0).path("message").path("content").asText("");
            JsonNode tokens = root.path("usage").path("total_tokens");
            return new LlmResponse(content, tokens.isInt() ? tokens.asInt() : null);
        } catch (Exception e) {
            throw new BizException(ErrorCode.LLM_ERROR, "模型响应解析失败");
        }
    }

    @Override
    public void chatStream(LlmRequest request, LlmStreamCallback callback) {
        StringBuilder full = new StringBuilder();
        try {
            client(request).post()
                    .uri(CHAT_PATH)
                    .header(HttpHeaders.AUTHORIZATION, bearer(request.apiKey()))
                    .header(HttpHeaders.ACCEPT, MediaType.TEXT_EVENT_STREAM_VALUE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(buildBody(request, true))
                    .exchange((req, resp) -> {
                        if (resp.getStatusCode().isError()) {
                            String error = new String(resp.getBody().readAllBytes(), StandardCharsets.UTF_8);
                            callback.onError("模型返回 HTTP " + resp.getStatusCode().value()
                                    + "：" + snippet(error));
                            return null;
                        }
                        try (BufferedReader reader = new BufferedReader(
                                new InputStreamReader(resp.getBody(), StandardCharsets.UTF_8))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                if (!line.startsWith("data:")) {
                                    continue;
                                }
                                String payload = line.substring(5).trim();
                                if ("[DONE]".equals(payload)) {
                                    break;
                                }
                                JsonNode node = objectMapper.readTree(payload);
                                String delta = node.path("choices").path(0)
                                        .path("delta").path("content").asText("");
                                if (!delta.isEmpty()) {
                                    full.append(delta);
                                    callback.onChunk(delta);
                                }
                            }
                        }
                        callback.onComplete(full.toString());
                        return null;
                    });
        } catch (BizException e) {
            callback.onError(e.getMessage());
        } catch (Exception e) {
            if (full.length() > 0) {
                // 中途断流但已有部分内容：按已生成内容收尾，避免用户白等
                callback.onComplete(full.toString());
                return;
            }
            callback.onError("连接模型服务失败：" + e.getMessage());
        }
    }

    private RestClient client(LlmRequest request) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        factory.setReadTimeout(request.timeoutSeconds() * 1000);
        return RestClient.builder()
                .baseUrl(request.baseUrl())
                .requestFactory(factory)
                .build();
    }

    private Map<String, Object> buildBody(LlmRequest request, boolean stream) {
        List<Map<String, String>> messages = request.messages().stream()
                .map(m -> Map.of("role", m.role(), "content", m.content()))
                .toList();
        Map<String, Object> body = new HashMap<>();
        body.put("model", request.model());
        body.put("messages", messages);
        body.put("stream", stream);
        if (request.temperature() != null) {
            body.put("temperature", request.temperature());
        }
        if (request.maxTokens() != null) {
            body.put("max_tokens", request.maxTokens());
        }
        return body;
    }

    private String bearer(String apiKey) {
        return "Bearer " + apiKey;
    }

    private String snippet(String text) {
        if (text == null) {
            return "";
        }
        String compact = text.replaceAll("\\s+", " ").trim();
        return compact.length() > ERROR_SNIPPET_LENGTH
                ? compact.substring(0, ERROR_SNIPPET_LENGTH) + "..."
                : compact;
    }
}
