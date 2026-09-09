// 域/模块: 平台底座/用户中心
// 类型: 契约 DTO
// 职责: 批量导入结果报告（一期同步版：学校规模百级可同步，异步任务化随规模需求升级——契约已同步标注）
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.platform.api.user;

import java.util.List;

/** 导入报告：total/succeeded/failed + 行级明细 */
public record ImportResult(int total, int succeeded, int failed, List<RowResult> rows) {

    public record RowResult(int row, String studentName, boolean success, String message) {
    }
}
