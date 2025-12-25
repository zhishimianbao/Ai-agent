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
    private final String jsApiKey;
    private final String securityJsCode;

    public AmapAPITool(String apiKey) {
        this.apiKey = apiKey;
        this.jsApiKey = null;
        this.securityJsCode = null;
    }

    public AmapAPITool(String apiKey, String jsApiKey, String securityJsCode) {
        this.apiKey = apiKey;
        this.jsApiKey = jsApiKey;
        this.securityJsCode = securityJsCode;
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

    /**
     * 生成地图HTML片段，用于嵌入到旅游规划HTML中
     * @param locationsJson 地点信息JSON数组，格式：[{"name":"地点名","type":"attraction|restaurant|hotel","time":"时间","description":"描述","lng":"经度","lat":"纬度","address":"地址"},...]
     * @param destination 目的地城市名称
     * @return 地图HTML片段（包含地图容器、地点列表和样式）
     */
    @Tool(description = "生成地图HTML片段，用于嵌入到旅游规划HTML中。需要提供地点信息（包含名称、类型、时间、描述、经纬度坐标）和目的地城市名称")
    public String generateMapHtmlFragment(
            @ToolParam(description = "地点信息JSON数组，格式：[{\"name\":\"地点名\",\"type\":\"attraction|restaurant|hotel\",\"time\":\"时间\",\"description\":\"描述\",\"lng\":\"经度\",\"lat\":\"纬度\",\"address\":\"地址\"},...]") String locationsJson,
            @ToolParam(description = "目的地城市名称") String destination) {
        try {
            if (jsApiKey == null || securityJsCode == null) {
                return "地图功能未配置：缺少JS API密钥或安全密钥";
            }

            JSONArray locations = JSONUtil.parseArray(locationsJson);
            if (locations == null || locations.size() == 0) {
                return "地点信息为空，无法生成地图";
            }

            StringBuilder html = new StringBuilder();
            
            // 地图section开始
            html.append("<div id=\"travel-map-section\" class=\"section\">\n");
            html.append("    <h2 class=\"section-title\"><i class=\"fas fa-map-marked-alt\"></i> 旅游路线地图</h2>\n");
            
            // 地图容器
            html.append("    <div class=\"map-wrapper\" style=\"margin-top: 20px; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 15px rgba(0,0,0,0.1);\">\n");
            html.append("        <div id=\"travel-map\" style=\"width:100%;height:600px;\"></div>\n");
            html.append("    </div>\n");
            
            // 地点列表容器
            html.append("    <div id=\"location-list\" class=\"location-list-embedded\" style=\"margin-top: 20px; display: grid; grid-template-columns: repeat(auto-fill, minmax(250px, 1fr)); gap: 15px;\">\n");
            
            // 添加地点项
            for (int i = 0; i < locations.size(); i++) {
                JSONObject loc = locations.getJSONObject(i);
                String lng = loc.getStr("lng");
                String lat = loc.getStr("lat");
                
                if (lng != null && lat != null && !lng.isEmpty() && !lat.isEmpty()) {
                    String name = escapeHtml(loc.getStr("name", ""));
                    String type = loc.getStr("type", "other");
                    String time = escapeHtml(loc.getStr("time", ""));
                    String description = escapeHtml(loc.getStr("description", ""));
                    if (description.length() > 80) {
                        description = description.substring(0, 80) + "...";
                    }
                    
                    html.append("        <div class=\"location-item-embedded\" data-index=\"").append(i).append("\" ");
                    html.append("style=\"background: #f8f9fa; border-radius: 8px; padding: 15px; cursor: pointer; transition: all 0.3s ease; border-left: 4px solid #3498db;\">\n");
                    html.append("            <div style=\"font-weight: bold; color: #2c3e50; margin-bottom: 5px;\">");
                    html.append(getIconForType(type)).append(" ").append(name);
                    html.append("            </div>\n");
                    html.append("            <div style=\"font-size: 0.9rem; color: #e74c3c; margin-bottom: 5px;\">");
                    html.append(time);
                    html.append("            </div>\n");
                    html.append("            <div style=\"font-size: 0.85rem; color: #666; line-height: 1.4;\">");
                    html.append(description);
                    html.append("            </div>\n");
                    html.append("        </div>\n");
                }
            }
            
            html.append("    </div>\n");
            html.append("</div>\n");
            
            // 添加地图样式
            html.append("<style>\n");
            html.append("    .location-item-embedded:hover {\n");
            html.append("        transform: translateY(-2px);\n");
            html.append("        box-shadow: 0 6px 12px rgba(0,0,0,0.1);\n");
            html.append("    }\n");
            html.append("    .location-item-embedded.active {\n");
            html.append("        border-left-color: #e74c3c;\n");
            html.append("        background: #fff5f5;\n");
            html.append("    }\n");
            html.append("</style>\n");
            
            return html.toString();
        } catch (Exception e) {
            return "生成地图HTML片段失败：" + e.getMessage();
        }
    }

    /**
     * 生成地图JavaScript代码，用于初始化高德地图并显示地点标记
     * @param locationsJson 地点信息JSON数组，格式：[{"name":"地点名","type":"attraction|restaurant|hotel","time":"时间","description":"描述","lng":"经度","lat":"纬度","address":"地址"},...]
     * @param destination 目的地城市名称
     * @return 地图JavaScript代码
     */
    @Tool(description = "生成地图JavaScript代码，用于初始化高德地图并显示地点标记。需要提供地点信息（包含名称、类型、时间、描述、经纬度坐标）和目的地城市名称")
    public String generateMapJavaScript(
            @ToolParam(description = "地点信息JSON数组，格式：[{\"name\":\"地点名\",\"type\":\"attraction|restaurant|hotel\",\"time\":\"时间\",\"description\":\"描述\",\"lng\":\"经度\",\"lat\":\"纬度\",\"address\":\"地址\"},...]") String locationsJson,
            @ToolParam(description = "目的地城市名称") String destination) {
        try {
            if (jsApiKey == null || securityJsCode == null) {
                return "地图功能未配置：缺少JS API密钥或安全密钥";
            }

            JSONArray locations = JSONUtil.parseArray(locationsJson);
            if (locations == null || locations.size() == 0) {
                return "地点信息为空，无法生成地图脚本";
            }

            StringBuilder script = new StringBuilder();
            
            // 安全密钥配置和脚本加载
            script.append("(function() {\n");
            script.append("    var apiKey = '").append(escapeJs(jsApiKey)).append("';\n");
            script.append("    var securityJsCode = '").append(escapeJs(securityJsCode)).append("';\n");
            script.append("    \n");
            script.append("    // 设置安全密钥配置（必须在JS API加载之前）\n");
            script.append("    window._AMapSecurityConfig = {\n");
            script.append("        securityJsCode: securityJsCode\n");
            script.append("    };\n");
            script.append("    \n");
            script.append("    // 动态加载地图脚本\n");
            script.append("    var script1 = document.createElement('script');\n");
            script.append("    script1.src = 'https://webapi.amap.com/maps?v=2.0&key=' + apiKey;\n");
            script.append("    script1.onerror = function() {\n");
            script.append("        console.error('地图脚本加载失败，请检查网络连接和API密钥');\n");
            script.append("        var errorDiv = document.getElementById('travel-map');\n");
            script.append("        if (errorDiv) {\n");
            script.append("            errorDiv.innerHTML = '<div style=\"padding: 20px; text-align: center; color: #e74c3c;\"><i class=\"fas fa-exclamation-triangle\"></i> 地图加载失败，请检查网络连接</div>';\n");
            script.append("        }\n");
            script.append("    };\n");
            script.append("    script1.onload = function() {\n");
            script.append("        var script2 = document.createElement('script');\n");
            script.append("        script2.src = 'https://webapi.amap.com/ui/1.1/main.js';\n");
            script.append("        script2.onerror = function() {\n");
            script.append("            console.error('地图UI脚本加载失败');\n");
            script.append("            initMap();\n");
            script.append("        };\n");
            script.append("        script2.onload = function() {\n");
            script.append("            initMap();\n");
            script.append("        };\n");
            script.append("        document.head.appendChild(script2);\n");
            script.append("    };\n");
            script.append("    document.head.appendChild(script1);\n");
            script.append("    \n");
            script.append("    function initMap() {\n");
            
            // 地图初始化代码
            script.append("        // 检查地图容器是否存在\n");
            script.append("        var mapContainer = document.getElementById('travel-map');\n");
            script.append("        if (!mapContainer) {\n");
            script.append("            console.error('地图容器不存在');\n");
            script.append("            return;\n");
            script.append("        }\n\n");
            script.append("        // 初始化地图\n");
            script.append("        var map = new AMap.Map('travel-map', {\n");
            script.append("            zoom: 13,\n");
            script.append("            center: [119.973, 31.810], // 默认中心点\n");
            script.append("            viewMode: '3D',\n");
            script.append("            resizeEnable: true\n");
            script.append("        });\n\n");
            script.append("        // 等待地图加载完成\n");
            script.append("        map.on('complete', function() {\n");
            script.append("            console.log('地图加载完成');\n");
            script.append("        });\n\n");
            script.append("        var markers = [];\n");
            script.append("        var infoWindows = [];\n");
            script.append("        var polyline = null;\n\n");
            
            // 地点数据
            script.append("        // 地点数据（只包含有坐标的地点）\n");
            script.append("        var locations = [];\n");
            script.append("        var locationIndexMap = {}; // 列表项索引 -> locations数组索引\n");
            
            int validIndex = 0;
            for (int i = 0; i < locations.size(); i++) {
                JSONObject loc = locations.getJSONObject(i);
                String lng = loc.getStr("lng");
                String lat = loc.getStr("lat");
                
                if (lng != null && lat != null && !lng.isEmpty() && !lat.isEmpty()) {
                    script.append("        locations.push({\n");
                    script.append("            name: '").append(escapeJs(loc.getStr("name", ""))).append("',\n");
                    script.append("            type: '").append(escapeJs(loc.getStr("type", "other"))).append("',\n");
                    script.append("            time: '").append(escapeJs(loc.getStr("time", ""))).append("',\n");
                    script.append("            description: '").append(escapeJs(loc.getStr("description", ""))).append("',\n");
                    script.append("            lng: ").append(lng).append(",\n");
                    script.append("            lat: ").append(lat).append(",\n");
                    script.append("            address: '").append(escapeJs(loc.getStr("address", ""))).append("',\n");
                    script.append("            listIndex: ").append(i).append(" // 原始列表索引\n");
                    script.append("        });\n");
                    script.append("        locationIndexMap[").append(i).append("] = ").append(validIndex).append(";\n");
                    validIndex++;
                }
            }
            script.append("\n");
            
            // 创建标记和信息窗口
            script.append("        // 创建标记和信息窗口（等待地图加载完成）\n");
            script.append("        function addMarkers() {\n");
            script.append("            locations.forEach(function(loc, mapIndex) {\n");
            script.append("                var marker = new AMap.Marker({\n");
            script.append("                    position: [loc.lng, loc.lat],\n");
            script.append("                    title: loc.name,\n");
            script.append("                    icon: getMarkerIcon(loc.type),\n");
            script.append("                    offset: new AMap.Pixel(-13, -30)\n");
            script.append("                });\n\n");
            script.append("                var infoWindow = new AMap.InfoWindow({\n");
            script.append("                    content: '<div style=\"padding: 10px; min-width: 200px; max-width: 300px; box-sizing: border-box;\">' +\n");
            script.append("                        '<h3 style=\"margin: 0 0 10px 0; color: #2c3e50; font-size: 16px; font-weight: bold; word-wrap: break-word;\">' + loc.name + '</h3>' +\n");
            script.append("                        '<p style=\"margin: 5px 0; color: #e74c3c; font-size: 13px; word-wrap: break-word;\"><i class=\"fas fa-clock\"></i> ' + loc.time + '</p>' +\n");
            script.append("                        '<p style=\"margin: 5px 0; color: #666; font-size: 13px; line-height: 1.5; word-wrap: break-word;\">' + loc.description + '</p>' +\n");
            script.append("                        (loc.address ? '<p style=\"margin: 5px 0; color: #999; font-size: 12px; word-wrap: break-word;\"><i class=\"fas fa-map-marker-alt\"></i> ' + loc.address + '</p>' : '') +\n");
            script.append("                        '</div>',\n");
            script.append("                    offset: new AMap.Pixel(0, -31),\n");
            script.append("                    closeWhenClickMap: true,\n");
            script.append("                    autoMove: false\n");
            script.append("                });\n\n");
            script.append("                marker.on('click', function() {\n");
            script.append("                    // 关闭其他信息窗口\n");
            script.append("                    infoWindows.forEach(function(iw) {\n");
            script.append("                        iw.close();\n");
            script.append("                    });\n");
            script.append("                    // 使用标记的位置打开信息窗口\n");
            script.append("                    var position = marker.getPosition();\n");
            script.append("                    infoWindow.open(map, position);\n");
            script.append("                    // 高亮对应的列表项\n");
            script.append("                    document.querySelectorAll('.location-item-embedded').forEach(function(item) {\n");
            script.append("                        item.classList.remove('active');\n");
            script.append("                    });\n");
            script.append("                    var listIndex = loc.listIndex;\n");
            script.append("                    var listItem = document.querySelector('.location-item-embedded[data-index=\"' + listIndex + '\"]');\n");
            script.append("                    if (listItem) {\n");
            script.append("                        listItem.classList.add('active');\n");
            script.append("                        listItem.scrollIntoView({ behavior: 'smooth', block: 'nearest' });\n");
            script.append("                    }\n");
            script.append("                });\n\n");
            script.append("                markers.push(marker);\n");
            script.append("                infoWindows.push(infoWindow);\n");
            script.append("                map.add(marker);\n");
            script.append("            });\n");
            script.append("            \n");
            script.append("            // 绘制路线\n");
            script.append("            if (locations.length > 1) {\n");
            script.append("                var path = locations.map(function(loc) {\n");
            script.append("                    return [loc.lng, loc.lat];\n");
            script.append("                });\n\n");
            script.append("                polyline = new AMap.Polyline({\n");
            script.append("                    path: path,\n");
            script.append("                    isOutline: true,\n");
            script.append("                    outlineColor: '#ffeeff',\n");
            script.append("                    borderWeight: 3,\n");
            script.append("                    strokeColor: '#3366FF',\n");
            script.append("                    strokeOpacity: 1,\n");
            script.append("                    strokeWeight: 5,\n");
            script.append("                    strokeStyle: 'solid',\n");
            script.append("                    lineJoin: 'round',\n");
            script.append("                    lineCap: 'round',\n");
            script.append("                    zIndex: 50\n");
            script.append("                });\n\n");
            script.append("                map.add(polyline);\n");
            script.append("                map.setFitView([polyline], false, [50, 50, 50, 50]);\n");
            script.append("            } else if (locations.length === 1) {\n");
            script.append("                map.setCenter([locations[0].lng, locations[0].lat]);\n");
            script.append("                map.setZoom(15);\n");
            script.append("            }\n");
            script.append("        }\n\n");
            script.append("        // 地图加载完成后添加标记\n");
            script.append("        if (map.getStatus() === 'complete') {\n");
            script.append("            addMarkers();\n");
            script.append("        } else {\n");
            script.append("            map.on('complete', function() {\n");
            script.append("                addMarkers();\n");
            script.append("            });\n");
            script.append("        }\n\n");
            
            // 列表项点击事件
            script.append("        // 列表项点击事件\n");
            script.append("        document.querySelectorAll('.location-item-embedded').forEach(function(item) {\n");
            script.append("            item.addEventListener('click', function() {\n");
            script.append("                var listIndex = parseInt(item.getAttribute('data-index'));\n");
            script.append("                var mapIndex = locationIndexMap[listIndex];\n");
            script.append("                if (mapIndex !== undefined && locations[mapIndex]) {\n");
            script.append("                    var loc = locations[mapIndex];\n");
            script.append("                    // 关闭其他信息窗口\n");
            script.append("                    infoWindows.forEach(function(iw) {\n");
            script.append("                        iw.close();\n");
            script.append("                    });\n");
            script.append("                    map.setZoomAndCenter(16, [loc.lng, loc.lat]);\n");
            script.append("                    // 延迟打开信息窗口，确保地图已移动完成\n");
            script.append("                    setTimeout(function() {\n");
            script.append("                        infoWindows[mapIndex].open(map, [loc.lng, loc.lat]);\n");
            script.append("                    }, 500);\n");
            script.append("                    \n");
            script.append("                    // 高亮列表项\n");
            script.append("                    document.querySelectorAll('.location-item-embedded').forEach(function(i) {\n");
            script.append("                        i.classList.remove('active');\n");
            script.append("                    });\n");
            script.append("                    item.classList.add('active');\n");
            script.append("                }\n");
            script.append("            });\n");
            script.append("        });\n\n");
            
            // 获取标记图标函数
            script.append("        // 根据类型获取标记图标\n");
            script.append("        function getMarkerIcon(type) {\n");
            script.append("            var iconUrl = 'https://webapi.amap.com/theme/v1.3/markers/n/mark_';\n");
            script.append("            var iconColor = 'b';\n");
            script.append("            if (type === 'restaurant') iconColor = 'r';\n");
            script.append("            else if (type === 'hotel') iconColor = 'g';\n");
            script.append("            else if (type === 'attraction') iconColor = 'b';\n");
            script.append("            return iconUrl + iconColor + '.png';\n");
            script.append("        }\n");
            
            script.append("    }\n");
            script.append("})();\n");
            
            return script.toString();
        } catch (Exception e) {
            return "生成地图JavaScript代码失败：" + e.getMessage();
        }
    }

    /**
     * 根据类型获取图标HTML
     */
    private String getIconForType(String type) {
        return switch (type) {
            case "restaurant" -> "<i class=\"fas fa-utensils\"></i>";
            case "hotel" -> "<i class=\"fas fa-hotel\"></i>";
            case "attraction" -> "<i class=\"fas fa-monument\"></i>";
            default -> "<i class=\"fas fa-map-marker-alt\"></i>";
        };
    }

    /**
     * 转义HTML特殊字符
     */
    private String escapeHtml(String str) {
        if (str == null) return "";
        return str.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&#39;");
    }

    /**
     * 转义JavaScript字符串
     */
    private String escapeJs(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("'", "\\'")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r");
    }
}