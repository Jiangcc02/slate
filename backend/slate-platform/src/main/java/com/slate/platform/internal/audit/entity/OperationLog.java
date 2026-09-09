// 域/模块: 平台底座/系统管理
// 类型: 实体
// 职责: 操作审计日志（表 operation_log，只追加不修改——data-ownership）
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.platform.internal.audit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("operation_log")
public class OperationLog {

    @TableId(type = IdType.INPUT)
    private Long id;
    private Long accountId;
    private Long onBehalfOf;
    private String serviceId;
    private String action;
    private String target;
    private String paramsDigest;
    private String result;
    private String traceId;
    private String ip;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }
    public Long getOnBehalfOf() { return onBehalfOf; }
    public void setOnBehalfOf(Long onBehalfOf) { this.onBehalfOf = onBehalfOf; }
    public String getServiceId() { return serviceId; }
    public void setServiceId(String serviceId) { this.serviceId = serviceId; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }
    public String getParamsDigest() { return paramsDigest; }
    public void setParamsDigest(String paramsDigest) { this.paramsDigest = paramsDigest; }
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
