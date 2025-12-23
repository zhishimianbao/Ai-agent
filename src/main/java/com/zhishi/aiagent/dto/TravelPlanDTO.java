package com.zhishi.aiagent.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
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

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;
}