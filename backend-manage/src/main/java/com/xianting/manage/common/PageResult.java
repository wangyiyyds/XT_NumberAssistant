package com.xianting.manage.common;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.util.List;

/**
 * 统一分页壳：MyBatis Plus 的 Page 实体含 orders/searchCount 等内部字段，
 * 不直接序列化给前端，一律转成本结构。records 为已转好的 DTO 列表。
 */
@Data
public class PageResult<T> {
    private long total;
    private long pages;
    private long current;
    private long size;
    private List<T> records;

    public static <T> PageResult<T> of(IPage<?> page, List<T> records) {
        PageResult<T> r = new PageResult<>();
        r.total = page.getTotal();
        r.pages = page.getPages();
        r.current = page.getCurrent();
        r.size = page.getSize();
        r.records = records;
        return r;
    }
}
