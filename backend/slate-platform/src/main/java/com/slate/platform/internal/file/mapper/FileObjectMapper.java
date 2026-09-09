// 域/模块: 平台底座/文件服务
// 类型: Mapper
// 职责: file_object 表访问
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.platform.internal.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.slate.platform.internal.file.entity.FileObject;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FileObjectMapper extends BaseMapper<FileObject> {
}
