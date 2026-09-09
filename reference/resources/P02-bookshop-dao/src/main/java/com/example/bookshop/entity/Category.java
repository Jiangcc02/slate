package com.example.bookshop.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.List;

/**
 * 图书分类 —— 对应表 t_category
 * <p>
 * 自关联示例：parentId 指向本表的 id
 * - 顶级分类 parentId = NULL
 * - 二级分类 parentId = 父分类 id（如 计算机 → Java）
 */
@Data
@TableName("t_category")
public class Category {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String name;

    /**
     * 父分类ID，顶级为 null
     */
    private Integer parentId;

    private Integer sortOrder;

    /**
     * 关联查询用：子分类列表（非数据库列）
     */
    @TableField(exist = false)
    private List<Category> children;
}
