package com.zhishi.aiagent.controller;

import com.zhishi.aiagent.service.MapService;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 地图展示Controller
 */
@RestController
@Slf4j
public class MapController {

    private final MapService mapService;

    @Value("${map.js-key}")
    private String jsApiKey;

    @Value("${map.security-js-code}")
    private String securityJsCode;

    public MapController(MapService mapService) {
        this.mapService = mapService;
    }

    /**
     * 根据HTML文件生成带地图的页面（嵌入到原始HTML中）
     * @param fileName HTML文件名（如：travel_plan_常州.html）
     * @return 带地图的HTML页面
     */
    @GetMapping("/map/show")
    public ResponseEntity<String> showMapWithLocations(@RequestParam String fileName) {
        try {
            // 读取HTML文件
            Path filePath = Paths.get("tmp/file", fileName);
            if (!Files.exists(filePath)) {
                return ResponseEntity.badRequest()
                    .body("文件不存在: " + fileName);
            }

            String htmlContent = Files.readString(filePath);

            // 提取地点信息
            List<MapService.LocationInfo> locations = mapService.extractLocationsFromHtml(htmlContent);

            // 提取目的地城市
            String destination = mapService.extractDestinationFromHtml(htmlContent);
            if (destination.isEmpty()) {
                destination = "目的地";
            }

            // 获取地点坐标
            mapService.geocodeLocations(locations, destination);

            // 将地图嵌入到原始HTML中
            String mapHtml = embedMapIntoHtml(htmlContent, locations, destination);

            return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(mapHtml);

        } catch (Exception e) {
            log.error("生成地图页面失败", e);
            return ResponseEntity.internalServerError()
                .body("生成地图页面失败: " + e.getMessage());
        }
    }

    /**
     * 根据HTML内容直接生成带地图的页面（不依赖文件）
     * @param htmlContent HTML内容
     * @return 带地图的HTML页面
     */
    @PostMapping("/map/generate")
    public ResponseEntity<String> generateMapFromHtml(@RequestBody String htmlContent) {
        if (htmlContent == null || htmlContent.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body("HTML内容不能为空");
        }

        try {
            // 提取地点信息
            List<MapService.LocationInfo> locations = mapService.extractLocationsFromHtml(htmlContent);

            // 提取目的地城市
            String destination = mapService.extractDestinationFromHtml(htmlContent);
            if (destination.isEmpty()) {
                destination = "目的地"; // 默认值
            }

            // 获取地点坐标
            mapService.geocodeLocations(locations, destination);

            // 生成带地图的HTML
            String mapHtml = generateMapHtml(locations, destination, jsApiKey);

            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(mapHtml);

        } catch (Exception e) {
            log.error("生成地图页面失败", e);
            return ResponseEntity.internalServerError()
                    .body("生成地图页面失败: " + e.getMessage());
        }
    }


