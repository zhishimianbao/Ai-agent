package com.zhishi.aiagent.app;

import com.zhishi.aiagent.advisor.MyLoggerAdvisor;
import com.zhishi.aiagent.dto.TravelPlanDTO;
import com.zhishi.aiagent.mapper.TravelPlanMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

//@Transactional
@Component
@Slf4j
public class TripMind {

    private final ChatClient chatClient;
    private final PromptTemplate promptTemplate;

    private TravelPlanMapper travelPlanMapper; // MyBatis Mapper

    // 新增 ResourceLoader 用于加载模板文件
    public TripMind(ChatModel dashscopeChatModel, ResourceLoader resourceLoader, TravelPlanMapper travelPlanMapper) {
        this.travelPlanMapper = travelPlanMapper;

        // 初始化对话记忆（虽然本次任务为单次生成，但保留以支持未来扩展）
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(20)
                .build();

        this.chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
//                        ,new MyLoggerAdvisor()
                )
                .build();

        // 加载提示词模板
        this.promptTemplate = new PromptTemplate(
                resourceLoader.getResource("classpath:templates/TripMindPrompt.st")
        );
    }

    /**
     * AI 基础对话（支持多轮对话记忆）
     *
     * @param chatId
     * @return
     */
    public String doChat(String chatId) {
        ChatResponse chatResponse = chatClient
                .prompt()
                .user("你好，你是谁")
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        // 提取元数据
        var usage = chatResponse.getMetadata().getUsage();
        Integer inputTokens = usage.getPromptTokens();
        Integer outputTokens = usage.getCompletionTokens();
        String modelName = chatResponse.getMetadata().getModel();

        log.info("Tokens used - input: {}, output: {}", inputTokens, outputTokens);

        // 构建 DTO
        TravelPlanDTO dto = new TravelPlanDTO();
        dto.setChatId(chatId);
        dto.setModelName(modelName);
        dto.setInputTokens(inputTokens);
        dto.setOutputTokens(outputTokens);
        dto.setTotalTokens(inputTokens + outputTokens);
        // 示例：qwen3-max 约 ¥0.00012 / token（按实际模型定价调整）
//        dto.setCostEstimate((dto.getTotalTokens() * 0.00012));
        dto.setCreatedTime(LocalDateTime.now());

        // 持久化到 MySQL
        travelPlanMapper.insertCost(dto);

        return content;
    }

    /**
     * 生成个性化旅行攻略（单次任务，不依赖多轮对话）
     *
     * @param destination   目的地（如“日本京都”）
     * @param travelDates   出行时间（如“2025年10月1日-10月5日”）
     * @param interests     兴趣偏好（如“历史文化、美食、摄影”）
     * @return 生成的完整旅游攻略
     */
    public String generateTravelPlan(String chatId, String destination, String travelDates, String interests,String budget) {
        // 渲染模板
        String renderedPrompt = promptTemplate.render(Map.of(
                "destination", destination,
                "travelDates", travelDates,
                "interests", interests,
                "budget", budget
        ));

        log.info("Rendered prompt: {}", renderedPrompt);

        // 调用模型（此处 user 消息即为完整提示）
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(renderedPrompt)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .chatResponse();

        String content = chatResponse.getResult().getOutput().getText();

        log.info("Generated travel plan: {}", content);
        // 提取元数据
        var usage = chatResponse.getMetadata().getUsage();
        Integer inputTokens = usage.getPromptTokens();
        Integer outputTokens = usage.getCompletionTokens();
        String modelName = chatResponse.getMetadata().getModel();

        log.info("Tokens used - input: {}, output: {}", inputTokens, outputTokens);

        // 构建 DTO
        TravelPlanDTO dto = new TravelPlanDTO();
        dto.setChatId(chatId);
        dto.setModelName(modelName);
        dto.setInputTokens(inputTokens);
        dto.setOutputTokens(outputTokens);
        dto.setTotalTokens(inputTokens + outputTokens);
        // 示例：qwen3-max 约 ¥0.00012 / token（按实际模型定价调整）
//        dto.setCostEstimate((dto.getTotalTokens() * 0.00012));
        dto.setCreatedTime(LocalDateTime.now());

        // 持久化到 MySQL
        travelPlanMapper.insertCost(dto);

        return content;
    }
}