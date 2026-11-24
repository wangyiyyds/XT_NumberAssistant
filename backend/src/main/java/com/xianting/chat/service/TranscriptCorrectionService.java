package com.xianting.chat.service;

/**
 * 对本地语音识别产生的原始文本进行二次校对。
 *
 * <p>这个接口刻意与 {@link AiService} 分开：前者只允许做保真校对，不能沿用聊天
 * 智能体的开放式人设或历史消息，避免模型把口播内容扩写成新的文案。</p>
 */
public interface TranscriptCorrectionService {

    /**
     * 修正明显识别错误、标点和分段，保留原文的事实、数字和表达。
     */
    String correct(String transcript);
}
