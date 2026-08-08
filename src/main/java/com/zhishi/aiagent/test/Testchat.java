package com.zhishi.aiagent.test;

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
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Slf4j
public class Testchat {
    private final ChatClient chatClient;
    private TravelPlanMapper travelPlanMapper; // MyBatis Mapper

    // 新增 ResourceLoader 用于加载模板文件
    public Testchat(ChatModel dashscopeChatModel , TravelPlanMapper travelPlanMapper) {
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
                //开启联网搜索
//                .options(DashScopeChatOptions.builder().withEnableSearch(true).build())
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
}
