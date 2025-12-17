package com.zhishi.aiagent.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TravelPlanDTO {
    private Long id;
    private String chatId;
//    private String destination;
//    private String travelDates;
//    private String interests;
//    private String generatedPlan;
    private String modelName;
    private Integer inputTokens;
    private Integer outputTokens;
    private Integer totalTokens;
//    private Double costEstimate;
    private LocalDateTime createdTime;
}