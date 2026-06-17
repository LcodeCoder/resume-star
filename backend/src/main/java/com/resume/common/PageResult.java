package com.resume.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 统一分页返回对象
 * 功能：承载后端分页查询结果，固定 records / total / page / size 结构，便于前端统一渲染 el-pagination。
 * @author 开发人员
 * @date 2026-06-17
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {
    /** 当前页数据 */
    private List<T> records;
    /** 符合条件的总条数（用于前端计算总页数） */
    private long total;
    /** 当前页码（从 1 开始） */
    private int page;
    /** 每页条数 */
    private int size;

    /** 构造分页结果 */
    public static <T> PageResult<T> of(List<T> records, long total, int page, int size) {
        return new PageResult<>(records, total, page, size);
    }

    /** 空分页结果 */
    public static <T> PageResult<T> empty(int page, int size) {
        return new PageResult<>(List.of(), 0L, page, size);
    }
}
