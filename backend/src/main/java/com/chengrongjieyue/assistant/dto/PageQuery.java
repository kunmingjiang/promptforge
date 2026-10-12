package com.chengrongjieyue.assistant.dto;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * 分页参数：默认 20 条，上限 100 条，防止一次拉取过多数据
 */
public record PageQuery(Integer page, Integer size) {

    public <T> Page<T> toPage() {
        long current = (page == null || page < 1) ? 1 : page;
        long limit = (size == null || size < 1) ? 20 : Math.min(size, 100);
        return new Page<>(current, limit);
    }
}
