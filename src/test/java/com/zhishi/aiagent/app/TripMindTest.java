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
    void testNChat() {
        String chatId = UUID.randomUUID().toString();
        // 第一轮
        String message = "你好，我是程序员面包";
        var answer = tripMind.doChat(chatId);
        // 第二轮
        message = "我想让另一半（编程导航）更爱我";
        answer = tripMind.doChat(chatId);
        Assertions.assertNotNull(answer);
        // 第三轮
        message = "我的另一半叫什么来着？刚跟你说过，帮我回忆一下";
        answer = tripMind.doChat(chatId);
        Assertions.assertNotNull(answer);

        String chatId1 = UUID.randomUUID().toString();
        message = "我的另一半叫什么来着？刚跟你说过，帮我回忆一下";
        answer = tripMind.doChat(chatId1);
        Assertions.assertNotNull(answer);
    }

    @Test
    void testChat() {
        String chatId = UUID.randomUUID().toString();
        String answer = tripMind.doChat(chatId);
        Assertions.assertNotNull(answer);
    }

    @Test
    void generateTravelPlan() {
        String chatId = UUID.randomUUID().toString();
        String answer =tripMind.generateTravelPlan(chatId,"徐州","2025.12.20","美食","无");
        Assertions.assertNotNull(answer);
    }
}