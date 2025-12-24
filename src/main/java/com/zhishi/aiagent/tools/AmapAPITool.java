package com.zhishi.aiagent.tools;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 高德地图API工具类，提供旅行规划相关功能
 */
public class AmapAPITool {

    // 高德地图API基础URL
    private static final String AMAP_BASE_URL = "https://restapi.amap.com/v3";
    // 地理编码API
    private static final String GEOCODE_URL = AMAP_BASE_URL + "/geocode/geo";
    // 逆地理编码API
    private static final String REVERSE_GEOCODE_URL = AMAP_BASE_URL + "/geocode/regeo";
    // 驾车路径规划API
    private static final String DRIVING_DIRECTION_URL = AMAP_BASE_URL + "/direction/driving";
    // 步行路径规划API
    private static final String WALKING_DIRECTION_URL = AMAP_BASE_URL + "/direction/walking";
    // 兴趣点搜索API
    private static final String PLACE_SEARCH_URL = AMAP_BASE_URL + "/place/text";

    private final String apiKey;

    public AmapAPITool(String apiKey) {
        this.apiKey = apiKey;
    }

    /**
     * 地理编码：将地址转换为经纬度坐标
     * @param address 地址信息
     * @param city 城市名称（可选，用于提高解析精度）
     * @return 经纬度坐标信息
     */
    @Tool(description = "将地址转换为经纬度坐标")
    public String geocode(
            @ToolParam(description = "详细地址") String address,
            @ToolParam(description = "城市名称（可选）") String city) {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("address", address);
        if (city != null && !city.isEmpty()) {
            paramMap.put("city", city);
        }
        paramMap.put("key", apiKey);
        
        try {
            String response = HttpUtil.get(GEOCODE_URL, paramMap);
            JSONObject jsonObject = JSONUtil.parseObj(response);
            
            if ("1".equals(jsonObject.getStr("status"))) {
                JSONArray geocodes = jsonObject.getJSONArray("geocodes");
                if (geocodes != null && geocodes.size() > 0) {
                    JSONObject geocode = geocodes.getJSONObject(0);
                    return geocode.toString();
                }
            }
            return "地理编码失败：" + jsonObject.getStr("info", "未知错误");
        } catch (Exception e) {
            return "地理编码异常：" + e.getMessage();
        }
    }

    /**
     * 逆地理编码：将经纬度坐标转换为地址信息
     * @param location 经纬度坐标，格式：经度,纬度
     * @param radius 搜索半径（可选，单位：米，默认1000）
     * @return 地址信息
     */
    @Tool(description = "将经纬度坐标转换为地址信息")
    public String reverseGeocode(
            @ToolParam(description = "经纬度坐标，格式：经度,纬度") String location,
            @ToolParam(description = "搜索半径（可选，单位：米）") Integer radius) {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("location", location);
        if (radius != null) {
            paramMap.put("radius", radius);
        }
        paramMap.put("key", apiKey);
        paramMap.put("extensions", "all"); // 返回详细信息
        
        try {
            String response = HttpUtil.get(REVERSE_GEOCODE_URL, paramMap);
            JSONObject jsonObject = JSONUtil.parseObj(response);
            
            if ("1".equals(jsonObject.getStr("status"))) {
                JSONObject regeocode = jsonObject.getJSONObject("regeocode");
                if (regeocode != null) {
                    return regeocode.toString();
                }
            }
            return "逆地理编码失败：" + jsonObject.getStr("info", "未知错误");
        } catch (Exception e) {
            return "逆地理编码异常：" + e.getMessage();
        }
    }

