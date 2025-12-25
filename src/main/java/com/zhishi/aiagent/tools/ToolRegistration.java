package com.zhishi.aiagent.tools;

import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 集中的工具注册类
 */
@Configuration
public class ToolRegistration {

//    @Value("${search-api.api-key}")
    private String searchApiKey;
    
    @Value("${amap.api-key}")
    private String amapApiKey;

    @Value("${map.js-key:}")
    private String jsApiKey;

    @Value("${map.security-js-code:}")
    private String securityJsCode;

    @Bean
    public ToolCallback[] allTools() {
        FileOperationTool fileOperationTool = new FileOperationTool();
//        WebSearchTool webSearchTool = new WebSearchTool(searchApiKey);
        WebScrapingTool webScrapingTool = new WebScrapingTool();
        ResourceDownloadTool resourceDownloadTool = new ResourceDownloadTool();
        TerminalOperationTool terminalOperationTool = new TerminalOperationTool();
        PDFGenerationTool pdfGenerationTool = new PDFGenerationTool();
        TerminateTool terminateTool = new TerminateTool();
        AmapAPITool amapAPITool = new AmapAPITool(amapApiKey, jsApiKey, securityJsCode);
        return ToolCallbacks.from(
                fileOperationTool,
//                webSearchTool,
                webScrapingTool,
                resourceDownloadTool,
                terminalOperationTool,
                pdfGenerationTool,
                terminateTool,
                amapAPITool
        );
    }
}
