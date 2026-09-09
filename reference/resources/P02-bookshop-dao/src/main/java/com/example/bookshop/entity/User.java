package com.example.bookshop.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户实体 —— 对应表 t_user
 * <p>
 * 三个要点（P02 重点知识）：
 * 1. @TableName：类名 User 与表名 t_user 不一致，必须显式声明
 * 2. @TableId(type = IdType.AUTO)：声明主键并走数据库自增
 * 3. LocalDateTime createdAt ⇄ 数据库 created_at，靠 application.yml 的
 * map-underscore-to-camel-case: true 自动映射
 * <p>
 * 另：orders 字段是非数据库列，必须加 @TableField(exist = false)，
 * 否则 MyBatis-Plus 会自动把它拼进 SELECT 导致报错。
 */
@Data
@TableName("t_user")
public class User {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String username;

    private String password;

    private String email;

    private String phone;

    /**
     * 1=正常，0=禁用
     */
    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /**
     * 关联查询用：一个用户的多个订单（非数据库列）
     */
    @TableField(exist = false)
    private List<Order> orders;
}