    /**
     * 将地图嵌入到原始HTML中
     */
    private String embedMapIntoHtml(String htmlContent, List<MapService.LocationInfo> locations, String destination) {
        try {
            Document doc = Jsoup.parse(htmlContent);

            // 检查是否已经存在地图部分
            Element existingMap = doc.selectFirst("#travel-map-section");
            if (existingMap != null) {
                existingMap.remove();
            }

            // 在content部分末尾添加地图section
            Element content = doc.selectFirst(".content");
            if (content == null) {
                // 如果没有.content，则在body末尾添加
                content = doc.body();
            }

            // 创建地图section
            Element mapSection = new Element("div");
            mapSection.attr("id", "travel-map-section");
            mapSection.attr("class", "section");

            // 添加地图标题
            Element mapTitle = new Element("h2");
            mapTitle.attr("class", "section-title");
            mapTitle.html("<i class=\"fas fa-map-marked-alt\"></i> 旅游路线地图");
            mapSection.appendChild(mapTitle);

            // 添加地图容器
            Element mapWrapper = new Element("div");
            mapWrapper.attr("class", "map-wrapper");
            mapWrapper.attr("style", "margin-top: 20px; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 15px rgba(0,0,0,0.1);");

            Element mapDiv = new Element("div");
            mapDiv.attr("id", "travel-map");
            mapDiv.attr("style", "width:100%;height:600px;");
            mapWrapper.appendChild(mapDiv);

            // 添加地点列表容器
            Element locationListDiv = new Element("div");
            locationListDiv.attr("id", "location-list");
            locationListDiv.attr("class", "location-list-embedded");
            locationListDiv.attr("style", "margin-top: 20px; display: grid; grid-template-columns: repeat(auto-fill, minmax(250px, 1fr)); gap: 15px;");

            // 添加地点项
            for (int i = 0; i < locations.size(); i++) {
                MapService.LocationInfo loc = locations.get(i);
                if (loc.lng != null && loc.lat != null && !loc.lng.isEmpty() && !loc.lat.isEmpty()) {
                    Element locationItem = new Element("div");
                    locationItem.attr("class", "location-item-embedded");
                    locationItem.attr("data-index", String.valueOf(i));
                    locationItem.attr("style", "background: #f8f9fa; border-radius: 8px; padding: 15px; cursor: pointer; transition: all 0.3s ease; border-left: 4px solid #3498db;");
                    locationItem.html(
                            "<div style=\"font-weight: bold; color: #2c3e50; margin-bottom: 5px;\">" +
                                    getIconForType(loc.type) + " " + escapeHtml(loc.name) +
                                    "</div>" +
                                    "<div style=\"font-size: 0.9rem; color: #e74c3c; margin-bottom: 5px;\">" +
                                    escapeHtml(loc.time) +
                                    "</div>" +
                                    "<div style=\"font-size: 0.85rem; color: #666; line-height: 1.4;\">" +
                                    escapeHtml(loc.description.length() > 80 ? loc.description.substring(0, 80) + "..." : loc.description) +
                                    "</div>"
                    );
                    locationListDiv.appendChild(locationItem);
                }
            }

            mapSection.appendChild(mapWrapper);
            mapSection.appendChild(locationListDiv);
            content.appendChild(mapSection);

            // 添加地图样式
            Element style = doc.selectFirst("style");
            if (style == null) {
                style = new Element("style");
                doc.head().appendChild(style);
            }
            style.appendText("\n        /* 地图样式 */\n");
            style.appendText("        .location-item-embedded:hover {\n");
            style.appendText("            transform: translateY(-2px);\n");
            style.appendText("            box-shadow: 0 6px 12px rgba(0,0,0,0.1);\n");
            style.appendText("        }\n");
            style.appendText("        .location-item-embedded.active {\n");
            style.appendText("            border-left-color: #e74c3c;\n");
            style.appendText("            background: #fff5f5;\n");
            style.appendText("        }\n");

            // 添加地图脚本（通过后端接口获取API密钥）
            Element body = doc.body();

            // 添加地图脚本，直接将API密钥嵌入（避免fetch请求失败，支持本地文件打开）
            StringBuilder scriptContent = new StringBuilder();
            scriptContent.append("(function() {\n");
            scriptContent.append("    var apiKey = '").append(escapeJs(jsApiKey)).append("';\n");
            scriptContent.append("    var securityJsCode = '").append(escapeJs(securityJsCode)).append("';\n");
            scriptContent.append("    \n");
            scriptContent.append("    // 设置安全密钥配置（必须在JS API加载之前）\n");
            scriptContent.append("    window._AMapSecurityConfig = {\n");
            scriptContent.append("        securityJsCode: securityJsCode\n");
            scriptContent.append("    };\n");
            scriptContent.append("    \n");
            scriptContent.append("    // 动态加载地图脚本\n");
            scriptContent.append("    var script1 = document.createElement('script');\n");
            scriptContent.append("    script1.src = 'https://webapi.amap.com/maps?v=2.0&key=' + apiKey;\n");
            scriptContent.append("    script1.onerror = function() {\n");
            scriptContent.append("        console.error('地图脚本加载失败，请检查网络连接和API密钥');\n");
            scriptContent.append("        var errorDiv = document.getElementById('travel-map');\n");
            scriptContent.append("        if (errorDiv) {\n");
            scriptContent.append("            errorDiv.innerHTML = '<div style=\"padding: 20px; text-align: center; color: #e74c3c;\"><i class=\"fas fa-exclamation-triangle\"></i> 地图加载失败，请检查网络连接</div>';\n");
            scriptContent.append("        }\n");
            scriptContent.append("    };\n");
            scriptContent.append("    script1.onload = function() {\n");
            scriptContent.append("        var script2 = document.createElement('script');\n");
            scriptContent.append("        script2.src = 'https://webapi.amap.com/ui/1.1/main.js';\n");
            scriptContent.append("        script2.onerror = function() {\n");
            scriptContent.append("            console.error('地图UI脚本加载失败');\n");
            scriptContent.append("            // UI脚本加载失败不影响基本地图功能，继续初始化\n");
            scriptContent.append("            initMap();\n");
            scriptContent.append("        };\n");
            scriptContent.append("        script2.onload = function() {\n");
            scriptContent.append("            initMap();\n");
            scriptContent.append("        };\n");
            scriptContent.append("        document.head.appendChild(script2);\n");
            scriptContent.append("    };\n");
            scriptContent.append("    document.head.appendChild(script1);\n");
            scriptContent.append("    \n");
            scriptContent.append("    function initMap() {\n");
            scriptContent.append(generateMapScript(locations, destination));
            scriptContent.append("    }\n");
            scriptContent.append("})();\n");

            // 创建script元素
            Element mapScript = new Element("script");
            mapScript.attr("type", "text/javascript");
            body.appendChild(mapScript);

            // 生成HTML后，手动替换script标签内容，避免转义
            String html = doc.html();

            // 找到script标签的位置并替换内容
            String scriptTagStart = "<script type=\"text/javascript\">";
            String scriptTagEnd = "</script>";
            int startIdx = html.lastIndexOf(scriptTagStart);
            int endIdx = html.lastIndexOf(scriptTagEnd);

            if (startIdx != -1 && endIdx != -1) {
                String beforeScript = html.substring(0, startIdx + scriptTagStart.length());
                String afterScript = html.substring(endIdx);
                html = beforeScript + scriptContent.toString() + afterScript;
            }

            return html;

        } catch (Exception e) {
            log.error("嵌入地图到HTML失败", e);
            return htmlContent; // 失败时返回原始内容
        }
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
     * 生成包含高德地图的HTML页面（保留用于兼容）
     */
    private String generateMapHtml(List<MapService.LocationInfo> locations, String destination, String apiKey) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang=\"zh-CN\">\n");
        html.append("<head>\n");
        html.append("    <meta charset=\"UTF-8\">\n");
        html.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("    <title>").append(destination).append("旅游路线地图</title>\n");
        html.append("    <link rel=\"stylesheet\" href=\"https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css\">\n");
        html.append("    <style>\n");
        html.append(getMapPageStyles());
        html.append("    </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        html.append("    <div class=\"container\">\n");
        html.append("        <header>\n");
        html.append("            <h1><i class=\"fas fa-map-marked-alt\"></i> ").append(destination).append("旅游路线地图</h1>\n");
        html.append("        </header>\n");
        html.append("        <div class=\"content-wrapper\">\n");
        html.append("            <div class=\"map-container\">\n");
        html.append("                <div id=\"map\" style=\"width:100%;height:600px;\"></div>\n");
        html.append("            </div>\n");
        html.append("            <div class=\"location-list\">\n");
        html.append("                <h2><i class=\"fas fa-list\"></i> 地点列表</h2>\n");
        html.append("                <div class=\"locations\">\n");

