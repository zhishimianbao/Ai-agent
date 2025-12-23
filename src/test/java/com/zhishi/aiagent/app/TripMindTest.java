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

    @Test
    void generateTravelPlan() {
        String chatId = UUID.randomUUID().toString();
        String answer =tripMind.generateTravelPlan(chatId,"徐州","2025.12.20","美食","无");
        Assertions.assertNotNull(answer);
    }
}