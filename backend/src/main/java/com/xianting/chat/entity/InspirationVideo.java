package com.xianting.chat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_inspiration_video")
public class InspirationVideo {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long keywordId;
    private String author;
    private String fans;
    private String duration;
    private String publishDate;
    private String title;
    private String likes;
    private String coverUrl;
    private String videoUrl;
    private Boolean lowFansHighLikes;
    private LocalDateTime createdAt;
}
