// 域/模块: 平台底座/对外契约包
// 类型: 包声明
// 职责: 平台底座对外暴露的契约接口与 DTO——他域只许 import 本包（铁律 L7），实现一律在 internal
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc

/**
 * 平台底座对外契约包：认证授权 / 组织架构 / 用户中心 / 消息 / 文件 / 系统管理的
 * 跨域接口与 DTO 随实现任务在此逐包建立（api/auth、api/org…）。
 */
package com.slate.platform.api;
