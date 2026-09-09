package com.example.bookshop;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 启动类
 * <p>
 * ⚠️ @MapperScan 必须写！不写会报：
 * "No qualifying bean of type 'UserMapper' available"
 * <p>
 * 也可以在每个 Mapper 接口上加 @Mapper，但接口一多就麻烦，
 * 统一在启动类扫描是更常见的做法。
 */
@MapperScan("com.example.bookshop.mapper")
@SpringBootApplication
public class BookshopApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookshopApplication.class, args);
    }
}
