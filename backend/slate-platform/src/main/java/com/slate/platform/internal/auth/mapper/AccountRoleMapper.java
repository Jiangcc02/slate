// 域/模块: 平台底座/认证授权
// 类型: Mapper
// 职责: account_role 表访问
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.platform.internal.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.slate.platform.internal.auth.entity.AccountRole;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AccountRoleMapper extends BaseMapper<AccountRole> {
}
