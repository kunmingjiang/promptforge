package com.chengrongjieyue.assistant.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.chengrongjieyue.assistant.common.Result;
import com.chengrongjieyue.assistant.dto.PageQuery;
import com.chengrongjieyue.assistant.security.SecurityUtils;
import com.chengrongjieyue.assistant.service.ConversationService;
import com.chengrongjieyue.assistant.vo.ConversationDetailVO;
import com.chengrongjieyue.assistant.vo.ConversationVO;
import com.chengrongjieyue.assistant.vo.ConversationVersionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;

    @GetMapping
    public Result<IPage<ConversationVO>> list(
            PageQuery query, @RequestParam(required = false) String module) {
        return Result.success(conversationService.page(SecurityUtils.current().getId(), module, query));
    }

    @GetMapping("/{id}")
    public Result<ConversationDetailVO> detail(@PathVariable Long id) {
        return Result.success(conversationService.detail(SecurityUtils.current().getId(), id));
    }

    @GetMapping("/{id}/versions")
    public Result<List<ConversationVersionVO>> versions(@PathVariable Long id) {
        return Result.success(conversationService.versions(SecurityUtils.current().getId(), id));
    }
}
