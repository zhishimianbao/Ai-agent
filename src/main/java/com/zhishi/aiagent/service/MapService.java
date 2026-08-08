package com.zhishi.aiagent.service;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 地图服务类，用于解析旅游规划HTML并提取地点信息
 */
@Service
@Slf4j
public class MapService {

    @Value("${map.api-key}")
    private String amapApiKey;

    private static final String GEOCODE_URL = "https://restapi.amap.com/v3/geocode/geo";

    /**
     * 地点信息类
     */
    public static class LocationInfo {
        public String name;           // 地点名称
        public String type;           // 类型：景点、餐厅、酒店等
        public String time;           // 时间信息
        public String description;    // 描述
        public String lng;            // 经度
        public String lat;            // 纬度
        public String address;        // 地址

        public LocationInfo(String name, String type, String time, String description) {
            this.name = name;
            this.type = type;
            this.time = time;
            this.description = description;
        }
    }

    /**
     * 从HTML中提取地点信息
     * @param htmlContent HTML内容
     * @return 地点信息列表
     */
    public List<LocationInfo> extractLocationsFromHtml(String htmlContent) {
        List<LocationInfo> locations = new ArrayList<>();

        try {
            Document doc = Jsoup.parse(htmlContent);

            // 查找所有活动项
            Elements activities = doc.select(".activity");

            for (Element activity : activities) {
                // 提取时间
                Element timeElement = activity.selectFirst(".activity-time");
                String time = timeElement != null ? timeElement.text() : "";

                // 提取标题（地点名称）
                Element titleElement = activity.selectFirst(".activity-title");
                if (titleElement != null) {
                    // 移除图标，获取纯文本
                    String title = titleElement.text().trim();
                    if (title.startsWith("fas")) {
                        // 如果包含图标类名，尝试获取下一个文本节点
                        title = titleElement.ownText().trim();
                    }

                    // 提取描述
                    Element descElement = activity.selectFirst(".activity-details");
                    String description = descElement != null ? descElement.text() : "";

                    // 判断类型
                    String type = determineType(title, description);

                    if (!title.isEmpty()) {
                        LocationInfo location = new LocationInfo(title, type, time, description);
                        locations.add(location);
                    }
                }
            }

            log.info("从HTML中提取到 {} 个地点", locations.size());

        } catch (Exception e) {
            log.error("解析HTML失败", e);
        }

        return locations;
    }

    /**
     * 根据标题和描述判断地点类型
     */
    private String determineType(String title, String description) {
        String text = (title + " " + description).toLowerCase();

        if (text.contains("餐厅") || text.contains("用餐") || text.contains("美食") ||
            text.contains("饭店") || text.contains("餐馆") || text.contains("食堂")) {
            return "restaurant";
        } else if (text.contains("酒店") || text.contains("住宿") || text.contains("民宿") ||
                   text.contains("宾馆") || text.contains("客栈")) {
            return "hotel";
        } else if (text.contains("景点") || text.contains("景区") || text.contains("公园") ||
                   text.contains("博物馆") || text.contains("寺") || text.contains("塔") ||
                   text.contains("园") || text.contains("馆") || text.contains("巷") ||
                   text.contains("乐园")) {
            return "attraction";
        } else {
            return "other";
        }
    }

    /**
     * 批量获取地点的经纬度坐标
     * @param locations 地点列表
     * @param city 城市名称
     */
    public void geocodeLocations(List<LocationInfo> locations, String city) {
        for (LocationInfo location : locations) {
            try {
                Map<String, Object> paramMap = new HashMap<>();
                // 使用城市+地点名称进行地理编码
                String address = city + location.name;
                paramMap.put("address", address);
                paramMap.put("key", amapApiKey);

                String response = HttpUtil.get(GEOCODE_URL, paramMap);
                JSONObject jsonObject = JSONUtil.parseObj(response);

                if ("1".equals(jsonObject.getStr("status"))) {
                    JSONArray geocodes = jsonObject.getJSONArray("geocodes");
                    if (geocodes != null && geocodes.size() > 0) {
                        JSONObject geocode = geocodes.getJSONObject(0);
                        String locationStr = geocode.getStr("location");
                        if (locationStr != null && locationStr.contains(",")) {
                            String[] coords = locationStr.split(",");
                            location.lng = coords[0];
                            location.lat = coords[1];
                            location.address = geocode.getStr("formatted_address", "");
                            log.info("地点 {} 坐标: {}, {}", location.name, location.lng, location.lat);
                        }
                    }
                } else {
                    log.warn("地理编码失败: {} - {}", location.name, jsonObject.getStr("info"));
                }

                // 避免请求过快，稍微延迟
                Thread.sleep(100);

            } catch (Exception e) {
                log.error("获取地点 {} 坐标失败", location.name, e);
            }
        }
    }

    /**
     * 从HTML中提取目的地城市名称
     */
    public String extractDestinationFromHtml(String htmlContent) {
        try {
            Document doc = Jsoup.parse(htmlContent);

            // 尝试从标题中提取
            Element titleElement = doc.selectFirst("h1");
            if (titleElement != null) {
                String title = titleElement.text();
                // 提取城市名称（通常在标题开头）
                if (title.contains("旅游规划") || title.contains("攻略") || title.contains("之旅")) {
                    String[] parts = title.split("旅游规划|攻略|之旅");
                    if (parts.length > 0) {
                        String city = parts[0].trim();
                        // 移除可能的图标字符
                        city = city.replaceAll("[^\\u4e00-\\u9fa5]", "").trim();
                        if (!city.isEmpty()) {
                            return city;
                        }
                    }
                }
            }

            // 尝试从subtitle中提取（可扩展）
            // Element subtitleElement = doc.selectFirst(".subtitle");
            // if (subtitleElement != null) {
            //     String subtitle = subtitleElement.text();
            //     // 可以进一步解析
            // }

        } catch (Exception e) {
            log.error("提取目的地失败", e);
        }

        return "";
    }
}
