package com.zhishi.aiagent.controller;

import com.zhishi.aiagent.app.TripMind;
import com.zhishi.aiagent.app.TripMindWithMCPandTools;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;


@RestController
public class TripMindController {

    private final TripMind tripMind;
    private final TripMindWithMCPandTools tripMindWithMCPandTools;

    public TripMindController(TripMind tripMind, TripMindWithMCPandTools tripMindWithMCPandTools) {
        this.tripMind = tripMind;
        this.tripMindWithMCPandTools = tripMindWithMCPandTools;
    }

    @GetMapping("/tripmind/plan")
    public String TravelPlan(
            @RequestParam String chatId,
            @RequestParam String destination,
            @RequestParam String travelDates,
            @RequestParam String interests,
            @RequestParam String budget,
            @RequestParam(required = false, defaultValue = "") String time) {

        // time参数用于HTML生成时的文件名，如果为空则使用当前时间戳
        // 这里只是接收参数，实际使用在HTML生成时
        return tripMind.generateTravelPlan(chatId, destination, travelDates, interests, budget);
    }

    // 方式1：返回Flux响应式对象，并且添加SSE对应的MediaType，produces = MediaType.TEXT_EVENT_STREAM_VALUE，浏览器解析为SSE
    //不添加produces = MediaType.TEXT_EVENT_STREAM_VALUE，解析为纯文本流
    @GetMapping(value = "/tripmind/plan/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> generateTravelPlanSSE(
            @RequestParam String chatId,
            @RequestParam String destination,
            @RequestParam String travelDates,
            @RequestParam String interests,
            @RequestParam String budget) {
        return tripMindWithMCPandTools.generateTravelPlanWithMCPFlux(chatId, destination, travelDates, interests, budget);
    }

    // 方式2：返回Flux对象，并且设置泛型为ServerSentEvent,无需设置produces = MediaType.TEXT_EVENT_STREAM_VALUE
    @GetMapping(value = "/tripmind/plan/sse/event")
    public Flux<ServerSentEvent<String>> generateTravelPlanSSEEvent(
            @RequestParam String chatId,
            @RequestParam String destination,
            @RequestParam String travelDates,
            @RequestParam String interests,
            @RequestParam String budget) {
        return tripMindWithMCPandTools.generateTravelPlanWithMCPFlux(chatId, destination, travelDates, interests, budget)
                .map(chunk -> ServerSentEvent.<String>builder()
                        .data(chunk)
                        .build());
    }

    // 方式3：使用SSEEmitter，通过send方法持续向SseEmitter发送消息
    @GetMapping("/tripmind/plan/sse/emitter")
    public SseEmitter generateTravelPlanSseEmitter(
            @RequestParam String chatId,
            @RequestParam String destination,
            @RequestParam String travelDates,
            @RequestParam String interests,
            @RequestParam String budget) {
        return tripMindWithMCPandTools.generateTravelPlanWithMCPStreamSSE(chatId, destination, travelDates, interests, budget);
    }
}
