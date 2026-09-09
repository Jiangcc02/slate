// 域/模块: 平台底座/文件服务
// 类型: 服务
// 职责: 文件上传/下载全流程——预签名直传（30min）、登记校验（白名单/大小/键一致）、短时效下载 URL（15min）、引用保护删除
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.platform.internal.file.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.slate.common.error.BusinessException;
import com.slate.common.result.PageQuery;
import com.slate.common.result.PageResult;
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
import io.minio.http.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class FileService {

    private static final Logger log = LoggerFactory.getLogger(FileService.class);
    private static final Duration UPLOAD_URL_TTL = Duration.ofMinutes(30);
    private static final Duration DOWNLOAD_URL_TTL = Duration.ofMinutes(15);

    private final FileObjectMapper fileMapper;
    private final SnowflakeIdGenerator idGenerator;
    private final MinioClient minio;
    private final String bucket;
    private final Map<String, FileProperties.WhitelistRule> whitelist;
    private final org.springframework.data.redis.core.StringRedisTemplate redis;

    public FileService(FileObjectMapper fileMapper,
                       SnowflakeIdGenerator idGenerator,
                       MinioClient minio,
                       @Value("${slate.storage.bucket}") String bucket,
                       FileProperties fileProperties,
                       org.springframework.data.redis.core.StringRedisTemplate redis) {
        this.fileMapper = fileMapper;
        this.idGenerator = idGenerator;
        this.minio = minio;
        this.bucket = bucket;
        this.whitelist = fileProperties.whitelist() == null ? Map.of() : fileProperties.whitelist();
        this.redis = redis;
    }

    /** 申请预签名上传：白名单与大小前置校验（FILE-002/003），返回 objectKey + 30min 直传 URL */
    public UploadCredentials issueUploadCredentials(UploadCredentials.Request request) {
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
        String objectKey = request.bizType() + "/" + LocalDatePath.now() + "/"
                + idGenerator.nextId() + "-" + UUID.randomUUID().toString().substring(0, 8) + "." + extension;
        redis.opsForValue().set("file:key:" + objectKey, "1", UPLOAD_URL_TTL);   // 登记凭证，register 时校验（FILE-005）
        return new UploadCredentials(objectKey, presign(Method.PUT, objectKey),
                LocalDateTime.now().plus(UPLOAD_URL_TTL));
    }

    /** 上传完成登记元数据：objectKey 必须为本系统签发的凭证键（Redis 校验 FILE-005）+ 白名单复验 */
    public FileDto register(UploadCredentials.RegisterRequest request, Long uploaderUserId) {
        FileProperties.WhitelistRule rule = requireRule(request.bizType());
        if (Boolean.TRUE != redis.hasKey("file:key:" + request.objectKey())) {
            throw new BusinessException(FileErrorCode.FILE_005);
        }
        if (request.sizeBytes() > rule.maxSizeBytes()) {
            throw new BusinessException(FileErrorCode.FILE_003);
        }
        FileObject file = new FileObject();
        file.setId(idGenerator.nextId());
        file.setBucket(bucket);
        file.setObjectKey(request.objectKey());
        file.setBizType(request.bizType());
        file.setBizId(request.bizId());
        file.setFileName(request.fileName());
        file.setContentType(request.contentType());
        file.setSizeBytes(request.sizeBytes());
        file.setVisibility("private");
        file.setUploadedBy(uploaderUserId);
        fileMapper.insert(file);
        return toDto(file);
    }

    /** 预签名下载 URL（private，15min 短时效） */
    public String downloadUrl(Long id) {
        FileObject file = requireFile(id);
        return presign(Method.GET, file.getObjectKey());
    }

    public PageResult<FileDto> search(String bizType, Long bizId, PageQuery query) {
        LambdaQueryWrapper<FileObject> wrapper = new LambdaQueryWrapper<FileObject>()
                .eq(bizType != null && !bizType.isBlank(), FileObject::getBizType, bizType)
                .eq(bizId != null, FileObject::getBizId, bizId)
                .orderByDesc(FileObject::getId);
        List<FileObject> all = fileMapper.selectList(wrapper);
        List<FileDto> page = all.stream().skip(query.offset()).limit(query.limitedSize())
                .map(this::toDto).toList();
        return new PageResult<>(page, all.size(), query.getPage(), query.limitedSize());
    }

    /** 删除：业务引用中禁止（FILE-004）；逻辑删元数据，对象异步清理（回收任务随规模引入） */
    public void delete(Long id, Long operatorUserId, boolean manager) {
        FileObject file = requireFile(id);
        if (file.getBizId() != null) {
            throw new BusinessException(FileErrorCode.FILE_004);
        }
        if (!manager && !file.getUploadedBy().equals(operatorUserId)) {
            throw new BusinessException(FileErrorCode.FILE_001, "仅上传者或管理员可删除");
        }
        fileMapper.deleteById(id);
    }

    /** 懒确保 bucket 存在（首次使用时建，避免启动期强依赖对象存储） */
    private void ensureBucket() {
        try {
            if (!minio.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
                minio.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("MinIO bucket 已创建: {}", bucket);
            }
        } catch (Exception e) {
            throw new BusinessException(FileErrorCode.FILE_001, "对象存储不可用: " + e.getMessage());
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
            throw new BusinessException(FileErrorCode.FILE_001, "预签名失败: " + e.getMessage());
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
