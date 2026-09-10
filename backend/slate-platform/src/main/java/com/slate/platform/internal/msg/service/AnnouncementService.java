// 域/模块: 平台底座/消息中心
// 类型: 服务
// 职责: 公告——发布/下架/列表；范围节点校验（MSG-002）；按用户组织的严格可见性过滤随组织归属接入（/auth/me orgId 回填后）
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.platform.internal.msg.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.slate.common.error.BusinessException;
import com.slate.common.result.PageQuery;
import com.slate.common.result.PageResult;
import com.slate.framework.id.SnowflakeIdGenerator;
import com.slate.platform.api.msg.AnnouncementDto;
import com.slate.platform.internal.msg.entity.Announcement;
import com.slate.platform.internal.msg.error.MsgErrorCode;
import com.slate.platform.internal.msg.mapper.AnnouncementMapper;
import com.slate.platform.internal.org.entity.OrgUnit;
import com.slate.platform.internal.org.mapper.OrgUnitMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AnnouncementService {

    private final AnnouncementMapper announcementMapper;
    private final OrgUnitMapper orgUnitMapper;
    private final SnowflakeIdGenerator idGenerator;

    public AnnouncementService(AnnouncementMapper announcementMapper,
                               OrgUnitMapper orgUnitMapper,
                               SnowflakeIdGenerator idGenerator) {
        this.announcementMapper = announcementMapper;
        this.orgUnitMapper = orgUnitMapper;
        this.idGenerator = idGenerator;
    }

    /** 已发布公告列表（新→旧）：SQL 分页（历史公告随时间无界增长） */
    public PageResult<AnnouncementDto> list(PageQuery query) {
        Page<Announcement> page = announcementMapper.selectPage(
                new Page<>(query.getPage(), query.limitedSize()),
                new LambdaQueryWrapper<Announcement>()
                        .eq(Announcement::getStatus, Announcement.PUBLISHED)
                        .orderByDesc(Announcement::getId));
        List<AnnouncementDto> dtos = page.getRecords().stream()
                .map(this::toDto)
                .toList();
        return new PageResult<>(dtos, page.getTotal(), query.getPage(), query.limitedSize());
    }

    public AnnouncementDto create(AnnouncementDto.CreateRequest request, Long publisherUserId) {
        Long scopeOrgId = request.scopeOrgId();
        if (!"SCHOOL".equals(request.scopeType())) {
            if (scopeOrgId == null) {
                throw new BusinessException(MsgErrorCode.MSG_002);
            }
            OrgUnit unit = orgUnitMapper.selectById(scopeOrgId);
            if (unit == null) {
                throw new BusinessException(MsgErrorCode.MSG_002);
            }
        } else {
            scopeOrgId = null;
        }
        Announcement announcement = new Announcement();
        announcement.setId(idGenerator.nextId());
        announcement.setTitle(request.title());
        announcement.setContent(request.content());
        announcement.setScopeType(request.scopeType());
        announcement.setScopeOrgId(scopeOrgId);
        announcement.setStatus(Announcement.PUBLISHED);
        announcement.setPublishedBy(publisherUserId);
        announcementMapper.insert(announcement);
        return toDto(announcement);
    }

    /** 下架（软下线，记录保留） */
    public void offline(Long id) {
        Announcement announcement = require(id);
        announcement.setStatus(Announcement.OFFLINE);
        announcementMapper.updateById(announcement);
    }

    public void delete(Long id) {
        require(id);
        announcementMapper.deleteById(id);
    }

    private Announcement require(Long id) {
        Announcement announcement = announcementMapper.selectById(id);
        if (announcement == null) {
            throw new BusinessException(MsgErrorCode.MSG_001);
        }
        return announcement;
    }

    private AnnouncementDto toDto(Announcement a) {
        return new AnnouncementDto(a.getId(), a.getTitle(), a.getContent(),
                a.getScopeType(), a.getScopeOrgId(), a.getStatus(), a.getCreatedAt());
    }
}
