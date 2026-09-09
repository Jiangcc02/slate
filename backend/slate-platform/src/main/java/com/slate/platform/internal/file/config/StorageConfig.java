// 域/模块: 平台底座/文件服务
// 类型: 配置
// 职责: MinioClient 装配（端点/凭证来自配置；构造不连接，懒确保 bucket——启动期不强依赖对象存储）
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.platform.internal.file.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(FileProperties.class)
public class StorageConfig {

    @Bean
    public MinioClient minioClient(@Value("${slate.storage.endpoint}") String endpoint,
                                    @Value("${slate.storage.access-key}") String accessKey,
                                    @Value("${slate.storage.secret-key}") String secretKey) {
        return MinioClient.builder().endpoint(endpoint).credentials(accessKey, secretKey).build();
    }
}
