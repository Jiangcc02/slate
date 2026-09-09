// 域/模块: 平台底座/组织架构
// 类型: Mapper
// 职责: org_unit 表访问
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.platform.internal.org.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.slate.platform.internal.org.entity.OrgUnit;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrgUnitMapper extends BaseMapper<OrgUnit> {
}
