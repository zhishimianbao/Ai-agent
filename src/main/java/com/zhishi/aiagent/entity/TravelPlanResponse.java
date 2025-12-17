// com.zhishi.aiagent.dto.TravelPlanResponse.java
package com.zhishi.aiagent.entity;

import lombok.Data;

@Data
public class TravelPlanResponse {
    private String content;
    private TokenUsage tokenUsage;

    @Data
    public static class TokenUsage {
        private Integer promptTokens;
        private Integer completionTokens;
        private Integer totalTokens;
    }
}