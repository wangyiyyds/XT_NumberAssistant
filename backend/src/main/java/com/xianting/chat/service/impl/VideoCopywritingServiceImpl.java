package com.xianting.chat.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xianting.chat.common.ApiException;
import com.xianting.chat.service.VideoCopywritingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.URI;
import java.util.Map;

/** 将视频链接发送给已配置的转写服务；应用自身不会下载或保存视频文件。 */
@Service
public class VideoCopywritingServiceImpl implements VideoCopywritingService {

    private final String endpoint;
    private final String apiKey;
    private final ObjectMapper objectMapper;

    public VideoCopywritingServiceImpl(
            @Value("${app.video-transcription.url:}") String endpoint,
            @Value("${app.video-transcription.api-key:}") String apiKey,
            ObjectMapper objectMapper) {
        this.endpoint = endpoint;
        this.apiKey = apiKey;
        this.objectMapper = objectMapper;
    }

    @Override
    public String extract(String videoUrl) {
        validateVideoUrl(videoUrl);
        if (!StringUtils.hasText(endpoint)) {
            throw new ApiException(503, "未配置视频转写服务，请设置 VIDEO_TRANSCRIPTION_URL 后重试");
        }
        try {
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(10_000);
            factory.setReadTimeout(840_000);
            RestClient.RequestBodySpec request = RestClient.builder().requestFactory(factory).build().post().uri(endpoint)
                    .contentType(MediaType.APPLICATION_JSON);
            if (StringUtils.hasText(apiKey)) {
                request.header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
            }
            String body = request.body(Map.of("url", videoUrl)).retrieve().body(String.class);
            String text = readText(body);
            if (!StringUtils.hasText(text)) throw new ApiException(502, "转写服务未返回文案内容");
            return text.trim();
        } catch (ApiException e) {
            throw e;
        } catch (RestClientException e) {
            throw new ApiException(502, "视频转写服务请求失败，请稍后重试");
        }
    }

    private static void validateVideoUrl(String value) {
        try {
            URI uri = URI.create(value);
            if ((!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme()))
                    || !StringUtils.hasText(uri.getHost())) {
                throw new ApiException(400, "请输入有效的 http 或 https 视频链接");
            }
        } catch (IllegalArgumentException e) {
            throw new ApiException(400, "请输入有效的视频链接");
        }
    }

    private String readText(String body) {
        if (!StringUtils.hasText(body)) return null;
        try {
            JsonNode root = objectMapper.readTree(body);
            for (String key : new String[]{"text", "transcript", "copywriting", "content"}) {
                JsonNode value = root.path(key);
                if (value.isTextual()) return value.asText();
                value = root.path("data").path(key);
                if (value.isTextual()) return value.asText();
            }
            return null;
        } catch (Exception e) {
            throw new ApiException(502, "转写服务返回的数据格式不正确");
        }
    }
}
