package com.zhishi.aiagent.app;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class TripMindTest {

    @Resource
    private TripMind tripMind;

    @Resource
    private TripMindMCP tripMindMCP;

    @Resource
    private TripMindTools tripMindTools;

    @Test
    void generateTravelPlan() {
        String chatId = UUID.randomUUID().toString();
        String answer =tripMind.generateTravelPlan(chatId,"徐州","2025.12.20","美食","无");
        Assertions.assertNotNull(answer);
    }

    @Test
    void generateTravelPlanWithMCP() {
        String chatId = UUID.randomUUID().toString();
        String answer =tripMindMCP.generateTravelPlanWithMCP(chatId,"镇江","2025.12.27","美食","500-1000￥");
        Assertions.assertNotNull(answer);
    }

    @Test
    void generateTravelPlanWithTools() {
        String chatId = UUID.randomUUID().toString();
        String answer =tripMindTools.generateTravelPlanWithTools(chatId,"镇江","2025.12.27","美食","500-1000￥");
        Assertions.assertNotNull(answer);
    }
}