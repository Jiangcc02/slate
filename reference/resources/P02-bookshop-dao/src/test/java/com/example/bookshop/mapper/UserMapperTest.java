package com.example.bookshop.mapper;

import com.example.bookshop.entity.Order;
import com.example.bookshop.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UserMapper 测试 —— 5 个用例覆盖本課核心点
 * <p>
 * 运行前确认：
 * 1. MySQL 已启动，且执行过 P01 的 bookshop.sql
 * 2. application.yml 里的密码改成你自己的
 * 3. IDEA 已开启 Lombok 注解处理（Settings → Build → Compiler → Annotation Processors）
 */
@SpringBootTest
class UserMapperTest {

    @Autowired
    private UserMapper userMapper;

    /**
     * 用例1：BaseMapper 自带方法 —— 主键查询
     */
    @Test
    void testSelectById() {
        User user = userMapper.selectById(1);
        assertNotNull(user, "id=1 的用户应存在（bookshop.sql 已插入 zhangsan）");
        System.out.println("查询到用户：" + user.getUsername());
    }

    /**
     * 用例2：BaseMapper 自带方法 —— 条件构造器
     */
    @Test
    void testSelectByWrapper() {
        List<User> users = userMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>().eq(
                                                                                                                                      User::getStatus,
                                                                                                                                      1)          // status = 1
                                                                                                                              .orderByDesc(
                                                                                                                                      User::getId));
        assertFalse(users.isEmpty(), "应至少有 1 个正常状态用户");
        System.out.println("正常用户数：" + users.size());
    }

    /**
     * 用例3：关联查询 —— 用户 + 订单（1:N，验证 <collection> 折叠）
     */
    @Test
    void testSelectUserWithOrders() {
        User user = userMapper.selectUserWithOrders(1);
        assertNotNull(user);

        List<Order> orders = user.getOrders();
        // 注意：LEFT JOIN 下，没有订单的用户 orders 会是一个空集合，而不是 null
        assertNotNull(orders, "orders 不应为 null（LEFT JOIN 会折叠成空集合）");
        System.out.println("用户 " + user.getUsername() + " 有 " + orders.size() + " 个订单");
    }

    /**
     * 用例4：动态 SQL —— 条件为 null 时应忽略该条件
     */
    @Test
    void testSelectByCondition() {
        // 4.1 两个条件都给
        List<User> r1 = userMapper.selectByCondition("zhang", 1);
        System.out.println("keyword=zhang, status=1 → " + r1.size() + " 条");

        // 4.2 只给 keyword（status 传 null，AND status=? 应被 <where> 忽略）
        List<User> r2 = userMapper.selectByCondition("zhang", null);
        assertTrue(r2.size() >= r1.size(), "条件放宽后结果不应变少");

        // 4.3 两个条件都空 → 查全表
        List<User> r3 = userMapper.selectByCondition(null, null);
        assertTrue(r3.size() >= r2.size(), "无条件应返回最多");
        System.out.println("无条件 → " + r3.size() + " 条");
    }

    /**
     * 用例5：插入 + 批量插入，方法结束后清理测试数据
     */
    @Test
    void testInsertAndBatchInsert() {
        // 5.1 单条插入，验证自增主键回填
        User u = new User();
        u.setUsername("test_" + System.currentTimeMillis());
        u.setPassword("123456");
        u.setEmail("t@example.com");
        u.setPhone("13700000000");
        u.setStatus(1);

        int rows = userMapper.insert(u);
        assertEquals(1, rows);
        assertNotNull(u.getId(), "插入后主键应回填到对象上");
        System.out.println("新用户ID = " + u.getId());

        // 5.2 批量插入（foreach）
        List<User> batch = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            User x = new User();
            x.setUsername("batch_" + System.currentTimeMillis() + "_" + i);
            x.setPassword("123456");
            x.setStatus(1);
            batch.add(x);
        }
        assertEquals(3, userMapper.batchInsert(batch), "批量插入应影响 3 行");

        // 5.3 清理
        userMapper.deleteById(u.getId());
        assertNull(userMapper.selectById(u.getId()), "删除后应查不到");
    }
}
