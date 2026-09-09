// 域/模块: 平台底座/消息中心
// 类型: Mapper
// 职责: MessageDelivery 表访问
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.platform.internal.msg.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.slate.platform.internal.msg.entity.MessageDelivery;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MessageDeliveryMapper extends BaseMapper<MessageDelivery> {
}
