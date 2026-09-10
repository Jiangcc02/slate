// 域/模块: 平台底座/消息中心
// 类型: 服务
// 职责: 站内信——发送（一条消息+批量投递）、收件箱、已读、未读数；一期轮询（WebSocket 随三期随堂测引入）
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.platform.internal.msg.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.slate.common.error.BusinessException;
import com.slate.common.result.PageQuery;
import com.slate.common.result.PageResult;
import com.slate.framework.id.SnowflakeIdGenerator;
import com.slate.platform.api.msg.MessageDto;
import com.slate.platform.internal.auth.entity.UserProfile;
import com.slate.platform.internal.auth.mapper.UserProfileMapper;
import com.slate.platform.internal.msg.entity.Message;
import com.slate.platform.internal.msg.entity.MessageDelivery;
import com.slate.platform.internal.msg.error.MsgErrorCode;
import com.slate.platform.internal.msg.mapper.MessageDeliveryMapper;
import com.slate.platform.internal.msg.mapper.MessageMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class MessageService {

    /** 多值 INSERT 单语句行数上限（防超长 SQL） */
    private static final int BATCH_SIZE = 500;

    private final MessageMapper messageMapper;
    private final MessageDeliveryMapper deliveryMapper;
    private final UserProfileMapper profileMapper;
    private final SnowflakeIdGenerator idGenerator;

    public MessageService(MessageMapper messageMapper,
                          MessageDeliveryMapper deliveryMapper,
                          UserProfileMapper profileMapper,
                          SnowflakeIdGenerator idGenerator) {
        this.messageMapper = messageMapper;
        this.deliveryMapper = deliveryMapper;
        this.profileMapper = profileMapper;
        this.idGenerator = idGenerator;
    }

    /** 发送：一条消息 + 批量投递记录；收件人须全部存在（无效即 MSG-003 整体拒绝，保持事务性）。
     *  传输层幂等由 IdempotentFilter 承担（服务间/Agent 调用按契约携带 Idempotency-Key） */
    @Transactional
    public void send(MessageDto.SendRequest request, Long senderUserId) {
        List<Long> receiverIds = request.receiverIds().stream().distinct().toList();
        validateReceivers(receiverIds);
        Message message = new Message();
        message.setId(idGenerator.nextId());
        message.setTitle(request.title());
        message.setContent(request.content());
        message.setRefType(request.refType());
        message.setRefId(request.refId());
        message.setSenderId(senderUserId);
        messageMapper.insert(message);
        List<MessageDelivery> batch = new ArrayList<>(Math.min(receiverIds.size(), BATCH_SIZE));
        for (Long receiverId : receiverIds) {
            MessageDelivery delivery = new MessageDelivery();
            delivery.setId(idGenerator.nextId());
            delivery.setMessageId(message.getId());
            delivery.setReceiverId(receiverId);
            batch.add(delivery);
            if (batch.size() == BATCH_SIZE) {
                deliveryMapper.insertBatch(batch);
                batch = new ArrayList<>(BATCH_SIZE);
            }
        }
        if (!batch.isEmpty()) {
            deliveryMapper.insertBatch(batch);
        }
    }

    /** 收件人有效性：user_profile 中须全部存在（幽灵收件人只会永远未读且无法投达） */
    private void validateReceivers(List<Long> receiverIds) {
        Set<Long> existing = new HashSet<>(profileMapper.selectBatchIds(receiverIds)
                .stream().map(UserProfile::getId).toList());
        List<Long> missing = receiverIds.stream().filter(id -> !existing.contains(id)).toList();
        if (!missing.isEmpty()) {
            throw new BusinessException(MsgErrorCode.MSG_003, "无效收件人: " + missing);
        }
    }

    /** 我的收件箱（新→旧；unreadOnly=true 只看未读）：SQL 分页，排序命中 idx_md_receiver_id */
    public PageResult<MessageDto> inbox(Long receiverId, boolean unreadOnly, PageQuery query) {
        LambdaQueryWrapper<MessageDelivery> wrapper = new LambdaQueryWrapper<MessageDelivery>()
                .eq(MessageDelivery::getReceiverId, receiverId)
                .orderByDesc(MessageDelivery::getId);
        if (unreadOnly) {
            wrapper.isNull(MessageDelivery::getReadAt);
        }
        Page<MessageDelivery> page = deliveryMapper.selectPage(
                new Page<>(query.getPage(), query.limitedSize()), wrapper);
        List<MessageDelivery> deliveries = page.getRecords();
        Map<Long, Message> messages = deliveries.isEmpty() ? Map.of()
                : messageMapper.selectBatchIds(deliveries.stream().map(MessageDelivery::getMessageId).toList())
                .stream().collect(Collectors.toMap(Message::getId, Function.identity()));
        List<MessageDto> dtos = deliveries.stream()
                .map(d -> {
                    Message message = messages.get(d.getMessageId());
                    if (message == null) {
                        throw new BusinessException(MsgErrorCode.MSG_001);
                    }
                    return new MessageDto(message.getId(), message.getTitle(), message.getContent(),
                            message.getRefType(), message.getRefId(), message.getCreatedAt(), d.getReadAt());
                })
                .toList();
        return new PageResult<>(dtos, page.getTotal(), query.getPage(), query.limitedSize());
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

    /** 全部已读：单条 UPDATE（此前逐行 update 在未读积压时是 1+N 次往返） */
    @Transactional
    public void markAllRead(Long receiverId) {
        deliveryMapper.update(null, new LambdaUpdateWrapper<MessageDelivery>()
                .eq(MessageDelivery::getReceiverId, receiverId)
                .isNull(MessageDelivery::getReadAt)
                .set(MessageDelivery::getReadAt, LocalDateTime.now()));
    }

    public long unreadCount(Long receiverId) {
        return deliveryMapper.selectCount(new LambdaQueryWrapper<MessageDelivery>()
                .eq(MessageDelivery::getReceiverId, receiverId)
                .isNull(MessageDelivery::getReadAt));
    }
}
