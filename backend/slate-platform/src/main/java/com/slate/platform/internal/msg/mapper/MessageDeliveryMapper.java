// 域/模块: 平台底座/消息中心
// 类型: Mapper
// 职责: MessageDelivery 表访问（含群发多值批量 INSERT）
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.platform.internal.msg.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.slate.platform.internal.msg.entity.MessageDelivery;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MessageDeliveryMapper extends BaseMapper<MessageDelivery> {

    /** 群发批量投递：一条 SQL 多值插入（调用方需按 ≤500 行分片，避免超长语句） */
    @Insert("<script>INSERT INTO message_delivery (id, message_id, receiver_id, read_at, created_at) VALUES "
            + "<foreach collection='list' item='d' separator=','>"
            + "(#{d.id}, #{d.messageId}, #{d.receiverId}, NULL, NOW())"
            + "</foreach></script>")
    int insertBatch(@Param("list") List<MessageDelivery> list);
}
