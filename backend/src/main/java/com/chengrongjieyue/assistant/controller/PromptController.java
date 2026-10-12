package com.chengrongjieyue.assistant.controller;

import com.chengrongjieyue.assistant.audit.Audit;
import com.chengrongjieyue.assistant.common.Result;
import com.chengrongjieyue.assistant.dto.PageQuery;
import com.chengrongjieyue.assistant.dto.PromptGenerateRequest;
import com.chengrongjieyue.assistant.security.SecurityUtils;
import com.chengrongjieyue.assistant.service.ConversationService;
import com.chengrongjieyue.assistant.service.PromptService;
import com.chengrongjieyue.assistant.vo.ConversationDetailVO;
import com.chengrongjieyue.assistant.vo.ConversationVersionVO;
import com.chengrongjieyue.assistant.vo.ConversationVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/prompt")
@RequiredArgsConstructor
public class PromptController {

    private final PromptService promptService;
    private final ConversationService conversationService;

    /**
     * SSE 流式生成：事件 message（增量）/ done（会话与版本信息）/ error
     */
    @PostMapping(value = "/generate", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Audit(action = "prompt_generate", target = "conversation")
    public SseEmitter generate(@Valid @RequestBody PromptGenerateRequest request) {
        SseEmitter emitter = new SseEmitter(300_000L);
        promptService.generateStream(SecurityUtils.current().getId(), request.input(), emitter);
        return emitter;
    }

    @GetMapping("/history")
    public Result<IPage<ConversationVO>> history(PageQuery query) {
        return Result.success(conversationService.page(SecurityUtils.current().getId(), "prompt", query));
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
