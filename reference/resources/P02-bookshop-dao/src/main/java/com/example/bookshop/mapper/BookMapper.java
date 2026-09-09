package com.example.bookshop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.bookshop.entity.Book;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * 图书 Mapper
 */
public interface BookMapper extends BaseMapper<Book> {

    /**
     * 多表 JOIN：图书 + 分类名
     */
    List<Book> selectBookWithCategory();

    /**
     * 动态 SQL：价格区间 + 关键字 + 分类，三个条件都可为空
     */
    List<Book> search(
            @Param("keyword") String keyword,
            @Param("categoryId") Integer categoryId,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice
    );

    /**
     * 库存扣减（乐观锁式更新，防止超卖）
     * SQL：UPDATE t_book SET stock = stock - #{n} WHERE id = #{id} AND stock >= #{n}
     *
     * @return 影响行数，0 表示库存不足扣减失败
     */
    int deductStock(@Param("id") Integer id, @Param("num") Integer num);
}
