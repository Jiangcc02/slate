// 域/模块: 平台底座/工程规范
// 类型: 公共契约模型
// 职责: 分页/排序请求参数契约——page(1起) size(≤100) sort(field,desc 可多组)（api-conventions §5）
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.common.result;

import java.util.List;

/**
 * 分页查询请求：作为 Controller 查询参数的公共形态。
 */
public class PageQuery {

    public static final int MAX_SIZE = 100;
    public static final int DEFAULT_SIZE = 20;

    private int page = 1;
    private int size = DEFAULT_SIZE;
    private List<String> sort;

    public int offset() {
        return (Math.max(page, 1) - 1) * Math.min(Math.max(size, 1), MAX_SIZE);
    }

    public int limitedSize() {
        return Math.min(Math.max(size, 1), MAX_SIZE);
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public List<String> getSort() {
        return sort;
    }

    public void setSort(List<String> sort) {
        this.sort = sort;
    }
}
