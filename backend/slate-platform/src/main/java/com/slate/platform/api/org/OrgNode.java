// 域/模块: 平台底座/组织架构
// 类型: 契约 DTO
// 职责: 组织树节点与节点增改请求（契约见 detail/api/组织架构.md）
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.platform.api.org;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/** 组织树节点（五级：INSTITUTION/CAMPUS/SECTION/GRADE/CLASS） */
public record OrgNode(Long id, String type, String name, Long parentId, Integer sort, List<OrgNode> children) {

    /** 创建节点（INSTITUTION 挂根 parentId=0，其余类型父节点须为上一级类型） */
    public record CreateRequest(
            @NotBlank @Pattern(regexp = "INSTITUTION|CAMPUS|SECTION|GRADE|CLASS") String type,
            @NotBlank @Size(max = 64) String name,
            Long parentId,
            Integer sort) {
    }

    /** 更新节点（名称与排序；类型与父子关系不可改——树结构稳定） */
    public record UpdateRequest(
            @NotBlank @Size(max = 64) String name,
            Integer sort) {
    }
}
