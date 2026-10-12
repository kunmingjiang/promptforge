package com.chengrongjieyue.assistant.controller;

import com.chengrongjieyue.assistant.common.Result;
import com.chengrongjieyue.assistant.service.ModelConfigService;
import com.chengrongjieyue.assistant.vo.ModelStatusVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 模型状态：登录即可查看（不含密钥），用于业务页内嵌的配置引导弹框。
 * 写操作仍收敛在 /api/admin/**，普通用户只读。
 */
@RestController
@RequestMapping("/api/model")
@RequiredArgsConstructor
public class ModelStatusController {

    private final ModelConfigService modelConfigService;

    @GetMapping("/status")
    public Result<ModelStatusVO> status() {
        return Result.success(modelConfigService.status());
    }
}
