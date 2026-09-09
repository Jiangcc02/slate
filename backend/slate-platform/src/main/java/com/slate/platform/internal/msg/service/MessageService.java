// 域/模块: 平台底座/消息中心
// 类型: 服务
// 职责: 站内信——发送（一条消息+批量投递）、收件箱、已读、未读数；一期轮询（WebSocket 随三期随堂测引入）
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.platform.internal.msg.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.slate.common.error.BusinessException;
import com.slate.common.result.PageQuery;
import com.slate.common.result.PageResult;
import com.slate.framework.id.SnowflakeIdGenerator;
import com.slate.platform.api.msg.MessageDto;
import com.slate.platform.internal.msg.entity.Message;
import com.slate.platform.internal.msg.entity.MessageDelivery;
import com.slate.platform.internal.msg.error.MsgErrorCode;
import com.slate.platform.internal.msg.mapper.MessageDeliveryMapper;
import com.slate.platform.internal.msg.mapper.MessageMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class MessageService {

    private final MessageMapper messageMapper;
    private final MessageDeliveryMapper deliveryMapper;
    private final SnowflakeIdGenerator idGenerator;

    public MessageService(MessageMapper messageMapper,
                          MessageDeliveryMapper deliveryMapper,
                          SnowflakeIdGenerator idGenerator) {
        this.messageMapper = messageMapper;
        this.deliveryMapper = deliveryMapper;
        this.idGenerator = idGenerator;
    }

    /** 发送：一条消息 + 逐收件人投递记录（无效收件人直接 MSG-003 整体拒绝，保持事务性） */
    @Transactional
    public void send(MessageDto.SendRequest request, Long senderUserId) {
        Message message = new Message();
        message.setId(idGenerator.nextId());
        message.setTitle(request.title());
        message.setContent(request.content());
        message.setRefType(request.refType());
        message.setRefId(request.refId());
        message.setSenderId(senderUserId);
        messageMapper.insert(message);
        for (Long receiverId : request.receiverIds().stream().distinct().toList()) {
            MessageDelivery delivery = new MessageDelivery();
            delivery.setId(idGenerator.nextId());
            delivery.setMessageId(message.getId());
            delivery.setReceiverId(receiverId);
            deliveryMapper.insert(delivery);
        }
    }

    /** 我的收件箱（新→旧；unreadOnly=true 只看未读） */
    public PageResult<MessageDto> inbox(Long receiverId, boolean unreadOnly, PageQuery query) {
        LambdaQueryWrapper<MessageDelivery> wrapper = new LambdaQueryWrapper<MessageDelivery>()
                .eq(MessageDelivery::getReceiverId, receiverId)
                .orderByDesc(MessageDelivery::getId);
        if (unreadOnly) {
            wrapper.isNull(MessageDelivery::getReadAt);
        }
        List<MessageDelivery> deliveries = deliveryMapper.selectList(wrapper);
        List<MessageDelivery> page = deliveries.stream()
                .skip(query.offset()).limit(query.limitedSize()).toList();
        Map<Long, Message> messages = page.isEmpty() ? Map.of()
                : messageMapper.selectBatchIds(page.stream().map(MessageDelivery::getMessageId).toList())
                .stream().collect(Collectors.toMap(Message::getId, Function.identity()));
        List<MessageDto> dtos = page.stream()
                .map(d -> {
                    Message message = messages.get(d.getMessageId());
                    if (message == null) {
                        throw new BusinessException(MsgErrorCode.MSG_001);
                    }
                    return new MessageDto(message.getId(), message.getTitle(), message.getContent(),
                            message.getRefType(), message.getRefId(), message.getCreatedAt(), d.getReadAt());
                })
                .toList();
        return new PageResult<>(dtos, deliveries.size(), query.getPage(), query.limitedSize());
    }

    @Transactional
    public void markRead(Long receiverId, Long messageId) {
        MessageDelivery delivery = deliveryMapper.selectOne(new LambdaQueryWrapper<MessageDelivery>()
                .eq(MessageDelivery::getReceiverId, receiverId)
                .eq(MessageDelivery::getMessageId, messageId));
        if (delivery == null) {
            throw new BusinessException(MsgErrorCode.MSG_001);
        }
        if (delivery.getReadAt() == null) {
            delivery.setReadAt(LocalDateTime.now());
            deliveryMapper.updateById(delivery);
        }
    }

    @Transactional
    public void markAllRead(Long receiverId) {
        for (MessageDelivery delivery : deliveryMapper.selectList(new LambdaQueryWrapper<MessageDelivery>()
                .eq(MessageDelivery::getReceiverId, receiverId)
                .isNull(MessageDelivery::getReadAt))) {
            delivery.setReadAt(LocalDateTime.now());
            deliveryMapper.updateById(delivery);
        }
    }

    public long unreadCount(Long receiverId) {
        return deliveryMapper.selectCount(new LambdaQueryWrapper<MessageDelivery>()
                .eq(MessageDelivery::getReceiverId, receiverId)
                .isNull(MessageDelivery::getReadAt));
    }
}