        // 添加地点列表
        for (int i = 0; i < locations.size(); i++) {
            MapService.LocationInfo loc = locations.get(i);
            String icon = getIconForType(loc.type);
            html.append("                    <div class=\"location-item\" data-index=\"").append(i).append("\">\n");
            html.append("                        <div class=\"location-icon\">").append(icon).append("</div>\n");
            html.append("                        <div class=\"location-info\">\n");
            html.append("                            <div class=\"location-name\">").append(loc.name).append("</div>\n");
            html.append("                            <div class=\"location-time\">").append(loc.time).append("</div>\n");
            html.append("                            <div class=\"location-desc\">").append(loc.description.length() > 50 ?
                loc.description.substring(0, 50) + "..." : loc.description).append("</div>\n");
            html.append("                        </div>\n");
            html.append("                    </div>\n");
        }

        html.append("                </div>\n");
        html.append("            </div>\n");
        html.append("        </div>\n");
        html.append("    </div>\n");

        // 添加安全密钥配置（在JS API加载之前）
        html.append("    <script type=\"text/javascript\">\n");
        html.append("        window._AMapSecurityConfig = {\n");
        html.append("            securityJsCode: '").append(escapeJs(securityJsCode)).append("'\n");
        html.append("        };\n");
        html.append("    </script>\n");

        // 添加JavaScript代码
        html.append("    <script type=\"text/javascript\" src=\"https://webapi.amap.com/maps?v=2.0&key=").append(apiKey).append("\"></script>\n");
        html.append("    <script type=\"text/javascript\" src=\"https://webapi.amap.com/ui/1.1/main.js\"></script>\n");
        html.append("    <script>\n");
        html.append(generateMapScript(locations, destination));
        html.append("    </script>\n");
        html.append("</body>\n");
        html.append("</html>\n");

