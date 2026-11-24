package com.xianting.chat.service;

/** 从公开视频链接提取口播或字幕文本。 */
public interface VideoCopywritingService {
    String extract(String videoUrl);
}
