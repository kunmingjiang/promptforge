package com.chengrongjieyue.assistant.vo;

import java.time.LocalDateTime;
import java.util.List;

public record RequirementDocumentVO(
        Long projectId,
        Integer version,
        String content,
        List<DocumentVersionVO> versions
) {

    public record DocumentVersionVO(
            Long versionId,
            Integer version,
            LocalDateTime createdAt
    ) {
    }
}
