// 域/模块: 平台底座/文件服务
// 类型: Mapper
// 职责: file_object 表访问（含回收任务用的含逻辑删行查询）
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.platform.internal.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.slate.platform.internal.file.entity.FileObject;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface FileObjectMapper extends BaseMapper<FileObject> {

    /** 软删超过保留期的行（原生 SQL 绕开 @TableLogic 的 deleted=0 过滤；updated_at 即软删时刻） */
    @Select("SELECT id, bucket, object_key, uploaded_by, updated_at FROM file_object "
            + "WHERE deleted = 1 AND updated_at < #{cutoff} LIMIT 500")
    List<FileObject> selectSoftDeletedBefore(@Param("cutoff") LocalDateTime cutoff);

    /** 对象键存在性（含已软删行——孤儿判定要看全量元数据，原生 SQL 同上） */
    @Select("<script>SELECT object_key FROM file_object WHERE object_key IN "
            + "<foreach collection='keys' item='k' open='(' separator=',' close=')'>#{k}</foreach></script>")
    List<String> selectExistingObjectKeys(@Param("keys") List<String> keys);
}
