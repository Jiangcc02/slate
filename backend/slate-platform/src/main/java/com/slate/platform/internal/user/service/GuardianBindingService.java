// 域/模块: 平台底座/用户中心
// 类型: 服务
// 职责: 家长-学生绑定——发起（手机号定位/新建家长档）、确认（监护人同意，confirmed 方可触达）、解绑（事件通知家校域）
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.platform.internal.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.slate.common.error.BusinessException;
import com.slate.framework.crypto.AesCipher;
import com.slate.framework.id.SnowflakeIdGenerator;
import com.slate.platform.api.events.DomainEventPublisher;
import com.slate.platform.api.user.GuardianBindingDto;
import com.slate.platform.internal.auth.entity.UserProfile;
import com.slate.platform.internal.auth.mapper.UserProfileMapper;
import com.slate.platform.internal.user.entity.GuardianProfile;
import com.slate.platform.internal.user.entity.GuardianStudent;
import com.slate.platform.internal.user.error.UserErrorCode;
import com.slate.platform.internal.user.mapper.GuardianProfileMapper;
import com.slate.platform.internal.user.mapper.GuardianStudentMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GuardianBindingService {

    public static final String EVENT_BINDING_CHANGED = "user.guardian-binding-changed";

    private final GuardianStudentMapper bindingMapper;
    private final GuardianProfileMapper guardianProfileMapper;
    private final UserProfileMapper profileMapper;
    private final UserService userService;
    private final DomainEventPublisher eventPublisher;
    private final SnowflakeIdGenerator idGenerator;
    private final AesCipher aes;

    public GuardianBindingService(GuardianStudentMapper bindingMapper,
                                  GuardianProfileMapper guardianProfileMapper,
                                  UserProfileMapper profileMapper,
                                  UserService userService,
                                  DomainEventPublisher eventPublisher,
                                  SnowflakeIdGenerator idGenerator,
                                  AesCipher aes) {
        this.bindingMapper = bindingMapper;
        this.guardianProfileMapper = guardianProfileMapper;
        this.profileMapper = profileMapper;
        this.userService = userService;
        this.eventPublisher = eventPublisher;
        this.idGenerator = idGenerator;
        this.aes = aes;
    }

    /** 学生的绑定列表（家长姓名与手机号脱敏） */
    public List<GuardianBindingDto> listByStudent(Long studentId) {
        return bindingMapper.selectList(new LambdaQueryWrapper<GuardianStudent>()
                        .eq(GuardianStudent::getStudentId, studentId)
                        .ne(GuardianStudent::getStatus, GuardianStudent.UNBOUND))
                .stream().map(this::toDto).toList();
    }

    /** 发起绑定：按手机号哈希定位家长档（不存在则建档+开户），生成 pending；重复绑定幂等返回既有记录 */
    @Transactional
    public GuardianBindingDto bind(GuardianBindingDto.BindRequest request) {
        UserProfile student = profileMapper.selectById(request.studentId());
        if (student == null || !"STUDENT".equals(student.getUserType())) {
            throw new BusinessException(UserErrorCode.USER_003);
        }
        // 等值定位走确定性哈希列（随机 IV 加密列不可比较）；并发建档撞唯一键时回读既有档
        String phoneHash = aes.hmac(request.phone());
        GuardianProfile guardian = guardianProfileMapper.selectOne(
                new LambdaQueryWrapper<GuardianProfile>().eq(GuardianProfile::getPhoneHash, phoneHash));
        Long guardianId;
        if (guardian == null) {
            guardianId = idGenerator.nextId();
            GuardianProfile created = new GuardianProfile();
            created.setUserId(guardianId);
            created.setPhoneEnc(aes.encrypt(request.phone()));
            created.setPhoneHash(phoneHash);
            try {
                guardianProfileMapper.insert(created);
            } catch (DuplicateKeyException e) {
                guardian = guardianProfileMapper.selectOne(
                        new LambdaQueryWrapper<GuardianProfile>().eq(GuardianProfile::getPhoneHash, phoneHash));
                if (guardian == null) {
                    throw e;
                }
            }
            if (guardian != null && !guardian.getUserId().equals(guardianId)) {
                guardianId = guardian.getUserId();
            } else {
                UserProfile guardianUser = new UserProfile();
                guardianUser.setId(guardianId);
                guardianUser.setRealName(request.relation() + "（" + AesCipher.maskTail(request.phone(), 3, 4) + "）");
                guardianUser.setUserType("GUARDIAN");
                profileMapper.insert(guardianUser);
                // 家长开户随其本人首次登录/通知触达流程（一期家长端未上线，账号延后激活）
            }
        } else {
            guardianId = guardian.getUserId();
        }
        GuardianStudent exists = bindingMapper.selectOne(new LambdaQueryWrapper<GuardianStudent>()
                .eq(GuardianStudent::getGuardianId, guardianId)
                .eq(GuardianStudent::getStudentId, request.studentId()));
        if (exists != null && !GuardianStudent.UNBOUND.equals(exists.getStatus())) {
            return toDto(exists);   // 幂等
        }
        GuardianStudent binding = exists == null ? new GuardianStudent() : exists;
        if (exists == null) {
            binding.setId(idGenerator.nextId());
            binding.setGuardianId(guardianId);
            binding.setStudentId(request.studentId());
        }
        binding.setRelation(request.relation());
        binding.setIsPrimary(request.isPrimary() == null ? 0 : request.isPrimary());
        binding.setStatus(GuardianStudent.PENDING);
        if (exists == null) {
            try {
                bindingMapper.insert(binding);
            } catch (DuplicateKeyException e) {
                // 并发重复绑定撞 uk_gs：按幂等语义回读既有记录返回
                GuardianStudent raced = bindingMapper.selectOne(new LambdaQueryWrapper<GuardianStudent>()
                        .eq(GuardianStudent::getGuardianId, guardianId)
                        .eq(GuardianStudent::getStudentId, request.studentId()));
                if (raced == null) {
                    throw e;
                }
                return toDto(raced);
            }
        } else {
            bindingMapper.updateById(binding);
        }
        publish(guardianId, request.studentId(), "PENDING");
        return toDto(binding);
    }

    /** 确认 / 解绑：确认须家长侧操作或持确认码（一期管理端代确认，家长端上线后收紧） */
    @Transactional
    public GuardianBindingDto act(Long bindingId, String action) {
        GuardianStudent binding = bindingMapper.selectById(bindingId);
        if (binding == null || GuardianStudent.UNBOUND.equals(binding.getStatus())) {
            throw new BusinessException(UserErrorCode.USER_004);
        }
        if ("confirm".equals(action)) {
            binding.setStatus(GuardianStudent.CONFIRMED);
        } else {
            binding.setStatus(GuardianStudent.UNBOUND);
        }
        bindingMapper.updateById(binding);
        publish(binding.getGuardianId(), binding.getStudentId(),
                GuardianStudent.CONFIRMED.equals(binding.getStatus()) ? "CONFIRMED" : "UNBOUND");
        return toDto(binding);
    }

    private void publish(Long guardianId, Long studentId, String status) {
        eventPublisher.publish(EVENT_BINDING_CHANGED, "user.guardian-binding", studentId, java.util.Map.of(
                "guardianId", guardianId, "studentId", studentId, "status", status));
    }

    private GuardianBindingDto toDto(GuardianStudent binding) {
        UserProfile guardian = profileMapper.selectById(binding.getGuardianId());
        UserProfile student = profileMapper.selectById(binding.getStudentId());
        GuardianProfile guardianSub = guardianProfileMapper.selectById(binding.getGuardianId());
        String phone = guardianSub == null || guardianSub.getPhoneEnc() == null
                ? null : AesCipher.maskTail(aes.decrypt(guardianSub.getPhoneEnc()), 3, 4);
        // 学生姓名保留明文：绑定确认需要核对孩子身份；是否随家长端确认流程脱敏待家长端设计裁定
        return new GuardianBindingDto(
                binding.getId(), binding.getGuardianId(),
                guardian == null ? null : AesCipher.maskName(guardian.getRealName()), phone,
                binding.getStudentId(), student == null ? null : student.getRealName(),
                binding.getRelation(), binding.getIsPrimary(), binding.getStatus());
    }
}
