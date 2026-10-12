package com.chengrongjieyue.assistant.service;

import com.chengrongjieyue.assistant.mapper.ConversationMapper;
import com.chengrongjieyue.assistant.mapper.MessageMapper;
import com.chengrongjieyue.assistant.mapper.RequirementProjectMapper;
import com.chengrongjieyue.assistant.mapper.UserMapper;
import com.chengrongjieyue.assistant.vo.StatsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 管理后台只读统计：仅 count 查询，不返回任何业务明细
 */
@Service
@RequiredArgsConstructor
public class StatsService {

    private final UserMapper userMapper;
    private final ConversationMapper conversationMapper;
    private final MessageMapper messageMapper;
    private final RequirementProjectMapper requirementProjectMapper;

    public StatsVO stats() {
        return new StatsVO(
                safe(userMapper.selectCount(null)),
                safe(conversationMapper.selectCount(null)),
                safe(messageMapper.selectCount(null)),
                safe(requirementProjectMapper.selectCount(null)));
    }

    private long safe(Long count) {
        return count == null ? 0L : count;
    }
}
