package com.example.bookshop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.bookshop.entity.User;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户 Mapper
 * <p>
 * 继承 BaseMapper<User> 后，MyBatis-Plus 直接赠送 17 个常用方法：
 * insert / deleteById / deleteByIds / updateById / selectById
 * selectList(Wrapper) / selectPage / selectCount ...
 * 只有「关联查询」和「动态条件」才需要自己写 XML（见 UserMapper.xml）。
 */
public interface UserMapper extends BaseMapper<User> {

    /**
     * 关联查询：查用户的同时带出他的所有订单（1:N）
     * 实现方式：resultMap + <collection>，一次 JOIN 查出后由 MyBatis 折叠去重
     *
     * @param id 用户ID
     * @return 用户（orders 已填充），查不到返回 null
     */
    User selectUserWithOrders(@Param("id") Integer id);

    /**
     * 动态 SQL 示例：多条件模糊查询
     * keyword 为 null 或空串时，自动忽略该条件（见 XML 的 <if> 标签）
     *
     * @param keyword 用户名关键字
     * @param status  状态，null 表示不限
     * @return 命中用户列表
     */
    List<User> selectByCondition(@Param("keyword") String keyword, @Param("status") Integer status);

    /**
     * 批量插入演示（foreach 标签）
     */
    int batchInsert(@Param("list") List<User> list);
}
