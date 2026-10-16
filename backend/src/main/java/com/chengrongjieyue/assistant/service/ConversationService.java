package com.chengrongjieyue.assistant.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chengrongjieyue.assistant.common.BizException;
import com.chengrongjieyue.assistant.common.ErrorCode;
import com.chengrongjieyue.assistant.dto.PageQuery;
import com.chengrongjieyue.assistant.entity.Conversation;
import com.chengrongjieyue.assistant.entity.ConversationVersion;
import com.chengrongjieyue.assistant.entity.Message;
import com.chengrongjieyue.assistant.mapper.ConversationMapper;
import com.chengrongjieyue.assistant.mapper.ConversationVersionMapper;
import com.chengrongjieyue.assistant.mapper.MessageMapper;
import com.chengrongjieyue.assistant.vo.ConversationDetailVO;
import com.chengrongjieyue.assistant.vo.ConversationVO;
import com.chengrongjieyue.assistant.vo.ConversationVersionVO;
import com.chengrongjieyue.assistant.vo.MessageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationMapper conversationMapper;
    private final MessageMapper messageMapper;
    private final ConversationVersionMapper versionMapper;

    public IPage<ConversationVO> page(Long userId, String module, PageQuery query) {
        LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<Conversation>()
                .eq(Conversation::getUserId, userId)
                .eq(module != null && !module.isBlank(), Conversation::getModule, module)
                .orderByDesc(Conversation::getCreatedAt);
        Page<Conversation> page = conversationMapper.selectPage(query.toPage(), wrapper);
        return page.convert(c -> new ConversationVO(c.getId(), c.getTitle(), c.getModule(), c.getCreatedAt()));
    }

    public ConversationDetailVO detail(Long userId, Long conversationId) {
        Conversation conversation = owned(conversationId, userId);
        List<Message> messages = messageMapper.selectList(new LambdaQueryWrapper<Message>()
                .eq(Message::getConversationId, conversationId)
                .orderByAsc(Message::getId)
                .last("LIMIT 200"));
        List<MessageVO> messageVOs = messages.stream()
                .map(m -> new MessageVO(m.getId(), m.getRole(), m.getContent(), m.getCreatedAt()))
                .toList();
        return new ConversationDetailVO(conversation.getId(), conversation.getTitle(),
                conversation.getModule(), conversation.getFinalized(), conversation.getCreatedAt(), messageVOs);
    }

    public List<ConversationVersionVO> versions(Long userId, Long conversationId) {
        owned(conversationId, userId);
        return versionMapper.selectList(new LambdaQueryWrapper<ConversationVersion>()
                        .eq(ConversationVersion::getConversationId, conversationId)
                        .orderByDesc(ConversationVersion::getVersion))
                .stream()
                .map(v -> new ConversationVersionVO(v.getId(), v.getVersion(), v.getCreatedAt(), v.getSnapshot()))
                .toList();
    }

    public Conversation owned(Long conversationId, Long userId) {
        Conversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "会话不存在");
        }
        if (!conversation.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权访问该会话");
        }
        return conversation;
    }
}
