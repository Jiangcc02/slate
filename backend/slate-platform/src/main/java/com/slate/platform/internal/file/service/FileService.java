// 域/模块: 平台底座/文件服务
// 类型: 服务
// 职责: 文件上传/下载全流程——预签名直传（30min）、登记校验（白名单/大小/键一致）、短时效下载 URL（15min）、引用保护删除
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.platform.internal.file.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.slate.common.error.BusinessException;
import com.slate.common.result.PageQuery;
import com.slate.common.result.PageResult;
import com.slate.framework.error.SysErrorCode;
import com.slate.framework.id.SnowflakeIdGenerator;
import com.slate.platform.api.file.FileDto;
import com.slate.platform.api.file.UploadCredentials;
import com.slate.platform.internal.file.entity.FileObject;
import com.slate.platform.internal.file.config.FileProperties;
import com.slate.platform.internal.file.error.FileErrorCode;
import com.slate.platform.internal.file.mapper.FileObjectMapper;
import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.http.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class FileService {

    private static final Logger log = LoggerFactory.getLogger(FileService.class);
    private static final Duration UPLOAD_URL_TTL = Duration.ofMinutes(30);
    private static final Duration DOWNLOAD_URL_TTL = Duration.ofMinutes(15);
    private static final DateTimeFormatter QUOTA_DAY = DateTimeFormatter.BASIC_ISO_DATE;

    private final FileObjectMapper fileMapper;
    private final SnowflakeIdGenerator idGenerator;
    private final MinioClient minio;
    private final String bucket;
    private final Map<String, FileProperties.WhitelistRule> whitelist;
    private final long dailyUploadQuota;
    private final StringRedisTemplate redis;
    /** bucket 就绪标记：懒确保一次成功后不再每次预签名都发起 bucketExists 往返 */
    private volatile boolean bucketReady = false;

    public FileService(FileObjectMapper fileMapper,
                       SnowflakeIdGenerator idGenerator,
                       MinioClient minio,
                       @Value("${slate.storage.bucket}") String bucket,
                       FileProperties fileProperties,
                       StringRedisTemplate redis) {
        this.fileMapper = fileMapper;
        this.idGenerator = idGenerator;
        this.minio = minio;
        this.bucket = bucket;
        this.whitelist = fileProperties.whitelist() == null ? Map.of() : fileProperties.whitelist();
        this.dailyUploadQuota = fileProperties.dailyUploadQuota();
        this.redis = redis;
    }

    /** 申请预签名上传：白名单/大小前置校验（FILE-002/003）+ 每用户每日签发配额（FILE-003），返回 objectKey + 30min 直传 URL */
    public UploadCredentials issueUploadCredentials(UploadCredentials.Request request, Long requesterUserId) {
        FileProperties.WhitelistRule rule = requireRule(request.bizType());
        String extension = extensionOf(request.fileName());
        if (!rule.extensions().contains(extension)) {
            throw new BusinessException(FileErrorCode.FILE_002,
                    request.bizType() + " 不允许扩展名 " + extension + "，允许: " + rule.extensions());
        }
        if (request.sizeBytes() > rule.maxSizeBytes()) {
            throw new BusinessException(FileErrorCode.FILE_003,
                    "大小超限：" + request.sizeBytes() + " > " + rule.maxSizeBytes());
        }
        String quotaKey = "file:quota:" + requesterUserId + ":" + LocalDate.now().format(QUOTA_DAY);
        Long issued = redis.opsForValue().increment(quotaKey);
        if (issued != null) {
            if (issued == 1L) {
                redis.expire(quotaKey, Duration.ofHours(48));
            }
            if (issued > dailyUploadQuota) {
                throw new BusinessException(FileErrorCode.FILE_003, "今日上传签发配额已用尽（" + dailyUploadQuota + "）");
            }
        }
        String objectKey = request.bizType() + "/" + LocalDatePath.now() + "/"
                + idGenerator.nextId() + "-" + UUID.randomUUID().toString().substring(0, 8) + "." + extension;
        // 登记凭证绑定签发人与业务类型：register 复验（防冒名登记/类型漂移），一次性，登记成功即消费
        redis.opsForValue().set("file:key:" + objectKey, requesterUserId + ":" + request.bizType(), UPLOAD_URL_TTL);
        return new UploadCredentials(objectKey, presign(Method.PUT, objectKey),
                LocalDateTime.now().plus(UPLOAD_URL_TTL));
    }

    /** 上传完成登记元数据：objectKey 须为本系统签发的有效凭证（FILE-005）+ 白名单复验 + MinIO 实存核验（statObject）；
     *  大小以服务端读到的实际对象大小为准（客户端自报 sizeBytes 不可信，presigned PUT 无法限制请求体） */
    public FileDto register(UploadCredentials.RegisterRequest request, Long uploaderUserId) {
        FileProperties.WhitelistRule rule = requireRule(request.bizType());
        String credentialKey = "file:key:" + request.objectKey();
        String credential = redis.opsForValue().get(credentialKey);
        if (credential == null) {
            throw new BusinessException(FileErrorCode.FILE_005);
        }
        if (!credential.equals(uploaderUserId + ":" + request.bizType())) {
            throw new BusinessException(FileErrorCode.FILE_005, "登记人与签发人或业务类型不一致");
        }
        Long actualSize = statSize(request.objectKey());
        if (actualSize == null) {
            throw new BusinessException(FileErrorCode.FILE_005, "对象尚未上传或不存在");
        }
        if (actualSize > rule.maxSizeBytes()) {
            throw new BusinessException(FileErrorCode.FILE_003,
                    "实际大小超限：" + actualSize + " > " + rule.maxSizeBytes());
        }
        FileObject file = new FileObject();
        file.setId(idGenerator.nextId());
        file.setBucket(bucket);
        file.setObjectKey(request.objectKey());
        file.setBizType(request.bizType());
        file.setBizId(request.bizId());
        file.setFileName(request.fileName());
        file.setContentType(request.contentType());
        file.setSizeBytes(actualSize);
        file.setVisibility("private");
        file.setUploadedBy(uploaderUserId);
        fileMapper.insert(file);
        redis.delete(credentialKey);   // 凭证一次性：登记成功即作废
        return toDto(file);
    }

    /** 预签名下载 URL：仅上传者或 file:manage 管理员（业务域分享规则随 bizType 归属接入）；
     *  无权与不存在同码返回，不泄露对象存在性 */
    public String downloadUrl(Long id, Long operatorUserId, boolean manager) {
        FileObject file = requireFile(id);
        if (!manager && !file.getUploadedBy().equals(operatorUserId)) {
            throw new BusinessException(FileErrorCode.FILE_001, "文件不存在或无权访问");
        }
        return presign(Method.GET, file.getObjectKey());
    }

    public PageResult<FileDto> search(String bizType, Long bizId, PageQuery query) {
        LambdaQueryWrapper<FileObject> wrapper = new LambdaQueryWrapper<FileObject>()
                .eq(bizType != null && !bizType.isBlank(), FileObject::getBizType, bizType)
                .eq(bizId != null, FileObject::getBizId, bizId)
                .orderByDesc(FileObject::getId);
        Page<FileObject> page = fileMapper.selectPage(new Page<>(query.getPage(), query.limitedSize()), wrapper);
        List<FileDto> dtos = page.getRecords().stream().map(this::toDto).toList();
        return new PageResult<>(dtos, page.getTotal(), query.getPage(), query.limitedSize());
    }

    /** 删除：非管理员禁删业务引用中文件（FILE-004）；管理员可强制删除（含误绑 bizId 的存量）；
     *  逻辑删元数据，物理对象由 FileRetentionWorker 按 slate.retention.deleted-file-days 延迟回收 */
    public void delete(Long id, Long operatorUserId, boolean manager) {
        FileObject file = requireFile(id);
        if (!manager) {
            if (file.getBizId() != null) {
                throw new BusinessException(FileErrorCode.FILE_004);
            }
            if (!file.getUploadedBy().equals(operatorUserId)) {
                throw new BusinessException(FileErrorCode.FILE_001, "文件不存在或无权访问");
            }
        }
        fileMapper.deleteById(id);
    }

    /** MinIO 对象实存与实际大小（对象不存在返回 null） */
    Long statSize(String objectKey) {
        try {
            return minio.statObject(StatObjectArgs.builder().bucket(bucket).object(objectKey).build()).size();
        } catch (Exception e) {
            return null;
        }
    }

    /** 懒确保 bucket 存在（首次成功后进程内缓存；并发首建的 BucketAlreadyOwnedByYou 视为已就绪） */
    private void ensureBucket() {
        if (bucketReady) {
            return;
        }
        try {
            if (!minio.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
                try {
                    minio.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                    log.info("MinIO bucket 已创建: {}", bucket);
                } catch (Exception e) {
                    if (!String.valueOf(e.getMessage()).contains("BucketAlreadyOwnedByYou")) {
                        throw e;
                    }
                }
            }
            bucketReady = true;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(SysErrorCode.SYS_999, "对象存储不可用");
        }
    }

    private String presign(Method method, String objectKey) {
        try {
            ensureBucket();
            return minio.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(method)
                    .bucket(bucket)
                    .object(objectKey)
                    .expiry((int) (method == Method.PUT ? UPLOAD_URL_TTL.getSeconds() : DOWNLOAD_URL_TTL.getSeconds()))
                    .build());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("预签名失败: objectKey={}, error={}", objectKey, e.getMessage());
            throw new BusinessException(SysErrorCode.SYS_999, "对象存储不可用");
        }
    }

    private FileProperties.WhitelistRule requireRule(String bizType) {
        FileProperties.WhitelistRule rule = whitelist.get(bizType);
        if (rule == null) {
            throw new BusinessException(FileErrorCode.FILE_002, "未知业务类型: " + bizType);
        }
        return rule;
    }

    private String extensionOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dot + 1).toLowerCase();
    }

    private FileObject requireFile(Long id) {
        FileObject file = fileMapper.selectById(id);
        if (file == null) {
            throw new BusinessException(FileErrorCode.FILE_001);
        }
        return file;
    }

    private FileDto toDto(FileObject file) {
        return new FileDto(file.getId(), file.getObjectKey(), file.getBizType(), file.getBizId(),
                file.getFileName(), file.getContentType(), file.getSizeBytes(), file.getVisibility(),
                file.getUploadedBy(), file.getCreatedAt());
    }

    /** 对象键的日期分目录（yyyy/MM/dd） */
    static final class LocalDatePath {
        static String now() {
            return java.time.LocalDate.now().toString().replace("-", "/");
        }
    }
}
