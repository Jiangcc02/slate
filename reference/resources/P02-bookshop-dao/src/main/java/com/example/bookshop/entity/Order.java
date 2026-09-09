package com.example.bookshop.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单实体 —— 对应表 t_order
 * <p>
 * ⚠️ 易错点：Order 是 SQL 关键字（ORDER BY），表名必须写成 t_order，
 * 并且靠 @TableName("t_order") 绑定；如果直接建表叫 order 会语法报错。
 * <p>
 * 另：receiver 是「收货信息快照」，下单那一刻就固化，
 * 后续用户改地址不应影响历史订单（P01 4.x 节弱实体部分讲过）。
 */
@Data
@TableName("t_order")
public class Order {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String orderNo;

    private Integer userId;

    private BigDecimal totalAmount;

    /**
     * 0=待支付，1=已支付，2=已发货，3=已完成，4=已取消
     */
    private Integer status;

    /**
     * 收货信息快照
     */
    private String receiver;

    private LocalDateTime createdAt;

    private LocalDateTime paidAt;

    /**
     * 关联查询用：订单明细列表（非数据库列）
     */
    @TableField(exist = false)
    private List<OrderItem> items;

    /**
     * 关联查询用：下单用户（非数据库列）
     */
    @TableField(exist = false)
    private User user;
}
