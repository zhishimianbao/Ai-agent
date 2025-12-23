package com.zhishi.aiagent.agent;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class BaseAgentTest {

    @Resource
    private  MyManus myManus;

    @Test
    public void run() {
//        String userPrompt = """
//                我居住在镇江京口区江苏大学，请帮我找到 5 公里内合适的吃饭地点，
//                并结合一些网络图片，制定一份详细的餐饮计划，
//                并以 PDF 格式输出""";
                String userPrompt = """
                我居住在上海市静安区，请帮我找到 5 公里内合适的吃饭地点，
                并以 PDF 格式输出""";
        String answer = myManus.run(userPrompt);
        Assertions.assertNotNull(answer);
    }
}