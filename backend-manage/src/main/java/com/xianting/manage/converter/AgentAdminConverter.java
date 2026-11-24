package com.xianting.manage.converter;

import com.xianting.manage.dto.AgentAdminDto;
import com.xianting.manage.dto.AgentDetailDto;
import com.xianting.manage.dto.AgentSaveRequest;
import com.xianting.manage.entity.Agent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.ArrayList;
import java.util.List;

/** Agent 实体 <-> 管理端 DTO 转换器 */
@Mapper(componentModel = "spring")
public interface AgentAdminConverter {

    @Mapping(target = "id", source = "agent.publicId")
    @Mapping(target = "departmentId", source = "departmentPublicId")
    @Mapping(target = "departmentName", source = "departmentName")
    @Mapping(target = "activePromptVersion", source = "activePromptVersion")
    AgentAdminDto toDto(Agent agent, String departmentPublicId, String departmentName, Integer activePromptVersion);

    /** 详情多出的 activePrompt 为跨聚合数据，由 Service 组装后 set，不在转换器内处理 */
    @Mapping(target = "id", source = "agent.publicId")
    @Mapping(target = "departmentId", source = "departmentPublicId")
    @Mapping(target = "departmentName", source = "departmentName")
    @Mapping(target = "activePromptVersion", source = "activePromptVersion")
    @Mapping(target = "activePrompt", ignore = true)
    AgentDetailDto toDetailDto(Agent agent, String departmentPublicId, String departmentName, Integer activePromptVersion);

    /** publicId/departmentId/conversationStarters 由 Service 处理（UUID 生成、部门解析、JSON 序列化） */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "departmentId", ignore = true)
    @Mapping(target = "conversationStarters", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Agent toEntity(AgentSaveRequest request);

    /** 实体字段是 JSON 字符串，DTO 字段是列表：MapStruct 自动选用该方法（照抄客户端 AgentConverter） */
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
