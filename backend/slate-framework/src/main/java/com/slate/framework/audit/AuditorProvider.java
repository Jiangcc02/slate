// 域/模块: 平台底座/工程规范
// 类型: 审计上下文接口
// 职责: 向 framework 切面提供当前操作者（由 platform 依登录态实现），避免 framework 反向依赖业务模块
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.framework.audit;

/** 当前操作者提供者：无登录态（系统任务/匿名）返回 null */
public interface AuditorProvider {
    Long currentAccountId();
}
