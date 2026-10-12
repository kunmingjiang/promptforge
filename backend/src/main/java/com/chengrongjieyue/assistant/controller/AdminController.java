package com.chengrongjieyue.assistant.controller;

import com.chengrongjieyue.assistant.audit.Audit;
import com.chengrongjieyue.assistant.common.Result;
import com.chengrongjieyue.assistant.dto.ModelConfigTestRequest;
import com.chengrongjieyue.assistant.dto.ModelConfigUpsertRequest;
import com.chengrongjieyue.assistant.service.ModelConfigService;
import com.chengrongjieyue.assistant.service.StatsService;
import com.chengrongjieyue.assistant.vo.ModelConfigTestResult;
import com.chengrongjieyue.assistant.vo.ModelConfigVO;
import com.chengrongjieyue.assistant.vo.StatsVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理后台：路径级 hasRole("ADMIN") 保护，只开放模型配置与只读统计
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final ModelConfigService modelConfigService;
    private final StatsService statsService;

    @GetMapping("/model-config")
    public Result<ModelConfigVO> getModelConfig() {
        return Result.success(modelConfigService.get());
    }

    @PutMapping("/model-config")
    @Audit(action = "update_model_config", target = "model_config")
    public Result<ModelConfigVO> updateModelConfig(@Valid @RequestBody ModelConfigUpsertRequest request) {
        return Result.success(modelConfigService.update(request));
    }

    @PostMapping("/model-config/test")
    public Result<ModelConfigTestResult> testModelConfig(@Valid @RequestBody ModelConfigTestRequest request) {
        return Result.success(modelConfigService.test(request));
    }

    @GetMapping("/stats")
    public Result<StatsVO> stats() {
        return Result.success(statsService.stats());
    }
}
