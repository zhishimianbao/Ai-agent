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

    @GetMapping("/plan")
    public String TravelPlan(
            @RequestParam String chatId,
            @RequestParam String destination,
            @RequestParam String travelDates,
            @RequestParam String interests,
            @RequestParam String budget) {

        return tripMind.generateTravelPlan(chatId, destination, travelDates, interests, budget);
    }

    @GetMapping("/test")
    public String test() {
        return tripMind.doChat("123");
    }
}