    /**
     * 驾车路径规划
     * @param origin 起点坐标，格式：经度,纬度
     * @param destination 终点坐标，格式：经度,纬度
     * @param waypoints 途经点（可选，多个途经点用|分隔）
     * @return 路径规划信息
     */
    @Tool(description = "驾车路径规划")
    public String drivingDirection(
            @ToolParam(description = "起点坐标，格式：经度,纬度") String origin,
            @ToolParam(description = "终点坐标，格式：经度,纬度") String destination,
            @ToolParam(description = "途经点（可选，多个途经点用|分隔）") String waypoints) {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("origin", origin);
        paramMap.put("destination", destination);
        if (waypoints != null && !waypoints.isEmpty()) {
            paramMap.put("waypoints", waypoints);
        }
        paramMap.put("key", apiKey);
        paramMap.put("extensions", "all"); // 返回详细信息
        
        try {
            String response = HttpUtil.get(DRIVING_DIRECTION_URL, paramMap);
            JSONObject jsonObject = JSONUtil.parseObj(response);
            
            if ("1".equals(jsonObject.getStr("status"))) {
                JSONObject route = jsonObject.getJSONObject("route");
                if (route != null) {
                    JSONArray paths = route.getJSONArray("paths");
                    if (paths != null && paths.size() > 0) {
                        // 返回前2条路径信息
                        List<Object> topPaths = paths.subList(0, Math.min(2, paths.size()));
                        return topPaths.stream()
                                .map(obj -> ((JSONObject) obj).toString())
                                .collect(Collectors.joining(","));
                    }
                }
            }
            return "路径规划失败：" + jsonObject.getStr("info", "未知错误");
        } catch (Exception e) {
            return "路径规划异常：" + e.getMessage();
        }
    }

    /**
     * 兴趣点搜索
     * @param keywords 搜索关键词
     * @param city 城市名称
     * @param type 兴趣点类型（可选，如：050000表示景点）
     * @param offset 返回结果数量（可选，默认10）
     * @return 兴趣点搜索结果
     */
    @Tool(description = "兴趣点搜索")
    public String placeSearch(
            @ToolParam(description = "搜索关键词") String keywords,
            @ToolParam(description = "城市名称") String city,
            @ToolParam(description = "兴趣点类型（可选）") String type,
            @ToolParam(description = "返回结果数量（可选）") Integer offset) {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("keywords", keywords);
        paramMap.put("city", city);
        if (type != null && !type.isEmpty()) {
            paramMap.put("type", type);
        }
        paramMap.put("offset", offset != null ? offset : 10);
        paramMap.put("key", apiKey);
        
        try {
            String response = HttpUtil.get(PLACE_SEARCH_URL, paramMap);
            JSONObject jsonObject = JSONUtil.parseObj(response);
            
            if ("1".equals(jsonObject.getStr("status"))) {
                JSONArray pois = jsonObject.getJSONArray("pois");
                if (pois != null && pois.size() > 0) {
                    return pois.stream()
                            .map(obj -> obj.toString())
                            .collect(Collectors.joining(","));
                }
            }
            return "兴趣点搜索失败：" + jsonObject.getStr("info", "未知错误");
        } catch (Exception e) {
            return "兴趣点搜索异常：" + e.getMessage();
        }
    }

    /**
     * 步行路径规划
     * @param origin 起点坐标，格式：经度,纬度
     * @param destination 终点坐标，格式：经度,纬度
     * @return 路径规划信息
     */
    @Tool(description = "步行路径规划")
    public String walkingDirection(
            @ToolParam(description = "起点坐标，格式：经度,纬度") String origin,
            @ToolParam(description = "终点坐标，格式：经度,纬度") String destination) {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("origin", origin);
        paramMap.put("destination", destination);
        paramMap.put("key", apiKey);
        paramMap.put("extensions", "all"); // 返回详细信息
        
        try {
            String response = HttpUtil.get(WALKING_DIRECTION_URL, paramMap);
            JSONObject jsonObject = JSONUtil.parseObj(response);
            
            if ("1".equals(jsonObject.getStr("status"))) {
                JSONObject route = jsonObject.getJSONObject("route");
                if (route != null) {
                    JSONArray paths = route.getJSONArray("paths");
                    if (paths != null && paths.size() > 0) {
                        // 返回前2条路径信息
                        List<Object> topPaths = paths.subList(0, Math.min(2, paths.size()));
                        return topPaths.stream()
                                .map(obj -> ((JSONObject) obj).toString())
                                .collect(Collectors.joining(","));
                    }
                }
            }
            return "步行路径规划失败：" + jsonObject.getStr("info", "未知错误");
        } catch (Exception e) {
            return "步行路径规划异常：" + e.getMessage();
        }
    }
}