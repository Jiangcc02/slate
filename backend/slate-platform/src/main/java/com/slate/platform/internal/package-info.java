// 域/模块: 平台底座/内部实现包
// 类型: 包声明
// 职责: 平台底座内部实现（Controller/Service/Repository/配置）——禁止他域 import（铁律 L7，ArchUnit 强制）
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc

/**
 * 平台底座内部实现包：分层为 internal/auth、internal/org、internal/user…（Controller → Service → Repository 单向，铁律 L7）。
 */
package com.slate.platform.internal;
