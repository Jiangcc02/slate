// 域/模块: 平台底座/系统管理
// 类型: 实体
// 职责: 参数配置（表 sys_config；作用域 GLOBAL/CAMPUS）
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.platform.internal.sysadmin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("sys_config")
public class SysConfig {

    @TableId(type = IdType.INPUT)
    private Long id;
    private String cfgKey;
    private String cfgValue;
    private String scope;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCfgKey() { return cfgKey; }
    public void setCfgKey(String cfgKey) { this.cfgKey = cfgKey; }
    public String getCfgValue() { return cfgValue; }
    public void setCfgValue(String cfgValue) { this.cfgValue = cfgValue; }
    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
