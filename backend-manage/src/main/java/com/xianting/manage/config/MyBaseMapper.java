package com.xianting.manage.config;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * 项目通用 Mapper 父接口：继承 MyBatis-Plus 的 {@link BaseMapper}{@code <T>}，
 * 所有 Mapper 均继承本接口。
 *
 * @param <T> 实体类型，与各 Mapper 对应的实体类一致
 */
public interface MyBaseMapper<T> extends BaseMapper<T> {
}
