package com.zhishi.aiagent.controller;

import com.zhishi.aiagent.app.TripMind;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TripMindController {

    private final TripMind tripMind;

    public TripMindController(TripMind tripMind) {
        this.tripMind = tripMind;
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


}
