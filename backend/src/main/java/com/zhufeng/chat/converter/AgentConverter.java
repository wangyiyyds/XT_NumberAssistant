package com.zhufeng.chat.converter;

import com.zhufeng.chat.dto.AgentDto;
import com.zhufeng.chat.entity.Agent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.ArrayList;
import java.util.List;

/** Agent 实体 <-> AgentDto 转换器 */
@Mapper(componentModel = "spring")
public interface AgentConverter {

    @Mapping(target = "id", source = "agent.publicId")
    @Mapping(target = "agentId", source = "agent.publicId")
    @Mapping(target = "department", source = "departmentName")
    AgentDto toDto(Agent agent, String departmentName);

    @Mapping(target = "publicId", source = "dto.id")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "departmentId", ignore = true)
    @Mapping(target = "conversationStarters", ignore = true)
    Agent toEntity(AgentDto dto);

    /** 实体字段是 JSON 字符串，DTO 字段是列表：MapStruct 自动选用该方法 */
    default List<String> toStarters(String json) {
        if (json == null || json.isBlank()) return List.of();
        String s = json.trim();
        if (!s.startsWith("[")) return List.of();
        List<String> out = new ArrayList<>();
        for (String part : s.substring(1, s.length() - 1).split("\",\"")) {
            String v = part.replace("\"", "").trim();
            if (!v.isEmpty()) out.add(v);
        }
        return out;
    }
}
