package com.example.bookshop.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单明细 —— 对应表 t_order_item
 * <p>
 * 这是 t_order × t_book 的 M:N 关联表，并且带业务属性：
 * - quantity 购买数量
 * - price    下单时单价（快照，不是图书现价！）
 * <p>
 * 为什么必须存快照价？因为图书会调价，历史订单金额不能跟着变。
 */
@Data
@TableName("t_order_item")
public class OrderItem {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer orderId;

    private Integer bookId;

    private Integer quantity;

    /**
     * 下单时单价快照
     */
    private BigDecimal price;

    /**
     * 关联查询用：明细对应的图书（非数据库列）
     */
    @TableField(exist = false)
    private Book book;
}
