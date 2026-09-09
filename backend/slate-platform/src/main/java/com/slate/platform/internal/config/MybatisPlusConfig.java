// 域/模块: 平台底座/工程规范
// 类型: MyBatis-Plus 配置
// 职责: Mapper 扫描（新模块的 mapper 包在此追加）与分页插件
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.platform.internal.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling   // 领域事件投递器等定时任务
@MapperScan({"com.slate.platform.internal.auth.mapper", "com.slate.platform.internal.audit.mapper",
        "com.slate.platform.internal.events.mapper", "com.slate.platform.internal.org.mapper",
        "com.slate.platform.internal.user.mapper", "com.slate.platform.internal.msg.mapper"})
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
