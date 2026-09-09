// 域/模块: 平台底座/工程规范
// 类型: 公共契约模型
// 职责: 分页响应契约——data 固定 { list, total, page, size }（api-conventions §3）
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.common.result;

import java.util.List;

/**
 * 分页响应：分页接口的 data 固定形态。
 */
public class PageResult<T> {

    private List<T> list;
    private long total;
    private int page;
    private int size;

    public PageResult() {
    }

    public PageResult(List<T> list, long total, int page, int size) {
        this.list = list;
        this.total = total;
        this.page = page;
        this.size = size;
    }

    public static <T> PageResult<T> of(List<T> list, long total, PageQuery query) {
        return new PageResult<>(list, total, query.getPage(), query.limitedSize());
    }

    public List<T> getList() {
        return list;
    }

    public long getTotal() {
        return total;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }
}
