// com.zhishi.aiagent.dto.PlanRequest.java
package com.zhishi.aiagent.entity;

import lombok.Data;

@Data
public class PlanRequest {
    private String destination;   // 目的地，如 "日本京都"
    private String travelDates;   // 出行时间，如 "2025年10月1日-10月5日"
    private String interests;     // 兴趣偏好，如 "历史文化,美食,摄影"
}