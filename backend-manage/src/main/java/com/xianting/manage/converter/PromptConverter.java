package com.xianting.manage.converter;

import com.xianting.manage.dto.PromptDetailDto;
import com.xianting.manage.dto.PromptDto;
import com.xianting.manage.entity.Prompt;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Prompt 实体 -> 提示词 DTO 转换器（active_agent_id 生成列不在实体中，天然不参与映射） */
@Mapper(componentModel = "spring")
public interface PromptConverter {

    /** agentPublicId / parentVersion 为跨表数据，由 Service 解析后经入参注入 */
    @Mapping(target = "id", source = "prompt.publicId")
    @Mapping(target = "agentId", source = "agentPublicId")
    @Mapping(target = "parentVersion", source = "parentVersion")
    PromptDto toDto(Prompt prompt, String agentPublicId, Integer parentVersion);

    @Mapping(target = "id", source = "prompt.publicId")
    @Mapping(target = "agentId", source = "agentPublicId")
    @Mapping(target = "parentVersion", source = "parentVersion")
    PromptDetailDto toDetailDto(Prompt prompt, String agentPublicId, Integer parentVersion);
}
