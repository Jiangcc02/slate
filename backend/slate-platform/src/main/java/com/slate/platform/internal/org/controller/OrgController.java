// 域/模块: 平台底座/组织架构
// 类型: 控制器
// 职责: orgs 前缀接口——组织树/班级名单/任课关系（契约见 docs/design/平台底座/detail/api/组织架构.md）
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.platform.internal.org.controller;

import com.slate.common.result.PageQuery;
import com.slate.common.result.PageResult;
import com.slate.framework.audit.Audited;
import com.slate.platform.api.org.MemberDto;
import com.slate.platform.api.org.OrgNode;
import com.slate.platform.api.org.TeachingAssignmentDto;
import com.slate.platform.internal.auth.security.RequirePermission;
import com.slate.platform.internal.org.service.MemberService;
import com.slate.platform.internal.org.service.OrgService;
import com.slate.platform.internal.org.service.TeachingService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orgs")
public class OrgController {

    private final OrgService orgService;
    private final MemberService memberService;
    private final TeachingService teachingService;

    public OrgController(OrgService orgService, MemberService memberService, TeachingService teachingService) {
        this.orgService = orgService;
        this.memberService = memberService;
        this.teachingService = teachingService;
    }

    // ── 组织树 ──

    @GetMapping("/tree")
    public List<OrgNode> tree(@RequestParam(required = false) String type) {
        return orgService.tree(type);
    }

    @PostMapping
    @RequirePermission("sys:org:write")
    @Audited(action = "org.unit.create", target = "org_unit")
    public Long create(@Valid @RequestBody OrgNode.CreateRequest request) {
        return orgService.create(request);
    }

    @PatchMapping("/{id}")
    @RequirePermission("sys:org:write")
    @Audited(action = "org.unit.update", target = "org_unit")
    public void update(@PathVariable Long id, @Valid @RequestBody OrgNode.UpdateRequest request) {
        orgService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @RequirePermission("sys:org:write")
    @Audited(action = "org.unit.delete", target = "org_unit")
    public void delete(@PathVariable Long id) {
        orgService.delete(id);
    }

    // ── 班级名单 ──

    @GetMapping("/{id}/members")
    @RequirePermission("org:member:read")
    public PageResult<MemberDto> members(@PathVariable Long id,
                                         @RequestParam(required = false) String academicYear,
                                         @RequestParam(required = false) String status,
                                         PageQuery query) {
        return memberService.list(id, academicYear, status, query);
    }

    @PostMapping("/{id}/members")
    @RequirePermission("org:member:write")
    @Audited(action = "org.member.enroll", target = "class_membership")
    public void enroll(@PathVariable Long id, @Valid @RequestBody MemberDto.AddRequest request) {
        memberService.enroll(id, request.studentIds(), request.academicYear());
    }

    @DeleteMapping("/{id}/members/{studentId}")
    @RequirePermission("org:member:write")
    @Audited(action = "org.member.leave", target = "class_membership")
    public void leave(@PathVariable Long id,
                      @PathVariable Long studentId,
                      @Valid @RequestBody MemberDto.RemoveRequest request) {
        memberService.leave(id, studentId, request.reason());
    }

    // ── 任课关系 ──

    @GetMapping("/{id}/teaching-assignments")
    @RequirePermission("org:teaching:read")
    public List<TeachingAssignmentDto> classTeachings(@PathVariable Long id,
                                                      @RequestParam(required = false) String academicYear) {
        return teachingService.listByClass(id, academicYear);
    }

    @PostMapping("/{id}/teaching-assignments")
    @RequirePermission("org:teaching:write")
    @Audited(action = "org.teaching.create", target = "teaching_assignment")
    public Long createTeaching(@PathVariable Long id,
                               @Valid @RequestBody TeachingAssignmentDto.CreateRequest request) {
        return teachingService.create(request);
    }

    @DeleteMapping("/{id}/teaching-assignments/{assignmentId}")
    @RequirePermission("org:teaching:write")
    @Audited(action = "org.teaching.delete", target = "teaching_assignment")
    public void deleteTeaching(@PathVariable Long id, @PathVariable Long assignmentId) {
        teachingService.delete(assignmentId);
    }

    /** 教师维度任课查询（教师端工作台入口，登录即可） */
    @GetMapping("/teaching-assignments")
    public List<TeachingAssignmentDto> teacherTeachings(@RequestParam Long teacherId,
                                                        @RequestParam(required = false) String academicYear) {
        return teachingService.listByTeacher(teacherId, academicYear);
    }
}
