package com.zhishi.aiagent.app;

import com.zhishi.aiagent.test.Testchat;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class EmotionalMasterTest {

    @Resource
    private EmotionalMaster emotionalMaster;

    @Test
    void chatWithMaster() {
        String chatId = UUID.randomUUID().toString();
        String message = "你好，我是程序员面包，我想知道如何让另一半（编程）更爱我";
        String answer = emotionalMaster.chatWithMaster(message,chatId);
        Assertions.assertNotNull(answer);
    }

    @Test
    void chatWithMasterByStream() {
        String chatId = UUID.randomUUID().toString();
        String message = "你好，我是程序员面包，我想知道如何让另一半（编程）更爱我";
        String answer = emotionalMaster.chatWithMaster(message,chatId);
        Assertions.assertNotNull(answer);
        message = "一起探索可能的靠近方式";
        String answer1 = emotionalMaster.chatWithMaster(message,chatId);
        Assertions.assertNotNull(answer1);
    }
}