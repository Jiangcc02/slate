// 域/模块: 平台底座/组织架构
// 类型: 服务
// 职责: 组织树管理——五级链校验、物化路径维护、删除保护（ORG-001/002/006）
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.platform.internal.org.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.slate.common.error.BusinessException;
import com.slate.common.result.PageQuery;
import com.slate.common.result.PageResult;
import com.slate.framework.id.SnowflakeIdGenerator;
import com.slate.platform.api.org.OrgNode;
import com.slate.platform.internal.org.entity.ClassMembership;
import com.slate.platform.internal.org.entity.OrgUnit;
import com.slate.platform.internal.org.error.OrgErrorCode;
import com.slate.platform.internal.org.mapper.ClassMembershipMapper;
import com.slate.platform.internal.org.mapper.OrgUnitMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class OrgService {

    private final OrgUnitMapper orgUnitMapper;
    private final ClassMembershipMapper classMembershipMapper;
    private final SnowflakeIdGenerator idGenerator;

    public OrgService(OrgUnitMapper orgUnitMapper,
                      ClassMembershipMapper classMembershipMapper,
                      SnowflakeIdGenerator idGenerator) {
        this.orgUnitMapper = orgUnitMapper;
        this.classMembershipMapper = classMembershipMapper;
        this.idGenerator = idGenerator;
    }

    /** 全量组织树；type 过滤时保留命中节点及其祖先路径 */
    public List<OrgNode> tree(String type) {
        List<OrgUnit> all = orgUnitMapper.selectList(null);
        all.sort(Comparator
                .comparing((OrgUnit u) -> u.getPath().length())
                .thenComparing(u -> u.getSort() == null ? 0 : u.getSort()));
        Set<Long> visible = new HashSet<>();
        if (type == null || type.isBlank()) {
            all.forEach(u -> visible.add(u.getId()));
        } else {
            Map<Long, OrgUnit> byId = new LinkedHashMap<>();
            all.forEach(u -> byId.put(u.getId(), u));
            for (OrgUnit unit : all) {
                if (type.equals(unit.getType())) {
                    OrgUnit cursor = unit;
                    while (cursor != null && visible.add(cursor.getId())) {
                        cursor = cursor.getParentId() == null || cursor.getParentId() == 0
                                ? null : byId.get(cursor.getParentId());
                    }
                }
            }
        }
        Map<Long, OrgNode> nodes = new LinkedHashMap<>();
        for (OrgUnit unit : all) {
            if (visible.contains(unit.getId())) {
                nodes.put(unit.getId(), new OrgNode(unit.getId(), unit.getType(), unit.getName(),
                        unit.getParentId(), unit.getSort(), new ArrayList<>()));
            }
        }
        List<OrgNode> roots = new ArrayList<>();
        for (OrgUnit unit : all) {
            OrgNode node = nodes.get(unit.getId());
            if (node == null) {
                continue;
            }
            OrgNode parent = nodes.get(unit.getParentId());
            if (parent == null) {
                roots.add(node);
            } else {
                parent.children().add(node);
            }
        }
        return roots;
    }

    @Transactional
    public Long create(OrgNode.CreateRequest request) {
        OrgUnit parent = null;
        if (OrgUnit.INSTITUTION.equals(request.type())) {
            if (request.parentId() != null && request.parentId() != 0) {
                throw new BusinessException(OrgErrorCode.ORG_006, "机构只能作为根节点");
            }
        } else {
            long parentId = request.parentId() == null ? 0 : request.parentId();
            parent = orgUnitMapper.selectById(parentId);
            String expectedParentType = OrgUnit.PARENT_TYPE.get(request.type());
            if (parent == null || expectedParentType == null || !expectedParentType.equals(parent.getType())) {
                throw new BusinessException(OrgErrorCode.ORG_006,
                        request.type() + " 的上级须为 " + expectedParentType);
            }
        }
        OrgUnit unit = new OrgUnit();
        unit.setId(idGenerator.nextId());
        unit.setType(request.type());
        unit.setName(request.name());
        unit.setParentId(parent == null ? 0 : parent.getId());
        unit.setSort(request.sort() == null ? 0 : request.sort());
        unit.setPath((parent == null ? "" : parent.getPath()) + unit.getId() + "/");
        orgUnitMapper.insert(unit);
        return unit.getId();
    }

    @Transactional
    public void update(Long id, OrgNode.UpdateRequest request) {
        OrgUnit unit = requireUnit(id);
        unit.setName(request.name());
        unit.setSort(request.sort() == null ? unit.getSort() : request.sort());
        orgUnitMapper.updateById(unit);
    }

    @Transactional
    public void delete(Long id) {
        OrgUnit unit = requireUnit(id);
        if (orgUnitMapper.selectCount(new LambdaQueryWrapper<OrgUnit>().eq(OrgUnit::getParentId, id)) > 0) {
            throw new BusinessException(OrgErrorCode.ORG_001);
        }
        if (OrgUnit.CLASS.equals(unit.getType())) {
            assertClassDeletable(id);
        }
        orgUnitMapper.deleteById(id);
    }

    /** 班级删除保护：在籍名单存在则 ORG-002（供 delete 前置与名单服务复用） */
    public void assertClassDeletable(Long classId) {
        if (classMembershipMapper.selectCount(new LambdaQueryWrapper<ClassMembership>()
                .eq(ClassMembership::getClassId, classId)
                .eq(ClassMembership::getStatus, ClassMembership.ENROLLED)) > 0) {
            throw new BusinessException(OrgErrorCode.ORG_002);
        }
    }

    /** 班级的年级（parent GRADE 节点）——同学年同学级唯一在籍校验用 */
    public Long gradeIdOf(Long classId) {
        OrgUnit classUnit = orgUnitMapper.selectById(classId);
        if (classUnit == null || !OrgUnit.CLASS.equals(classUnit.getType())) {
            throw new BusinessException(OrgErrorCode.ORG_006, "节点不是班级");
        }
        return classUnit.getParentId();
    }

    public OrgUnit requireUnit(Long id) {
        OrgUnit unit = orgUnitMapper.selectById(id);
        if (unit == null) {
            throw new BusinessException(OrgErrorCode.ORG_006, "组织节点不存在");
        }
        return unit;
    }

    /** 班级 ID → 名称（任课视图展示用，按 ID 批量引用不复制——global-data-model §4） */
    public Map<Long, String> getClassNames(List<Long> classIds) {
        if (classIds.isEmpty()) {
            return Map.of();
        }
        return orgUnitMapper.selectBatchIds(classIds).stream()
                .collect(Collectors.toMap(OrgUnit::getId, OrgUnit::getName, (a, b) -> a));
    }

    /** 名单分页（简单内存分页骨架——数据量班级级 ≤ 百人，随教务域规模演进再下沉 SQL 分页） */
    public <T> PageResult<T> page(List<T> list, PageQuery query) {
        int from = Math.min(query.offset(), list.size());
        int to = Math.min(from + query.limitedSize(), list.size());
        return new PageResult<>(list.subList(from, to), list.size(), query.getPage(), query.limitedSize());
    }
}
