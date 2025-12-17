package com.zhishi.aiagent.app;

import com.zhishi.aiagent.advisor.MyLoggerAdvisor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
public class TripMind {

    private final ChatClient chatClient;
    private final PromptTemplate promptTemplate;

    // 新增 ResourceLoader 用于加载模板文件
    public TripMind(ChatModel dashscopeChatModel, ResourceLoader resourceLoader) {
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
     * @param message
     * @param chatId
     * @return
     */
    public String doChat(String message, String chatId) {
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
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
    public String generateTravelPlan(String chatId, String destination, String travelDates, String interests) {
        // 渲染模板
        String renderedPrompt = promptTemplate.render(Map.of(
                "destination", destination,
                "travelDates", travelDates,
                "interests", interests
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
        return content;
    }


}