// 域/模块: 平台底座/消息中心
// 类型: 实体
// 职责: 公告（表 announcement；范围=组织节点引用，SCHOOL 全校）
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.platform.internal.msg.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("announcement")
public class Announcement {

    public static final String PUBLISHED = "PUBLISHED";
    public static final String OFFLINE = "OFFLINE";

    @TableId(type = IdType.INPUT)
    private Long id;
    private String title;
    private String content;
    private String scopeType;
    private Long scopeOrgId;
    private String status;
    private Long publishedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getScopeType() { return scopeType; }
    public void setScopeType(String scopeType) { this.scopeType = scopeType; }
    public Long getScopeOrgId() { return scopeOrgId; }
    public void setScopeOrgId(Long scopeOrgId) { this.scopeOrgId = scopeOrgId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getPublishedBy() { return publishedBy; }
    public void setPublishedBy(Long publishedBy) { this.publishedBy = publishedBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
