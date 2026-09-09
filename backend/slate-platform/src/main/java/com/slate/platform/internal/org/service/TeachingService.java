// 域/模块: 平台底座/组织架构
// 类型: 服务
// 职责: 任课关系——班级维度与教师维度查询、唯一性校验（ORG-004/005）；courseId 有效性随课程域模块接入校验
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.platform.internal.org.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.slate.common.error.BusinessException;
import com.slate.framework.id.SnowflakeIdGenerator;
import com.slate.platform.api.org.TeachingAssignmentDto;
import com.slate.platform.internal.auth.entity.UserProfile;
import com.slate.platform.internal.auth.mapper.UserProfileMapper;
import com.slate.platform.internal.org.entity.TeachingAssignment;
import com.slate.platform.internal.org.error.OrgErrorCode;
import com.slate.platform.internal.org.mapper.TeachingAssignmentMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TeachingService {

    private final TeachingAssignmentMapper teachingMapper;
    private final UserProfileMapper profileMapper;
    private final OrgService orgService;
    private final SnowflakeIdGenerator idGenerator;

    public TeachingService(TeachingAssignmentMapper teachingMapper,
                           UserProfileMapper profileMapper,
                           OrgService orgService,
                           SnowflakeIdGenerator idGenerator) {
        this.teachingMapper = teachingMapper;
        this.profileMapper = profileMapper;
        this.orgService = orgService;
        this.idGenerator = idGenerator;
    }

    /** 班级任课（orgs/{id}/teaching-assignments） */
    public List<TeachingAssignmentDto> listByClass(Long classId, String academicYear) {
        LambdaQueryWrapper<TeachingAssignment> wrapper = new LambdaQueryWrapper<TeachingAssignment>()
                .eq(TeachingAssignment::getClassId, classId);
        if (academicYear != null && !academicYear.isBlank()) {
            wrapper.eq(TeachingAssignment::getAcademicYear, academicYear);
        }
        return toDto(teachingMapper.selectList(wrapper));
    }

    /** 教师任课（教师端工作台入口：GET /orgs/teaching-assignments?teacherId=&academicYear=） */
    public List<TeachingAssignmentDto> listByTeacher(Long teacherId, String academicYear) {
        LambdaQueryWrapper<TeachingAssignment> wrapper = new LambdaQueryWrapper<TeachingAssignment>()
                .eq(TeachingAssignment::getTeacherId, teacherId);
        if (academicYear != null && !academicYear.isBlank()) {
            wrapper.eq(TeachingAssignment::getAcademicYear, academicYear);
        }
        return toDto(teachingMapper.selectList(wrapper));
    }

    @Transactional
    public Long create(TeachingAssignmentDto.CreateRequest request) {
        UserProfile teacher = profileMapper.selectById(request.teacherId());
        if (teacher == null || !"TEACHER".equals(teacher.getUserType())) {
            throw new BusinessException(OrgErrorCode.ORG_005, "教师档案无效");
        }
        orgService.requireUnit(request.classId());
        Long duplicate = teachingMapper.selectCount(new LambdaQueryWrapper<TeachingAssignment>()
                .eq(TeachingAssignment::getTeacherId, request.teacherId())
                .eq(TeachingAssignment::getClassId, request.classId())
                .eq(TeachingAssignment::getCourseId, request.courseId())
                .eq(TeachingAssignment::getAcademicYear, request.academicYear())
                .eq(TeachingAssignment::getSemester, request.semester()));
        if (duplicate > 0) {
            throw new BusinessException(OrgErrorCode.ORG_004);
        }
        // courseId 引用课程域 ID（不复制字段）；有效性校验随课程域模块接入（global-data-model §4）
        TeachingAssignment assignment = new TeachingAssignment();
        assignment.setId(idGenerator.nextId());
        assignment.setTeacherId(request.teacherId());
        assignment.setClassId(request.classId());
        assignment.setCourseId(request.courseId());
        assignment.setAcademicYear(request.academicYear());
        assignment.setSemester(request.semester());
        assignment.setSubject(request.subject());
        teachingMapper.insert(assignment);
        return assignment.getId();
    }

    @Transactional
    public void delete(Long id) {
        teachingMapper.deleteById(id);
    }

    private List<TeachingAssignmentDto> toDto(List<TeachingAssignment> assignments) {
        if (assignments.isEmpty()) {
            return List.of();
        }
        Map<Long, String> teacherNames = profileMapper.selectBatchIds(assignments.stream()
                        .map(TeachingAssignment::getTeacherId).distinct().toList()).stream()
                .collect(Collectors.toMap(UserProfile::getId, UserProfile::getRealName, (a, b) -> a));
        Map<Long, String> classNames = orgService.getClassNames(assignments.stream()
                .map(TeachingAssignment::getClassId).distinct().toList());
        return assignments.stream().map(a -> new TeachingAssignmentDto(
                a.getId(), a.getTeacherId(), teacherNames.get(a.getTeacherId()),
                a.getClassId(), classNames.get(a.getClassId()),
                a.getCourseId(), a.getAcademicYear(), a.getSemester(), a.getSubject()))
                .toList();
    }
}
