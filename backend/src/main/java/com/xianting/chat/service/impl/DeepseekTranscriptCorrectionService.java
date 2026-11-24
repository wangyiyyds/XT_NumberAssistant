package com.xianting.chat.service.impl;

import com.xianting.chat.common.ApiException;
import com.xianting.chat.service.TranscriptCorrectionService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/** DeepSeek 文本校对阶段：只接收本地 ASR 的结果，不接触视频或音频。 */
@Service
public class DeepseekTranscriptCorrectionService implements TranscriptCorrectionService {

    /** 留出提示词与模型输出空间，长视频按自然段/句末切块校正。 */
    private static final int MAX_CHUNK_LENGTH = 6_000;
    private static final String SYSTEM_PROMPT = """
            你是中文语音识别文本的校对助手。请只校对用户提供的原始转写内容。

            规则：
            1. 仅修正明显的同音字、错别字、漏字，补充合理的中文标点与自然分段；
            2. 必须保留原意、事实、数字、专有名词和口语表达；
            3. 不得概括、扩写、删减、解释、评论或编造原文没有的信息；
            4. 对不能确定的识别结果保留原文，不要猜测；
            5. 只输出校对后的正文，不要标题、前缀、说明或 Markdown 代码围栏。
            """;

    private final ChatClient chatClient;

    public DeepseekTranscriptCorrectionService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @Override
    public String correct(String transcript) {
        if (!StringUtils.hasText(transcript)) {
            throw new ApiException(502, "本地语音识别未返回可校对的文本");
        }

        List<String> correctedChunks = new ArrayList<>();
        for (String chunk : splitIntoChunks(transcript.trim())) {
            try {
                String corrected = chatClient.prompt()
                        .system(SYSTEM_PROMPT)
                        .user(chunk)
                        .call()
                        .content();
                if (!StringUtils.hasText(corrected)) {
                    throw new ApiException(502, "DeepSeek 未返回校对结果");
                }
                correctedChunks.add(corrected.trim());
            } catch (ApiException e) {
                throw e;
            } catch (Exception e) {
                throw new ApiException(502, "DeepSeek 文本校对失败，请检查 DEEPSEEK_API_KEY 后重试");
            }
        }
        return String.join("\n\n", correctedChunks);
    }

    private static List<String> splitIntoChunks(String text) {
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + MAX_CHUNK_LENGTH, text.length());
            if (end < text.length()) {
                int boundary = findBoundary(text, start, end);
                if (boundary > start) {
                    end = boundary;
                }
            }
            chunks.add(text.substring(start, end).trim());
            start = end;
            while (start < text.length() && Character.isWhitespace(text.charAt(start))) {
                start++;
            }
        }
        return chunks;
    }

    private static int findBoundary(String text, int start, int end) {
        for (int i = end - 1; i > start; i--) {
            char c = text.charAt(i);
            if (c == '\n' || c == '。' || c == '！' || c == '？' || c == '；') {
                return i + 1;
            }
        }
        return end;
    }
}
