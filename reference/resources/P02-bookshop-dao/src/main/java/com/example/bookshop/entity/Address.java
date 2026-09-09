package com.example.bookshop.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 收货地址 —— 对应表 t_address
 * <p>
 * 这是「弱实体」的落地：地址离开用户没有意义。
 * DDL 里 user_id NOT NULL + ON DELETE CASCADE，
 * 所以 Java 侧 userId 也不应为空（业务层校验）。
 */
@Data
@TableName("t_address")
public class Address {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer userId;

    private String receiver;

    private String phone;

    private String province;

    private String city;

    private String detail;

    /**
     * 1=默认地址，0=非默认
     */
    private Integer isDefault;
}
