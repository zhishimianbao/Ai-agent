package com.zhishi.aiagent.test;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;


@SpringBootTest
class testTest {

    @Resource
    private Testchat testchat;

    @Test
    void testNChat() {
        String chatId = UUID.randomUUID().toString();
        // 第一轮
        String message = "你好，我是程序员面包";

        // 第二轮
        message = "我想让另一半（编程）更爱我";
        String answer = testchat.doChat(chatId);
        Assertions.assertNotNull(answer);
        // 第三轮
        message = "我的另一半叫什么来着？刚跟你说过，帮我回忆一下";
        answer = testchat.doChat(chatId);
        Assertions.assertNotNull(answer);

        String chatId1 = UUID.randomUUID().toString();
        message = "我的另一半叫什么来着？刚跟你说过，帮我回忆一下";
        answer = testchat.doChat(chatId1);
        Assertions.assertNotNull(answer);
    }

    @Test
    void testChat() {
        String chatId = UUID.randomUUID().toString();
        String answer = testchat.doChat(chatId);
        Assertions.assertNotNull(answer);
    }

}