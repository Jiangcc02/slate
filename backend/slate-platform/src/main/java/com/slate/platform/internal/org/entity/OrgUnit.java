// 域/模块: 平台底座/组织架构
// 类型: 实体
// 职责: 组织五级树（表 org_unit，path 物化路径）
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.platform.internal.org.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("org_unit")
public class OrgUnit {

    public static final String INSTITUTION = "INSTITUTION";
    public static final String CAMPUS = "CAMPUS";
    public static final String SECTION = "SECTION";
    public static final String GRADE = "GRADE";
    public static final String CLASS = "CLASS";

    /** 五级链：上级类型约束（INSTITUTION 挂根） */
    public static final java.util.Map<String, String> PARENT_TYPE = java.util.Map.of(
            CAMPUS, INSTITUTION,
            SECTION, CAMPUS,
            GRADE, SECTION,
            CLASS, GRADE);

    @TableId(type = IdType.INPUT)
    private Long id;
    private String type;
    private String name;
    private Long parentId;
    private String path;
    private Integer sort;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
