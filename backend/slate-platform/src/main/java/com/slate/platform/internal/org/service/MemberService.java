// 域/模块: 平台底座/组织架构
// 类型: 服务
// 职责: 班级名单——入班（ORG-003 同学年同学级唯一在籍）/出班留痕；变更外发领域事件 org.class.member-changed
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.platform.internal.org.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.slate.common.error.BusinessException;
import com.slate.common.result.PageQuery;
import com.slate.common.result.PageResult;
import com.slate.framework.id.SnowflakeIdGenerator;
import com.slate.platform.api.events.DomainEventPublisher;
import com.slate.platform.api.org.MemberDto;
import com.slate.platform.internal.auth.entity.UserProfile;
import com.slate.platform.internal.auth.mapper.UserProfileMapper;
import com.slate.platform.internal.org.entity.ClassMembership;
import com.slate.platform.internal.org.entity.OrgUnit;
import com.slate.platform.internal.org.error.OrgErrorCode;
import com.slate.platform.internal.org.mapper.ClassMembershipMapper;
import com.slate.platform.internal.org.mapper.OrgUnitMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class MemberService {

    public static final String EVENT_MEMBER_CHANGED = "org.class.member-changed";

    private final ClassMembershipMapper membershipMapper;
    private final OrgUnitMapper orgUnitMapper;
    private final UserProfileMapper profileMapper;
    private final OrgService orgService;
    private final DomainEventPublisher eventPublisher;
    private final SnowflakeIdGenerator idGenerator;

    public MemberService(ClassMembershipMapper membershipMapper,
                         OrgUnitMapper orgUnitMapper,
                         UserProfileMapper profileMapper,
                         OrgService orgService,
                         DomainEventPublisher eventPublisher,
                         SnowflakeIdGenerator idGenerator) {
        this.membershipMapper = membershipMapper;
        this.orgUnitMapper = orgUnitMapper;
        this.profileMapper = profileMapper;
        this.orgService = orgService;
        this.eventPublisher = eventPublisher;
        this.idGenerator = idGenerator;
    }

    /** 班级名单（默认在籍；status 过滤可查历史异动） */
    public PageResult<MemberDto> list(Long classId, String academicYear, String status, PageQuery query) {
        LambdaQueryWrapper<ClassMembership> wrapper = new LambdaQueryWrapper<ClassMembership>()
                .eq(ClassMembership::getClassId, classId)
                .orderByDesc(ClassMembership::getJoinedAt);
        if (academicYear != null && !academicYear.isBlank()) {
            wrapper.eq(ClassMembership::getAcademicYear, academicYear);
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(ClassMembership::getStatus, status);
        } else {
            wrapper.eq(ClassMembership::getStatus, ClassMembership.ENROLLED);
        }
        List<ClassMembership> memberships = membershipMapper.selectList(wrapper);
        Map<Long, UserProfile> profiles = memberships.isEmpty() ? Map.of()
                : profileMapper.selectBatchIds(memberships.stream()
                        .map(ClassMembership::getStudentId).distinct().toList())
                .stream().collect(Collectors.toMap(UserProfile::getId, Function.identity()));
        List<MemberDto> dtos = memberships.stream().map(m -> {
            UserProfile profile = profiles.get(m.getStudentId());
            return new MemberDto(m.getId(), m.getStudentId(),
                    profile == null ? null : profile.getRealName(),
                    m.getAcademicYear(), m.getStatus(), m.getJoinedAt(), m.getLeftAt(), m.getLeaveReason());
        }).toList();
        return orgService.page(dtos, query);
    }

    /** 入班：学生须为 STUDENT 档案且同学年同学级无在籍（ORG-003） */
    @Transactional
    public void enroll(Long classId, List<Long> studentIds, String academicYear) {
        orgService.requireUnit(classId);
        Long gradeId = orgService.gradeIdOf(classId);
        List<UserProfile> students = profileMapper.selectBatchIds(studentIds);
        if (students.size() != studentIds.stream().distinct().count()
                || students.stream().anyMatch(p -> !"STUDENT".equals(p.getUserType()))) {
            throw new BusinessException(OrgErrorCode.ORG_005, "存在无效的学生档案");
        }
        for (Long studentId : studentIds.stream().distinct().toList()) {
            assertNotEnrolledInGrade(studentId, gradeId, academicYear);
            ClassMembership membership = new ClassMembership();
            membership.setId(idGenerator.nextId());
            membership.setClassId(classId);
            membership.setStudentId(studentId);
            membership.setAcademicYear(academicYear);
            membership.setStatus(ClassMembership.ENROLLED);
            membership.setJoinedAt(LocalDateTime.now());
            membershipMapper.insert(membership);
            publishChange(classId, studentId, academicYear, "ENROLLED");
        }
    }

    /** 出班：在籍→转出留痕（原因必填） */
    @Transactional
    public void leave(Long classId, Long studentId, String reason) {
        ClassMembership membership = membershipMapper.selectList(new LambdaQueryWrapper<ClassMembership>()
                        .eq(ClassMembership::getClassId, classId)
                        .eq(ClassMembership::getStudentId, studentId)
                        .eq(ClassMembership::getStatus, ClassMembership.ENROLLED)
                        .last("LIMIT 1"))
                .stream().findFirst()
                .orElseThrow(() -> new BusinessException(OrgErrorCode.ORG_005, "该学生不在此班在籍名单中"));
        membership.setStatus(ClassMembership.LEFT);
        membership.setLeftAt(LocalDateTime.now());
        membership.setLeaveReason(reason);
        membershipMapper.updateById(membership);
        publishChange(classId, studentId, membership.getAcademicYear(), "LEFT");
    }

    /** ORG-003：同学年同学级唯一在籍（班级→年级，覆盖同年级跨班重复） */
    private void assertNotEnrolledInGrade(Long studentId, Long gradeId, String academicYear) {
        List<ClassMembership> enrolled = membershipMapper.selectList(new LambdaQueryWrapper<ClassMembership>()
                .eq(ClassMembership::getStudentId, studentId)
                .eq(ClassMembership::getAcademicYear, academicYear)
                .eq(ClassMembership::getStatus, ClassMembership.ENROLLED));
        if (enrolled.isEmpty()) {
            return;
        }
        List<Long> classIds = enrolled.stream().map(ClassMembership::getClassId).toList();
        boolean sameGrade = orgUnitMapper.selectBatchIds(classIds).stream()
                .anyMatch(u -> gradeId.equals(u.getParentId()));
        if (sameGrade) {
            throw new BusinessException(OrgErrorCode.ORG_003);
        }
    }

    private void publishChange(Long classId, Long studentId, String academicYear, String action) {
        eventPublisher.publish(EVENT_MEMBER_CHANGED, "org.class", classId, Map.of(
                "classId", classId,
                "studentId", studentId,
                "academicYear", academicYear,
                "action", action,
                "occurredAt", LocalDateTime.now().toString()));
    }
}
