// 域/模块: 平台底座/工程规范
// 类型: Mapper
// 职责: domain_event 表访问
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.platform.internal.events.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.slate.platform.internal.events.entity.DomainEvent;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DomainEventMapper extends BaseMapper<DomainEvent> {
}
