package com.osschoolwork.backend.client;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 调用 Python 垃圾邮件识别服务（Flask API）
 */
@Component
public class SpamDetectionClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpamDetectionClient.class);

    private final RestTemplate restTemplate;
    private final String spamApiUrl;

    public SpamDetectionClient(@Value("${spam.api.url:http://localhost:5050}") String spamApiUrl) {
        this.restTemplate = new RestTemplate();
        this.spamApiUrl = spamApiUrl;
    }

    /**
     * 检测邮件内容是否为垃圾邮件
     * @return true 表示垃圾邮件
     */
    public boolean isSpam(String subject, String content) {
        try {
            String url = spamApiUrl + "/check";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            ObjectMapper mapper = new ObjectMapper();
            Map<String, String> body = Map.of(
                    "subject", subject != null ? subject : "",
                    "content", content != null ? content : ""
            );
            String json = mapper.writeValueAsString(body);

            HttpEntity<String> request = new HttpEntity<>(json, headers);
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);

            if (response != null && Boolean.TRUE.equals(response.get("isSpam"))) {
                LOGGER.info("垃圾邮件检测: 命中, subject={}", subject);
                return true;
            }
        } catch (Exception e) {
            LOGGER.warn("调用 Python 垃圾邮件检测服务失败，降级为非垃圾邮件: {}", e.getMessage());
        }
        return false;
    }
}
