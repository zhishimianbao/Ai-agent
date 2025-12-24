package com.zhishi.aiagent.app;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.zhishi.aiagent.advisor.MyLoggerAdvisor;
import com.zhishi.aiagent.tools.FileOperationTool;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.util.Map;

//@Transactional
@Component
@Slf4j
public class TripMindMCP {

    private final ChatClient chatClient;

    private final PromptTemplate promptTemplate;
    
    private final ResourceLoader resourceLoader;

    @Resource
    private ToolCallback[] allTools;

    // 全局 token 使用统计变量
    private int totalPromptTokens = 0;
    private int totalCompletionTokens = 0;

    // 新增 ResourceLoader 用于加载模板文件
    public TripMindMCP(ChatModel dashscopeChatModel, ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;

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
    public String generateTravelPlanWithMCP(String chatId, String destination, String travelDates, String interests,String budget) {
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
//                .options(DashScopeChatOptions.builder().withEnableSearch(true).build())
                    .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                    .advisors(new MyLoggerAdvisor())
                    .toolCallbacks(allTools)
                    .call()
                    .chatResponse();

        String content = chatResponse.getResult().getOutput().getText();

        log.info("Generated travel plan: {}", content);
        // 提取元数据
        Usage usage = chatResponse.getMetadata().getUsage();
        Integer inputTokens = usage.getPromptTokens();
        Integer outputTokens = usage.getCompletionTokens();
        String modelName = chatResponse.getMetadata().getModel();

        log.info("Tokens used - input: {}, output: {}", inputTokens, outputTokens);
        
        // 更新全局token统计
        updateTotalUsage(inputTokens, outputTokens);

        return content;
    }

    /**
     * 将AI生成的旅行规划转换为HTML格式
     * @param travelPlan AI生成的旅行规划内容
     * @param destination 目的地
     * @return AI生成的HTML内容
     */
    public String generateTravelHtml(String travelPlan, String destination) {
        try {
            // 加载HTML模板提示词
            PromptTemplate htmlPromptTemplate = new PromptTemplate(
                    resourceLoader.getResource("classpath:templates/TravelHtmlPrompt.st")
            );

            // 构建提示词内容
            Map<String, Object> htmlPromptParams = Map.of(
                    "travelPlan", travelPlan,
                    "destination", destination
            );
            String renderedHtmlPrompt = htmlPromptTemplate.render(htmlPromptParams);

            log.info("Rendered HTML prompt: {}", renderedHtmlPrompt);

            // 调用AI生成HTML
            ChatResponse htmlResponse = chatClient
                    .prompt()
                    .user(renderedHtmlPrompt)
                    .toolCallbacks(allTools) // 添加工具回调，允许AI调用工具
                    .call()
                    .chatResponse();

            String htmlContent = htmlResponse.getResult().getOutput().getText();
            log.info("Generated HTML content: {}", htmlContent);

            // 提取并统计token数
            Usage usage = htmlResponse.getMetadata().getUsage();
            Integer inputTokens = usage.getPromptTokens();
            Integer outputTokens = usage.getCompletionTokens();
            
            log.info("HTML generation tokens used - input: {}, output: {}", inputTokens, outputTokens);
            
            // 更新全局统计
            updateTotalUsage(inputTokens, outputTokens);

            return htmlContent;
        } catch (Exception e) {
            log.error("Error generating HTML travel plan: {}", e.getMessage());
            return "Error generating HTML travel plan: " + e.getMessage();
        }
    }


    /**
     * 重置全局token统计
     */
    private void resetTotalUsage() {
        this.totalPromptTokens = 0;
        this.totalCompletionTokens = 0;
    }

    /**
     * 更新全局token统计
     * @param promptTokens 输入token数
     * @param completionTokens 输出token数
     */
    private void updateTotalUsage(Integer promptTokens, Integer completionTokens) {
        this.totalPromptTokens += promptTokens;
        this.totalCompletionTokens += completionTokens;
    }
    
    /**
     * 生成旅游规划并生成HTML内容
     * @param chatId 对话ID
     * @param destination 目的地
     * @param travelDates 出行时间
     * @param interests 兴趣偏好
     * @param budget 预算
     * @return 包含旅游规划文本和HTML内容的结果
     */
    public Map<String, String> generateTravelPlanWithHtml(String chatId, String destination, String travelDates, String interests, String budget) {
        // 1. 重置全局token统计
        resetTotalUsage();
        
        // 2. 调用原方法生成旅游规划（不改变提示词）
        String travelPlan = generateTravelPlanWithMCP(chatId, destination, travelDates, interests, budget);
        
        // 3. 根据TravelHtmlPrompt.st生成HTML内容
        String htmlContent = generateTravelHtml(travelPlan, destination);
        
        // 4. 手动调用FileOperationTool保存HTML文件
        // 生成时间戳
        String time = String.valueOf(System.currentTimeMillis());
        FileOperationTool fileOperationTool = new FileOperationTool();
        String fileName = "travel_plan_" + destination.replaceAll("\\s+", "_") + "_" + time + ".html";
        String filePath = fileOperationTool.writeFile(fileName, htmlContent);
        log.info("手动保存HTML文件成功: {}", filePath);
        
        // 5. 统计本次两次调用的总token数
        int totalTokens = totalPromptTokens + totalCompletionTokens;
        log.info("Total tokens used in generateTravelPlanWithHtml - prompt: {}, completion: {}, total: {}", 
                totalPromptTokens, totalCompletionTokens, totalTokens);
        
        // 6. 返回结果
        return Map.of(
                "travelPlan", travelPlan,
                "htmlContent", htmlContent
//                "filePath", filePath
        );
    }



}