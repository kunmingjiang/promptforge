package com.chengrongjieyue.assistant.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chengrongjieyue.assistant.common.BizException;
import com.chengrongjieyue.assistant.common.ErrorCode;
import com.chengrongjieyue.assistant.crypto.AesEncryptor;
import com.chengrongjieyue.assistant.dto.ModelConfigTestRequest;
import com.chengrongjieyue.assistant.dto.ModelConfigUpsertRequest;
import com.chengrongjieyue.assistant.entity.ModelConfig;
import com.chengrongjieyue.assistant.llm.LlmMessage;
import com.chengrongjieyue.assistant.llm.LlmProviderFactory;
import com.chengrongjieyue.assistant.llm.LlmRequest;
import com.chengrongjieyue.assistant.llm.LlmResponse;
import com.chengrongjieyue.assistant.mapper.ModelConfigMapper;
import com.chengrongjieyue.assistant.vo.ModelConfigTestResult;
import com.chengrongjieyue.assistant.vo.ModelConfigVO;
import com.chengrongjieyue.assistant.vo.ModelStatusVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ModelConfigService {

    private final ModelConfigMapper modelConfigMapper;
    private final AesEncryptor aesEncryptor;
    private final LlmProviderFactory providerFactory;
    private final int timeoutSeconds;

    public ModelConfigService(ModelConfigMapper modelConfigMapper,
                              AesEncryptor aesEncryptor,
                              LlmProviderFactory providerFactory,
                              @Value("${app.llm.timeout-seconds}") int timeoutSeconds) {
        this.modelConfigMapper = modelConfigMapper;
        this.aesEncryptor = aesEncryptor;
        this.providerFactory = providerFactory;
        this.timeoutSeconds = timeoutSeconds;
    }

    /**
     * 取当前生效（已启用）的模型配置；未配置时抛出可读的业务异常
     */
    public ModelConfig getActive() {
        ModelConfig config = modelConfigMapper.selectOne(new LambdaQueryWrapper<ModelConfig>()
                .eq(ModelConfig::getEnabled, true)
                .orderByDesc(ModelConfig::getId)
                .last("LIMIT 1"));
        if (config == null) {
            throw new BizException(ErrorCode.CONFIG_MISSING,
                    "管理员尚未启用任何模型配置，请先在管理后台完成配置");
        }
        return config;
    }

    public ModelConfigVO get() {
        ModelConfig config = latest();
        return config == null ? null : toVO(config);
    }

    /**
     * 登录用户可见的模型状态摘要：生成页/需求页据此判断是否需要现场引导配置
     */
    public ModelStatusVO status() {
        ModelConfig config = latest();
        if (config == null) {
            return new ModelStatusVO(false, false, "deepseek", "", "尚未配置任何模型，需要先填写 Base URL 与 API Key");
        }
        if (config.getApiKeyEncrypted() == null || config.getApiKeyEncrypted().isBlank()) {
            return new ModelStatusVO(false, config.getEnabled(), config.getProvider(), config.getModelName(),
                    "模型信息已填写，但还缺少 API Key");
        }
        if (!Boolean.TRUE.equals(config.getEnabled())) {
            return new ModelStatusVO(false, false, config.getProvider(), config.getModelName(),
                    "模型配置存在但未启用，启用后即可使用");
        }
        return new ModelStatusVO(true, true, config.getProvider(), config.getModelName(), "模型就绪");
    }

    public ModelConfigVO update(ModelConfigUpsertRequest request) {
        ModelConfig config = latest();
        if (config == null) {
            config = new ModelConfig();
        }
        config.setProvider(request.provider());
        config.setBaseUrl(normalizeBaseUrl(request.baseUrl()));
        config.setModelName(request.modelName());
        config.setEnabled(Boolean.TRUE.equals(request.enabled()));
        if (request.apiKey() != null && !request.apiKey().isBlank()) {
            config.setApiKeyEncrypted(aesEncryptor.encrypt(request.apiKey().trim()));
        }
        config.setUpdatedAt(LocalDateTime.now());
        if (config.getId() == null) {
            modelConfigMapper.insert(config);
        } else {
            modelConfigMapper.updateById(config);
        }
        return toVO(config);
    }

    public ModelConfigTestResult test(ModelConfigTestRequest request) {
        long start = System.currentTimeMillis();
        try {
            LlmRequest llmRequest = new LlmRequest(
                    normalizeBaseUrl(request.baseUrl()), request.apiKey().trim(), request.modelName(),
                    List.of(new LlmMessage("user", "ping")), 0.1, 8, false, 30);
            LlmResponse response = providerFactory.get(request.provider()).chat(llmRequest);
            boolean hasReply = response.content() != null && !response.content().isBlank();
            return new ModelConfigTestResult(hasReply,
                    hasReply ? "连接成功，模型已正常响应" : "连接成功，但模型返回内容为空",
                    System.currentTimeMillis() - start);
        } catch (BizException e) {
            return new ModelConfigTestResult(false, e.getMessage(), System.currentTimeMillis() - start);
        }
    }

    public LlmRequest buildLlmRequest(ModelConfig config, List<LlmMessage> messages, boolean stream) {
        return new LlmRequest(
                config.getBaseUrl(),
                aesEncryptor.decrypt(config.getApiKeyEncrypted()),
                config.getModelName(),
                messages, 0.7, null, stream, timeoutSeconds);
    }

    private ModelConfig latest() {
        return modelConfigMapper.selectOne(new LambdaQueryWrapper<ModelConfig>()
                .orderByDesc(ModelConfig::getId)
                .last("LIMIT 1"));
    }

    private ModelConfigVO toVO(ModelConfig config) {
        return new ModelConfigVO(config.getId(), config.getProvider(), config.getBaseUrl(),
                config.getModelName(), config.getEnabled(), mask(config.getApiKeyEncrypted()),
                config.getUpdatedAt());
    }

    private String mask(String encryptedKey) {
        if (encryptedKey == null || encryptedKey.isBlank()) {
            return "";
        }
        try {
            String plain = aesEncryptor.decrypt(encryptedKey);
            return plain.length() <= 8 ? "****" : "****" + plain.substring(plain.length() - 4);
        } catch (Exception e) {
            return "****";
        }
    }

    private String normalizeBaseUrl(String baseUrl) {
        String trimmed = baseUrl == null ? "" : baseUrl.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }
}
