// 域/模块: 平台底座/用户中心
// 类型: 服务
// 职责: 用户档案——建档即开户（BCrypt 随机密码）、检索（默认脱敏）、更新、账号生命周期（禁用即撤销全部登录态）
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.platform.internal.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.slate.common.error.BusinessException;
import com.slate.common.result.PageQuery;
import com.slate.common.result.PageResult;
import com.slate.framework.crypto.AesCipher;
import com.slate.framework.id.SnowflakeIdGenerator;
import com.slate.platform.api.user.UserDto;
import com.slate.platform.internal.auth.entity.Account;
import com.slate.platform.internal.auth.mapper.AccountMapper;
import com.slate.platform.internal.auth.service.TokenService;
import com.slate.platform.internal.user.entity.GuardianProfile;
import com.slate.platform.internal.user.entity.StudentProfile;
import com.slate.platform.internal.user.entity.TeacherProfile;
import com.slate.platform.internal.user.error.UserErrorCode;
import com.slate.platform.internal.user.mapper.GuardianProfileMapper;
import com.slate.platform.internal.user.mapper.StudentProfileMapper;
import com.slate.platform.internal.user.mapper.TeacherProfileMapper;
import com.slate.platform.internal.auth.entity.UserProfile;
import com.slate.platform.internal.auth.mapper.UserProfileMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserProfileMapper profileMapper;
    private final StudentProfileMapper studentMapper;
    private final TeacherProfileMapper teacherMapper;
    private final GuardianProfileMapper guardianMapper;
    private final AccountMapper accountMapper;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final SnowflakeIdGenerator idGenerator;
    private final AesCipher aes;
    private final SecureRandom random = new SecureRandom();

    public UserService(UserProfileMapper profileMapper,
                       StudentProfileMapper studentMapper,
                       TeacherProfileMapper teacherMapper,
                       GuardianProfileMapper guardianMapper,
                       AccountMapper accountMapper,
                       TokenService tokenService,
                       PasswordEncoder passwordEncoder,
                       SnowflakeIdGenerator idGenerator,
                       AesCipher aes) {
        this.profileMapper = profileMapper;
        this.studentMapper = studentMapper;
        this.teacherMapper = teacherMapper;
        this.guardianMapper = guardianMapper;
        this.accountMapper = accountMapper;
        this.tokenService = tokenService;
        this.passwordEncoder = passwordEncoder;
        this.idGenerator = idGenerator;
        this.aes = aes;
    }

    /** 检索：按角色类型/姓名/账号状态过滤；SQL 分页（总量下推数据库）；默认脱敏 */
    public PageResult<UserDto> search(String userType, String realName, String accountStatus, PageQuery query) {
        LambdaQueryWrapper<UserProfile> wrapper = new LambdaQueryWrapper<>();
        if (userType != null && !userType.isBlank()) {
            wrapper.eq(UserProfile::getUserType, userType);
        }
        if (realName != null && !realName.isBlank()) {
            wrapper.like(UserProfile::getRealName, escapeLike(realName.trim()));
        }
        if (accountStatus != null && !accountStatus.isBlank()) {
            // account 与 user_profile 1:1：exists 子查询把状态过滤下推数据库（参数绑定，无拼接注入面）
            wrapper.exists("SELECT 1 FROM account a WHERE a.user_id = user_profile.id AND a.status = {0}",
                    accountStatus);
        }
        wrapper.orderByDesc(UserProfile::getId);
        Page<UserProfile> result = profileMapper.selectPage(new Page<>(query.getPage(), query.limitedSize()), wrapper);
        List<UserProfile> page = result.getRecords();
        Map<Long, Account> accounts = page.isEmpty() ? Map.of()
                : accountMapper.selectList(new LambdaQueryWrapper<Account>()
                        .in(Account::getUserId, page.stream().map(UserProfile::getId).toList()))
                .stream().collect(Collectors.toMap(Account::getUserId, Function.identity()));
        Map<Long, StudentProfile> students = loadStudents(page);
        Map<Long, TeacherProfile> teachers = loadTeachers(page);
        Map<Long, GuardianProfile> guardians = loadGuardians(page);
        List<UserDto> dtos = page.stream()
                .map(p -> toDto(p, accounts.get(p.getId()), students.get(p.getId()),
                        teachers.get(p.getId()), guardians.get(p.getId()), false))
                .toList();
        return new PageResult<>(dtos, result.getTotal(), query.getPage(), query.limitedSize());
    }

    /** 详情；reveal=true 返回明文（需 user:lifecycle 权限，控制器控制） */
    public UserDto get(Long id, boolean reveal) {
        UserProfile profile = requireProfile(id);
        Account account = accountMapper.selectOne(new LambdaQueryWrapper<Account>()
                .eq(Account::getUserId, id));
        return toDto(profile, account,
                studentMapper.selectById(id), teacherMapper.selectById(id), guardianMapper.selectById(id),
                reveal);
    }

    /** 建档即开户：初始密码随机（8 位数字），返回一次（response.username 为自动生成账号） */
    @Transactional
    public UserDto create(UserDto.CreateRequest request) {
        assertPhoneAvailable(request.phone());
        assertStudentNoAvailable(request.studentNo());
        Long userId = idGenerator.nextId();
        UserProfile profile = new UserProfile();
        profile.setId(userId);
        profile.setRealName(request.realName());
        profile.setUserType(request.userType());
        profileMapper.insert(profile);
        fillSubProfile(userId, request, null);
        Account account = new Account();
        account.setId(idGenerator.nextId());
        account.setUsername(resolveUsername(request.username(), request.userType(), userId));
        account.setPasswordHash(passwordEncoder.encode(randomPassword()));
        account.setStatus("active");
        account.setUserId(userId);
        try {
            accountMapper.insert(account);
        } catch (DuplicateKeyException e) {
            // 查后插竞态由 uk_account_username 兜底：转业务码而非 500
            throw new BusinessException(UserErrorCode.USER_008);
        }
        return get(userId, false);
    }

    /** 更新：PATCH 语义 null=保留（漏传字段不清空存量）；学籍号冲突由唯一键兜底转业务码 */
    @Transactional
    public void update(Long id, UserDto.UpdateRequest request) {
        UserProfile profile = requireProfile(id);
        if (request.realName() != null && !request.realName().isBlank()) {
            profile.setRealName(request.realName());
        }
        profileMapper.updateById(profile);
        try {
            fillSubProfile(id, null, request);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(UserErrorCode.USER_007);
        }
    }

    /** 生命周期：disable 撤销全部 refresh；reset-password 返回一次性新密码 */
    @Transactional
    public UserDto.StatusResult lifecycle(Long id, String action) {
        Account account = accountMapper.selectOne(new LambdaQueryWrapper<Account>()
                .eq(Account::getUserId, id));
        if (account == null) {
            throw new BusinessException(UserErrorCode.USER_001);
        }
        switch (action) {
            case "activate" -> {
                account.setStatus("active");
                accountMapper.updateById(account);
                return new UserDto.StatusResult(action, null);
            }
            case "disable" -> {
                account.setStatus("disabled");
                accountMapper.updateById(account);
                tokenService.revokeAll(account.getId());
                return new UserDto.StatusResult(action, null);
            }
            case "reset-password" -> {
                String newPassword = randomPassword();
                account.setPasswordHash(passwordEncoder.encode(newPassword));
                accountMapper.updateById(account);
                tokenService.revokeAll(account.getId());
                return new UserDto.StatusResult(action, newPassword);
            }
            default -> throw new BusinessException(UserErrorCode.USER_001, "不支持的动作: " + action);
        }
    }

    UserProfile requireProfile(Long id) {
        UserProfile profile = profileMapper.selectById(id);
        if (profile == null) {
            throw new BusinessException(UserErrorCode.USER_001);
        }
        return profile;
    }

    /** 子档填充：create 全量、update PATCH 语义（null=保留，与契约对齐）；单次查库，insert/update 二选一 */
    private void fillSubProfile(Long userId, UserDto.CreateRequest create, UserDto.UpdateRequest update) {
        String userType = create != null ? create.userType() : requireProfile(userId).getUserType();
        switch (userType) {
            case "STUDENT" -> {
                StudentProfile student = studentMapper.selectById(userId);
                boolean insert = student == null;
                if (insert) {
                    student = new StudentProfile();
                    student.setUserId(userId);
                }
                if (create != null) {
                    applyStudentNo(student, create.studentNo());
                    student.setGradeEntry(create.gradeEntry());
                } else {
                    if (update.studentNo() != null) {
                        applyStudentNo(student, update.studentNo());
                    }
                    if (update.gradeEntry() != null) {
                        student.setGradeEntry(update.gradeEntry());
                    }
                }
                if (insert) {
                    studentMapper.insert(student);
                } else {
                    studentMapper.updateById(student);
                }
            }
            case "TEACHER" -> {
                TeacherProfile teacher = teacherMapper.selectById(userId);
                boolean insert = teacher == null;
                if (insert) {
                    teacher = new TeacherProfile();
                    teacher.setUserId(userId);
                }
                if (create != null) {
                    teacher.setStaffNo(create.staffNo());
                    teacher.setSubject(create.subject());
                    teacher.setTitle(create.title());
                } else {
                    if (update.staffNo() != null) {
                        teacher.setStaffNo(update.staffNo());
                    }
                    if (update.subject() != null) {
                        teacher.setSubject(update.subject());
                    }
                    if (update.title() != null) {
                        teacher.setTitle(update.title());
                    }
                }
                if (insert) {
                    teacherMapper.insert(teacher);
                } else {
                    teacherMapper.updateById(teacher);
                }
            }
            case "GUARDIAN" -> {   // 家长子档由绑定流程维护（手机号唯一凭据）
            }
            default -> {   // STAFF：主档即可
            }
        }
    }

    /** 学籍号：加密列 + 确定性哈希列同步写（空串=显式清空） */
    private void applyStudentNo(StudentProfile student, String plain) {
        String no = emptyToNull(plain);
        student.setStudentNo(no == null ? null : aes.encrypt(no));
        student.setStudentNoHash(no == null ? null : aes.hmac(no));
    }

    void assertPhoneAvailable(String phone) {
        if (phone == null || phone.isBlank()) {
            return;
        }
        if (guardianMapper.selectCount(new LambdaQueryWrapper<GuardianProfile>()
                .eq(GuardianProfile::getPhoneHash, aes.hmac(phone))) > 0) {
            throw new BusinessException(UserErrorCode.USER_002);
        }
    }

    void assertStudentNoAvailable(String studentNo) {
        if (studentNo == null || studentNo.isBlank()) {
            return;
        }
        if (studentMapper.selectCount(new LambdaQueryWrapper<StudentProfile>()
                .eq(StudentProfile::getStudentNoHash, aes.hmac(studentNo))) > 0) {
            throw new BusinessException(UserErrorCode.USER_007);
        }
    }

    /** LIKE 通配符转义：用户输入的 %/_ 按字面匹配，防扫描放大 */
    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private String resolveUsername(String requested, String userType, Long userId) {
        if (requested != null && !requested.isBlank()) {
            return requested;
        }
        String prefix = switch (userType) {
            case "STUDENT" -> "s";
            case "TEACHER" -> "t";
            case "GUARDIAN" -> "g";
            default -> "u";
        };
        return prefix + userId;
    }

    private String randomPassword() {
        return String.format("%08d", random.nextInt(100_000_000));
    }

    private UserDto toDto(UserProfile profile, Account account, StudentProfile student,
                          TeacherProfile teacher, GuardianProfile guardian, boolean reveal) {
        String studentNoPlain = student == null || student.getStudentNo() == null
                ? null : aes.decrypt(student.getStudentNo());
        String phonePlain = guardian == null || guardian.getPhoneEnc() == null
                ? null : aes.decrypt(guardian.getPhoneEnc());
        return new UserDto(
                profile.getId(),
                account == null ? null : account.getUsername(),
                reveal ? profile.getRealName() : AesCipher.maskName(profile.getRealName()),
                profile.getUserType(),
                account == null ? null : account.getStatus(),
                reveal ? studentNoPlain : (studentNoPlain == null ? null : AesCipher.maskTail(studentNoPlain, 0, 4)),
                student == null ? null : student.getGradeEntry(),
                teacher == null ? null : teacher.getStaffNo(),
                teacher == null ? null : teacher.getSubject(),
                teacher == null ? null : teacher.getTitle(),
                reveal ? phonePlain : (phonePlain == null ? null : AesCipher.maskTail(phonePlain, 3, 4)),
                profile.getCreatedAt());
    }

    private Map<Long, StudentProfile> loadStudents(List<UserProfile> page) {
        return page.stream().noneMatch(p -> "STUDENT".equals(p.getUserType())) ? Map.of()
                : studentMapper.selectBatchIds(page.stream()
                .filter(p -> "STUDENT".equals(p.getUserType())).map(UserProfile::getId).toList())
                .stream().collect(Collectors.toMap(StudentProfile::getUserId, Function.identity()));
    }

    private Map<Long, TeacherProfile> loadTeachers(List<UserProfile> page) {
        return page.stream().noneMatch(p -> "TEACHER".equals(p.getUserType())) ? Map.of()
                : teacherMapper.selectBatchIds(page.stream()
                .filter(p -> "TEACHER".equals(p.getUserType())).map(UserProfile::getId).toList())
                .stream().collect(Collectors.toMap(TeacherProfile::getUserId, Function.identity()));
    }

    private Map<Long, GuardianProfile> loadGuardians(List<UserProfile> page) {
        return page.stream().noneMatch(p -> "GUARDIAN".equals(p.getUserType())) ? Map.of()
                : guardianMapper.selectBatchIds(page.stream()
                .filter(p -> "GUARDIAN".equals(p.getUserType())).map(UserProfile::getId).toList())
                .stream().collect(Collectors.toMap(GuardianProfile::getUserId, Function.identity()));
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
