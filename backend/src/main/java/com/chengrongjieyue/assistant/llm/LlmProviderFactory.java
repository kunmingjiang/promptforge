package com.chengrongjieyue.assistant.llm;

import com.chengrongjieyue.assistant.common.BizException;
import com.chengrongjieyue.assistant.common.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 适配器注册表：Spring 自动注入所有 LlmProvider 实现，
 * 后台切换 provider 只需改配置，业务代码零改动
 */
@Component
public class LlmProviderFactory {

    private final Map<String, LlmProvider> providers;

    public LlmProviderFactory(List<LlmProvider> providerList) {
        this.providers = new HashMap<>();
        for (LlmProvider provider : providerList) {
            providers.put(provider.name(), provider);
            // 别名指向同一实现：deepseek/glm/kimi/minimax/custom → OpenAI 兼容适配器
            for (String alias : provider.aliases()) {
                providers.putIfAbsent(alias, provider);
            }
        }
    }

    public LlmProvider get(String provider) {
        LlmProvider target = providers.get(provider);
        if (target == null) {
            throw new BizException(ErrorCode.CONFIG_MISSING, "暂不支持的模型类型：" + provider);
        }
        return target;
    }
}
