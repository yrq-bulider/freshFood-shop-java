package com.yan.freshfood.common.response;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

@Data
@NoArgsConstructor
public class PageR<T> {

    private List<T> list;
    private Long total;
    private Integer pageNum;
    private Integer pageSize;
    private Integer pages;

    public static <T> PageR<T> of(IPage<T> page) {
        PageR<T> r = new PageR<>();
        r.setList(page.getRecords());
        r.setTotal(page.getTotal());
        r.setPageNum((int) page.getCurrent());
        r.setPageSize((int) page.getSize());
        r.setPages((int) page.getPages());
        return r;
    }

    public static <T> PageR<T> empty(int pageNum, int pageSize) {
        PageR<T> r = new PageR<>();
        r.setList(Collections.emptyList());
        r.setTotal(0L);
        r.setPageNum(pageNum);
        r.setPageSize(pageSize);
        r.setPages(0);
        return r;
    }
}