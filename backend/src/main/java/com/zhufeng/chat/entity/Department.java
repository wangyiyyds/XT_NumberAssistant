package com.zhufeng.chat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("t_department")
public class Department {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String publicId;
    private String name;
    private Boolean enabled;
    private Boolean brandOnly;
    private Integer sortOrder;
}
