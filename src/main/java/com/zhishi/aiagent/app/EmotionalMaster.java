package com.zhishi.aiagent.app;

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
import reactor.core.publisher.Flux;

@Component
@Slf4j
public class EmotionalMaster {

        private final ChatClient chatClient;
        private final PromptTemplate promptTemplate;

        // 新增 ResourceLoader 用于加载模板文件
        public EmotionalMaster(ChatModel dashscopeChatModel, ResourceLoader resourceLoader) {


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
                    resourceLoader.getResource("classpath:templates/EmotionalMasterPrompt.st")
            );
        }

    /**
     * AI 基础对话（支持多轮对话记忆）
     *
     * @param message
     * @param chatId
     * @return
     */
    public String chatWithMaster(String message, String chatId) {

        String render = promptTemplate.render();

        ChatResponse chatResponse = chatClient
                .prompt()
                .system(render)
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }


    /**
     * AI 基础对话（支持多轮对话记忆，SSE 流式传输）
     *
     * @param message
     * @param chatId
     * @return
     */
    public Flux<String> chatWithMasterByStream(String message, String chatId) {
        String render = promptTemplate.render();
        return chatClient
                .prompt()
                .system(render)
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .stream()
                .content();
    }



}
