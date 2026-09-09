// 域/模块: 平台底座/文件服务
// 类型: 配置属性
// 职责: slate.files.whitelist 绑定——按业务类型的扩展名白名单与大小上限
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.platform.internal.file.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;

/** whitelist.<bizType>.extensions / whitelist.<bizType>.max-size-bytes */
@ConfigurationProperties(prefix = "slate.files")
public record FileProperties(Map<String, WhitelistRule> whitelist) {

    public record WhitelistRule(List<String> extensions, long maxSizeBytes) {
    }
}
