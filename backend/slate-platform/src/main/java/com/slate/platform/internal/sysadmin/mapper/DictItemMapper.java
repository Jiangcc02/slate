// 域/模块: 平台底座/系统管理
// 类型: Mapper
// 职责: DictItem 表访问
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.platform.internal.sysadmin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.slate.platform.internal.sysadmin.entity.DictItem;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DictItemMapper extends BaseMapper<DictItem> {
}
