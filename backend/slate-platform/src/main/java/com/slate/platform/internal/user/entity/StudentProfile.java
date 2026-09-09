// 域/模块: 平台底座/用户中心
// 类型: 实体
// 职责: 学生子档（学籍号 AES 加密列）
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.platform.internal.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("student_profile")
public class StudentProfile {

    @TableId(type = IdType.INPUT)
    private Long userId;
    private String studentNo;   // AES-GCM 加密文本
    private String gradeEntry;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getStudentNo() { return studentNo; }
    public void setStudentNo(String studentNo) { this.studentNo = studentNo; }
    public String getGradeEntry() { return gradeEntry; }
    public void setGradeEntry(String gradeEntry) { this.gradeEntry = gradeEntry; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
