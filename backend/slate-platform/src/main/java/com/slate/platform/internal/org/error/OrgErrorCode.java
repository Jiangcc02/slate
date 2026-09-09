// 域/模块: 平台底座/组织架构
// 类型: 错误码表（ORG）
// 职责: 组织架构错误码——语义与 detail/api/组织架构.md 逐条对齐，一经发布冻结（api-conventions §4）
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.platform.internal.org.error;

import com.slate.common.error.ErrorCode;

/** 组织架构错误码（域码 ORG，属主=平台底座） */
public enum OrgErrorCode implements ErrorCode {

    /** 组织节点存在子节点，不可删除 */
    ORG_001("ORG-001", "存在下级节点，不可删除"),

    /** 班级仍有在籍学生，不可删除 */
    ORG_002("ORG-002", "班级仍有在籍学生，不可删除"),

    /** 同一学生同学年同学级已有在籍记录 */
    ORG_003("ORG-003", "该学生本学年本年级已有在籍记录"),

    /** 任课关系重复（同教师×班级×课程×学期） */
    ORG_004("ORG-004", "任课关系已存在"),

    /** 引用的教师/课程/学年学期不存在或无效 */
    ORG_005("ORG-005", "引用的教师或学年学期无效"),

    /** 父节点不存在或类型层级不合法（五级链：机构→校区→学部→年级→班级） */
    ORG_006("ORG-006", "上级节点无效或层级不符");

    private final String code;
    private final String message;

    OrgErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }
}
