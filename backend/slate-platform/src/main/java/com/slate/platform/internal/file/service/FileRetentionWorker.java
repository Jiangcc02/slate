// 域/模块: 平台底座/文件服务
// 类型: 定时回收任务
// 职责: MinIO 对象回收——软删元数据满保留期后物理删除对象；无登记孤儿对象超时回收（存储只增不减的治理闭环）
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.platform.internal.file.service;

import com.slate.platform.internal.config.RetentionProperties;
import com.slate.platform.internal.file.entity.FileObject;
import com.slate.platform.internal.file.mapper.FileObjectMapper;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import io.minio.Result;
import io.minio.messages.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 每日执行（与 RetentionWorker 同一单线程调度器串行）：
 * ① 软删满 slate.retention.deleted-file-days 的元数据行 → 删除 MinIO 对象（行保留为墓碑）；
 * ② MinIO 中无元数据且上传时间超过 slate.retention.orphan-file-hours 的孤儿对象 → 删除
 *   （上传未登记/登记失败的残留；48h 覆盖 30min 上传凭证与可能的登记重试窗口）。
 * 单轮孤儿扫描/删除均有上限，防止首次运行拖垮调度线程。
 */
@Component
public class FileRetentionWorker {

    private static final Logger log = LoggerFactory.getLogger(FileRetentionWorker.class);
    private static final int ORPHAN_SCAN_LIMIT = 1000;
    private static final int ORPHAN_DELETE_LIMIT = 200;

    private final FileObjectMapper fileMapper;
    private final MinioClient minio;
    private final String bucket;
    private final RetentionProperties retention;

    public FileRetentionWorker(FileObjectMapper fileMapper,
                               MinioClient minio,
                               @Value("${slate.storage.bucket}") String bucket,
                               RetentionProperties retention) {
        this.fileMapper = fileMapper;
        this.minio = minio;
        this.bucket = bucket;
        this.retention = retention;
    }

    @Scheduled(cron = "${slate.retention.cron:0 30 4 * * *}")
    public void cleanup() {
        if (!retention.enabled()) {
            return;
        }
        try {
            purgeSoftDeleted();
        } catch (Exception e) {
            log.error("软删文件对象回收失败: {}", e.getMessage());
        }
        try {
            purgeOrphans();
        } catch (Exception e) {
            log.error("孤儿对象回收失败: {}", e.getMessage());
        }
    }

    private void purgeSoftDeleted() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retention.deletedFileDays());
        List<FileObject> rows = fileMapper.selectSoftDeletedBefore(cutoff);
        int removed = 0;
        for (FileObject row : rows) {
            if (removeQuietly(row.getObjectKey())) {
                removed++;
            }
        }
        if (removed > 0) {
            log.info("软删文件对象回收完成: 删除 {} 个对象（保留期 {} 天）", removed, retention.deletedFileDays());
        }
    }

    private void purgeOrphans() {
        LocalDateTime orphanBefore = LocalDateTime.now().minusHours(retention.orphanFileHours());
        List<String> candidates = new ArrayList<>();
        Iterable<Result<Item>> results = minio.listObjects(ListObjectsArgs.builder()
                .bucket(bucket).maxKeys(ORPHAN_SCAN_LIMIT).build());
        for (Result<Item> result : results) {
            try {
                Item item = result.get();
                if (item.lastModified() == null
                        || item.lastModified().toLocalDateTime().isAfter(orphanBefore)) {
                    continue;
                }
                candidates.add(item.objectName());
            } catch (Exception e) {
                log.warn("孤儿扫描读取对象元数据失败，跳过: {}", e.getMessage());
            }
        }
        if (candidates.isEmpty()) {
            return;
        }
        Set<String> registered = new HashSet<>(fileMapper.selectExistingObjectKeys(candidates));
        int removed = 0;
        for (String key : candidates) {
            if (removed >= ORPHAN_DELETE_LIMIT) {
                log.warn("孤儿对象单轮删除达上限 {}，其余下轮处理", ORPHAN_DELETE_LIMIT);
                break;
            }
            if (!registered.contains(key) && removeQuietly(key)) {
                removed++;
            }
        }
        if (removed > 0) {
            log.info("孤儿对象回收完成: 删除 {} 个（无元数据且超过 {} 小时）", removed, retention.orphanFileHours());
        }
    }

    private boolean removeQuietly(String objectKey) {
        try {
            minio.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(objectKey).build());
            return true;
        } catch (Exception e) {
            log.warn("对象删除失败（下轮重试）: objectKey={}, error={}", objectKey, e.getMessage());
            return false;
        }
    }
}
