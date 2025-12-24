package com.zhishi.aiagent.app;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.zhishi.aiagent.advisor.MyLoggerAdvisor;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.util.Map;

//@Transactional
@Component
@Slf4j
public class TripMindTools {

    private final ChatClient chatClient;

    private final PromptTemplate promptTemplate;

    // AI 调用工具能力
    @Resource
    private ToolCallback[] allTools;

    // 新增 ResourceLoader 用于加载模板文件
    public TripMindTools(ChatModel dashscopeChatModel, ResourceLoader resourceLoader) {

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
                resourceLoader.getResource("classpath:templates/TripMindSimplePrompt.st")
        );
    }

    /**
     * 生成个性化旅行攻略
     *
     * @param destination   目的地（如“日本京都”）
     * @param travelDates   出行时间（如“2025年10月1日-10月5日”）
     * @param interests     兴趣偏好（如“历史文化、美食、摄影”）
     * @param budget        预算(如“500-1000￥”)
     * @return 生成的完整旅游攻略
     */
    public String generateTravelPlanWithTools(String chatId, String destination, String travelDates, String interests,String budget) {
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
                //开启联网搜索
                .options(DashScopeChatOptions.builder().withEnableSearch(true).build())
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .advisors(new MyLoggerAdvisor())
                .toolCallbacks(allTools)
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

        return content;
    }

// TODO 地图API Tool 报告生成

}