package com.xianting.manage.mapper;

import com.xianting.manage.config.MyBaseMapper;
import com.xianting.manage.entity.Prompt;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface PromptMapper extends MyBaseMapper<Prompt> {

    /** 指定智能体当前的最大版本号（无版本时返回 0），新建版本时取 max+1 */
    @Select("SELECT COALESCE(MAX(version), 0) FROM t_prompt WHERE agent_id = #{agentId}")
    int maxVersionByAgentId(@Param("agentId") Long agentId);
}
