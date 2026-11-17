package com.xianting.chat.converter;

import com.xianting.chat.dto.BalanceDto;
import com.xianting.chat.entity.Balance;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Balance 实体 <-> BalanceDto 转换器 */
@Mapper(componentModel = "spring")
public interface BalanceConverter {

    /** balance 可能为 null（用户未初始化余额），保持原接口的兜底行为 */
    @Mapping(target = "tokenCredits", expression = "java(balance == null ? 0L : balance.getTokenCredits())")
    @Mapping(target = "autoRefillEnabled", expression = "java(balance != null && Boolean.TRUE.equals(balance.getAutoRefillEnabled()))")
    @Mapping(target = "expireAt", expression = "java(balance == null ? null : balance.getExpireAt())")
    @Mapping(target = "user", source = "userPublicId")
    BalanceDto toDto(Balance balance, String userPublicId);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    Balance toEntity(BalanceDto dto);
}
