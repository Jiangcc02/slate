package com.example.bookshop.mapper;

import com.example.bookshop.entity.Book;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BookMapper 测试 —— 覆盖关联查询 / 动态 SQL / 库存扣减
 */
@SpringBootTest
class BookMapperTest {

    @Autowired
    private BookMapper bookMapper;

    /**
     * 用例1：N:1 关联查询（book → category），验证 <association>
     */
    @Test
    void testSelectBookWithCategory() {
        List<Book> books = bookMapper.selectBookWithCategory();
        assertFalse(books.isEmpty(), "bookshop.sql 已插入 3 本书");

        Book first = books.get(0);
        assertNotNull(first.getCategory(), "分类应被关联出来");
        System.out.println(first.getTitle() + " → 分类：" + first.getCategory().getName());
    }

    /**
     * 用例2：动态 SQL 四条件自由组合
     */
    @Test
    void testSearch() {
        // 全空 → 全部在售图书
        List<Book> all = bookMapper.search(null, null, null, null);
        System.out.println("无条件：" + all.size() + " 本");

        // 关键字
        List<Book> kw = bookMapper.search("Java", null, null, null);
        assertTrue(kw.size() <= all.size());
        System.out.println("关键字 Java：" + kw.size() + " 本");

        // 价格区间 0 ~ 50
        List<Book> cheap = bookMapper.search(null, null, new BigDecimal("0"), new BigDecimal("50"));
        assertTrue(cheap.stream().allMatch(b -> b.getPrice().compareTo(new BigDecimal("50")) <= 0),
                   "结果应都在 50 元以内");
        System.out.println("50 元以内：" + cheap.size() + " 本");
    }

    /**
     * 用例3：库存扣减 —— 防超卖
     * 故意扣一个超过库存的数量，期望影响 0 行（而不是把库存扣成负数）
     */
    @Test
    void testDeductStock() {
        Book book = bookMapper.selectBookWithCategory().get(0);
        int stock = book.getStock();

        // 3.1 正常扣 1 件
        int r1 = bookMapper.deductStock(book.getId(), 1);
        assertEquals(1, r1, "库存充足时应扣减成功");
        assertEquals(stock - 1, bookMapper.selectById(book.getId()).getStock());

        // 3.2 超量扣减：库存 + 1000，必然不足 → 应影响 0 行
        int r2 = bookMapper.deductStock(book.getId(), stock + 1000);
        assertEquals(0, r2, "库存不足时必须扣减失败（防超卖）");
        System.out.println("防超卖验证通过，当前库存：" + bookMapper.selectById(book.getId()).getStock());
    }
}
