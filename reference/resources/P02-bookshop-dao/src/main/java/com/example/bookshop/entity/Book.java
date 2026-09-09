package com.example.bookshop.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 图书实体 —— 对应表 t_book
 * <p>
 * 注意 price 用 BigDecimal 而不是 double：
 * 金额计算必须精确，double 会有 0.1+0.2=0.30000000000000004 的精度丢失。
 */
@Data
@TableName("t_book")
public class Book {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String title;

    private String author;

    private String isbn;

    /**
     * 价格：必须用 BigDecimal
     */
    private BigDecimal price;

    private Integer stock;

    private String coverUrl;

    private String description;

    private Integer categoryId;

    /**
     * 1=在售，0=下架
     */
    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /**
     * 关联查询用：所属分类（非数据库列）
     */
    @TableField(exist = false)
    private Category category;
}