        return html.toString();
    }

    /**
     * 获取地图页面样式
     */
    private String getMapPageStyles() {
        return """
            * {
                margin: 0;
                padding: 0;
                box-sizing: border-box;
            }

            body {
                font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                background: linear-gradient(135deg, #f5f7fa 0%, #e4edf9 100%);
                padding: 20px;
            }

            .container {
                max-width: 1400px;
                margin: 0 auto;
                background: white;
                border-radius: 15px;
                box-shadow: 0 10px 30px rgba(0,0,0,0.1);
                overflow: hidden;
            }

            header {
                background: linear-gradient(135deg, #2c3e50 0%, #34495e 100%);
                color: white;
                padding: 30px;
                text-align: center;
            }

            header h1 {
                font-size: 2.2rem;
                text-shadow: 2px 2px 4px rgba(0,0,0,0.3);
            }

            .content-wrapper {
                display: flex;
                gap: 20px;
                padding: 20px;
            }

            .map-container {
                flex: 2;
                border-radius: 10px;
                overflow: hidden;
                box-shadow: 0 4px 15px rgba(0,0,0,0.1);
            }

            .location-list {
                flex: 1;
                background: #f8f9fa;
                border-radius: 10px;
                padding: 20px;
                max-height: 600px;
                overflow-y: auto;
            }

            .location-list h2 {
                color: #2c3e50;
                margin-bottom: 20px;
                font-size: 1.5rem;
            }

            .location-item {
                background: white;
                border-radius: 8px;
                padding: 15px;
                margin-bottom: 15px;
                cursor: pointer;
                transition: all 0.3s ease;
                border-left: 4px solid #3498db;
            }

            .location-item:hover {
                transform: translateX(5px);
                box-shadow: 0 4px 12px rgba(0,0,0,0.15);
            }

            .location-item.active {
                border-left-color: #e74c3c;
                background: #fff5f5;
            }

            .location-item {
                display: flex;
                gap: 15px;
            }

            .location-icon {
                font-size: 2rem;
                color: #3498db;
                display: flex;
                align-items: center;
            }

            .location-info {
                flex: 1;
            }

            .location-name {
                font-size: 1.1rem;
                font-weight: bold;
                color: #2c3e50;
                margin-bottom: 5px;
            }

            .location-time {
                font-size: 0.9rem;
                color: #e74c3c;
                margin-bottom: 5px;
            }

            .location-desc {
                font-size: 0.85rem;
                color: #666;
                line-height: 1.4;
            }

            @media (max-width: 968px) {
                .content-wrapper {
                    flex-direction: column;
                }

                .map-container {
                    height: 400px;
                }
            }
            """;
    }

    /**
     * 根据类型获取图标
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
     * 生成地图JavaScript代码
     */
    private String generateMapScript(List<MapService.LocationInfo> locations, String destination) {
        StringBuilder script = new StringBuilder();
        script.append("        // 检查地图容器是否存在\n");
        script.append("        var mapContainer = document.getElementById('travel-map');\n");
        script.append("        if (!mapContainer) {\n");
        script.append("            console.error('地图容器不存在');\n");
        script.append("            return;\n");
        script.append("        }\n\n");
        script.append("        // 初始化地图\n");
        script.append("        var map = new AMap.Map('travel-map', {\n");
        script.append("            zoom: 13,\n");
        script.append("            center: [119.973, 31.810], // 默认中心点（常州）\n");
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

        // 添加标记点
        script.append("        // 地点数据（只包含有坐标的地点）\n");
        script.append("        var locations = [];\n");
        script.append("        var locationIndexMap = {}; // 列表项索引 -> locations数组索引\n");

        int validIndex = 0;
        for (int i = 0; i < locations.size(); i++) {
            MapService.LocationInfo loc = locations.get(i);
            if (loc.lng != null && loc.lat != null && !loc.lng.isEmpty() && !loc.lat.isEmpty()) {
                script.append("        locations.push({\n");
                script.append("            name: '").append(escapeJs(loc.name)).append("',\n");
                script.append("            type: '").append(escapeJs(loc.type)).append("',\n");
                script.append("            time: '").append(escapeJs(loc.time)).append("',\n");
                script.append("            description: '").append(escapeJs(loc.description)).append("',\n");
                script.append("            lng: ").append(loc.lng).append(",\n");
                script.append("            lat: ").append(loc.lat).append(",\n");
                script.append("            address: '").append(escapeJs(loc.address != null ? loc.address : "")).append("',\n");
                script.append("            listIndex: ").append(i).append(" // 原始列表索引\n");
                script.append("        });\n");
                script.append("        locationIndexMap[").append(i).append("] = ").append(validIndex).append(";\n");
                validIndex++;
            }
        }
        script.append("\n");

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

        // 获取标记图标
        script.append("        // 根据类型获取标记图标\n");
        script.append("        function getMarkerIcon(type) {\n");
        script.append("            var iconUrl = 'https://webapi.amap.com/theme/v1.3/markers/n/mark_';\n");
        script.append("            var iconColor = 'b';\n");
        script.append("            if (type === 'restaurant') iconColor = 'r';\n");
        script.append("            else if (type === 'hotel') iconColor = 'g';\n");
        script.append("            else if (type === 'attraction') iconColor = 'b';\n");
        script.append("            return iconUrl + iconColor + '.png';\n");
        script.append("        }\n");

        return script.toString();
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
