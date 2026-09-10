// 域/模块: 平台底座/用户中心
// 类型: 启动回填任务
// 职责: V2 迁移新增的确定性哈希列（student_no_hash/phone_hash）存量回填——解密原列取明文再 HMAC；幂等可重复
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.platform.internal.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.slate.framework.crypto.AesCipher;
import com.slate.platform.internal.user.entity.GuardianProfile;
import com.slate.platform.internal.user.entity.StudentProfile;
import com.slate.platform.internal.user.mapper.GuardianProfileMapper;
import com.slate.platform.internal.user.mapper.StudentProfileMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 每次启动执行：仅处理 hash 为空且密文非空的行，回填完成即空转（毫秒级）。
 * 解密失败的行（历史密钥不一致等）记 error 跳过，人工处理后下次启动重试；
 * 哈希撞唯一键（同号多档的历史脏数据）记 error 跳过——唯一键只保护新数据。
 */
@Component
public class HashBackfillRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(HashBackfillRunner.class);

    private final StudentProfileMapper studentMapper;
    private final GuardianProfileMapper guardianMapper;
    private final AesCipher aes;

    public HashBackfillRunner(StudentProfileMapper studentMapper,
                              GuardianProfileMapper guardianMapper,
                              AesCipher aes) {
        this.studentMapper = studentMapper;
        this.guardianMapper = guardianMapper;
        this.aes = aes;
    }

    @Override
    public void run(ApplicationArguments args) {
        backfillStudents();
        backfillGuardians();
    }

    private void backfillStudents() {
        List<StudentProfile> pending = studentMapper.selectList(new LambdaQueryWrapper<StudentProfile>()
                .isNull(StudentProfile::getStudentNoHash)
                .isNotNull(StudentProfile::getStudentNo));
        for (StudentProfile row : pending) {
            try {
                String hash = aes.hmac(aes.decrypt(row.getStudentNo()));
                StudentProfile patch = new StudentProfile();
                patch.setUserId(row.getUserId());
                patch.setStudentNoHash(hash);
                studentMapper.updateById(patch);
            } catch (DuplicateKeyException e) {
                log.error("学籍号哈希回填冲突（同号多档历史数据，跳过待人工合并）: userId={}", row.getUserId());
            } catch (Exception e) {
                log.error("学籍号哈希回填失败（解密异常，跳过）: userId={}, error={}", row.getUserId(), e.getMessage());
            }
        }
        if (!pending.isEmpty()) {
            log.info("学籍号哈希回填完成: 处理 {} 行", pending.size());
        }
    }

    private void backfillGuardians() {
        List<GuardianProfile> pending = guardianMapper.selectList(new LambdaQueryWrapper<GuardianProfile>()
                .isNull(GuardianProfile::getPhoneHash)
                .isNotNull(GuardianProfile::getPhoneEnc));
        for (GuardianProfile row : pending) {
            try {
                String hash = aes.hmac(aes.decrypt(row.getPhoneEnc()));
                GuardianProfile patch = new GuardianProfile();
                patch.setUserId(row.getUserId());
                patch.setPhoneHash(hash);
                guardianMapper.updateById(patch);
            } catch (DuplicateKeyException e) {
                log.error("家长手机号哈希回填冲突（同号多档历史数据，跳过待人工合并）: userId={}", row.getUserId());
            } catch (Exception e) {
                log.error("家长手机号哈希回填失败（解密异常，跳过）: userId={}, error={}", row.getUserId(), e.getMessage());
            }
        }
        if (!pending.isEmpty()) {
            log.info("家长手机号哈希回填完成: 处理 {} 行", pending.size());
        }
    }
}
