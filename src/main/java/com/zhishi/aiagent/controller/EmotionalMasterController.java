package com.zhishi.aiagent.controller;

import com.zhishi.aiagent.agent.MyManus;
import com.zhishi.aiagent.app.EmotionalMaster;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;

@RestController
@RequestMapping("/ai")
public class EmotionalMasterController {

    @Resource
    private EmotionalMaster emotionalMaster;

    @Resource
    private ToolCallback[] allTools;

    @Resource
    private ChatModel dashscopeChatModel;

    /**
     * 同步调用 AI 情感大师应用
     *
     * @param message
     * @param chatId
     * @return
     */
    @GetMapping("/master/chat/sync")
    public String doChatWithMasterSync(String message, String chatId) {
        return emotionalMaster.chatWithMaster(message, chatId);
    }

    /**
     * SSE 流式调用 AI 情感大师应用
     *
     * @param message
     * @param chatId
     * @return
     */
    @GetMapping(value = "/master/chat/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> doChatWithMasterSSE(String message, String chatId) {
        return emotionalMaster.chatWithMasterByStream(message, chatId);
    }

    /**
     * SSE 流式调用 AI 情感大师应用
     *
     * @param message
     * @param chatId
     * @return
     */
    @GetMapping(value = "/master/chat/server_sent_event")
    public Flux<ServerSentEvent<String>> doChatWithMasterServerSentEvent(String message, String chatId) {
        return emotionalMaster.chatWithMasterByStream(message, chatId)
                .map(chunk -> ServerSentEvent.<String>builder()
                        .data(chunk)
                        .build());
    }

    /**
     * SSE 流式调用 AI 情感大师应用
     *
     * @param message
     * @param chatId
     * @return
     */
    @GetMapping(value = "/master/chat/sse_emitter")
    public SseEmitter doChatWithMasterServerSseEmitter(String message, String chatId) {
        // 创建一个超时时间较长的 SseEmitter
        SseEmitter sseEmitter = new SseEmitter(180000L); // 3 分钟超时
        // 获取 Flux 响应式数据流并且直接通过订阅推送给 SseEmitter
        emotionalMaster.chatWithMasterByStream(message, chatId)
                .subscribe(chunk -> {
                    try {
                        sseEmitter.send(chunk);
                    } catch (IOException e) {
                        sseEmitter.completeWithError(e);
                    }
                }, sseEmitter::completeWithError, sseEmitter::complete);
        // 返回
        return sseEmitter;
    }
}
