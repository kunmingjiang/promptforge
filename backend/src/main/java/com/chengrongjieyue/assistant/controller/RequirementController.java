package com.chengrongjieyue.assistant.controller;

import com.chengrongjieyue.assistant.audit.Audit;
import com.chengrongjieyue.assistant.common.Result;
import com.chengrongjieyue.assistant.dto.RequirementAnswersRequest;
import com.chengrongjieyue.assistant.dto.RequirementCreateRequest;
import com.chengrongjieyue.assistant.security.SecurityUtils;
import com.chengrongjieyue.assistant.service.RequirementService;
import com.chengrongjieyue.assistant.vo.RequirementDocumentVO;
import com.chengrongjieyue.assistant.vo.RequirementProjectVO;
import com.chengrongjieyue.assistant.vo.RequirementQuestionVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/requirement")
@RequiredArgsConstructor
public class RequirementController {

    private final RequirementService requirementService;

    @PostMapping("/projects")
    @Audit(action = "requirement_create", target = "requirement_project")
    public Result<RequirementProjectVO> create(@Valid @RequestBody RequirementCreateRequest request) {
        return Result.success(requirementService.create(SecurityUtils.current().getId(), request));
    }

    @GetMapping("/projects/{id}/questions")
    public Result<List<RequirementQuestionVO>> questions(@PathVariable Long id) {
        return Result.success(requirementService.questions(SecurityUtils.current().getId(), id));
    }

    @PostMapping("/projects/{id}/answers")
    public Result<List<RequirementQuestionVO>> submitAnswers(
            @PathVariable Long id, @Valid @RequestBody RequirementAnswersRequest request) {
        return Result.success(requirementService.submitAnswers(SecurityUtils.current().getId(), id, request));
    }

    @PostMapping(value = "/projects/{id}/document", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Audit(action = "requirement_document_generate", target = "requirement_document")
    public SseEmitter generateDocument(@PathVariable Long id) {
        SseEmitter emitter = new SseEmitter(300_000L);
        requirementService.generateDocument(SecurityUtils.current().getId(), id, emitter);
        return emitter;
    }

    @GetMapping("/projects/{id}/document")
    public Result<RequirementDocumentVO> document(
            @PathVariable Long id, @RequestParam(required = false) Integer version) {
        return Result.success(requirementService.document(SecurityUtils.current().getId(), id, version));
    }
}
