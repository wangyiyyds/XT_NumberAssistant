package com.zhufeng.chat.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/** 灵感视频 DTO，对外字段与原接口一致 */
@Data
public class InspirationVideoDto {
    /** 视频直接用数据库 id 对外 */
    @JsonProperty("_id")
    private Long id;
    private String author;
    private String fans;
    private String duration;
    private String publishDate;
    private String title;
    private String likes;
    private String coverUrl;
    private String videoUrl;
    private boolean lowFansHighLikes;
}
