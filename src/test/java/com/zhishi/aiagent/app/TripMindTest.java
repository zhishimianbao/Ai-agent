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
    void generateTravelHtml() {
        // 准备测试数据
        String simpleTravelPlan = "# 北京三日游\n\n## 第一天\n- 上午：参观故宫博物院\n- 下午：游览天安门广场\n- 晚上：品尝北京烤鸭\n\n## 第二天\n- 上午：爬八达岭长城\n- 下午：参观颐和园\n\n## 第三天\n- 上午：游览天坛\n- 下午：购买纪念品";
        String destination = "北京";
        
        // 调用测试方法
        String htmlContent = tripMindMCP.generateTravelHtml(simpleTravelPlan, destination);
        
        // 验证结果
        Assertions.assertNotNull(htmlContent);
        Assertions.assertTrue(htmlContent.length() > 0);

        System.out.println("=== generateTravelHtml 测试结果 ===");
        System.out.println("HTML内容长度: " + htmlContent.length());
    }

    @Test
    void generateTravelPlanWithMCP() {
        String chatId = UUID.randomUUID().toString();
        String answer =tripMindMCP.generateTravelPlanWithMCP(chatId,"常州","2025.12.26","景点和美食","500-1000￥");
        Assertions.assertNotNull(answer);
    }

    @Test
    void generateTravelPlanWithTools() {
        String chatId = UUID.randomUUID().toString();
        String answer =tripMindTools.generateTravelPlanWithTools(chatId,"镇江","2025.12.27","美食","500-1000￥");
        Assertions.assertNotNull(answer);
    }

    @Test
    void testTravelPlanWithAutoHtml() {
        String chatId = UUID.randomUUID().toString();
        String destination = "常州";
        String travelDates = "2025.12.26";
        String interests = "景观、美食";
        String budget = "200-500￥";
        
        // 调用测试方法
        var result = tripMindMCP.generateTravelPlanWithHtml(chatId, destination, travelDates, interests, budget);
        
        // 验证结果
        Assertions.assertNotNull(result);

        // 打印测试结果
        System.out.println("=== 测试结果 ===");
        System.out.println("旅行规划内容存在: " + (result.get("travelPlan").length() > 0));
        System.out.println("HTML内容存在: " + (result.get("htmlContent").length() > 0));
        System.out.println("是否包含工具调用: " + result.get("containsToolCall"));
        System.out.println("是否有FileOperationTool: " + result.get("hasWriteFileTool"));

    }
}