package com.zhishi.aiagent.agent;

import com.zhishi.aiagent.agent.model.AgentState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReActAgentFlowTest {

    @Test
    void shouldReturnFinalAnswerAndFinishWhenNoToolIsNeeded() {
        TestReActAgent agent = new TestReActAgent(false);

        String result = agent.run("直接回答这个问题");

        assertTrue(result.contains("这是模型直接生成的最终答案"));
        assertFalse(result.contains("Reached max steps"));
        assertEquals(AgentState.FINISHED, agent.getState());
        assertEquals(1, agent.getCurrentStep());
        assertEquals(0, agent.getActCount());
    }

    @Test
    void shouldContinueAfterToolResultAndFinishWithFinalAnswer() {
        TestReActAgent agent = new TestReActAgent(true);

        String result = agent.run("先调用工具再回答");

        assertTrue(result.contains("工具执行结果"));
        assertTrue(result.contains("结合工具结果生成的最终答案"));
        assertFalse(result.contains("Reached max steps"));
        assertEquals(AgentState.FINISHED, agent.getState());
        assertEquals(2, agent.getCurrentStep());
        assertEquals(1, agent.getActCount());
    }

    private static final class TestReActAgent extends ReActAgent {

        private final boolean needsTool;
        private int thinkCount;
        private int actCount;

        private TestReActAgent(boolean needsTool) {
            this.needsTool = needsTool;
            setMaxSteps(5);
        }

        @Override
        public boolean think() {
            thinkCount++;
            if (needsTool && thinkCount == 1) {
                return true;
            }
            setFinalAnswer(needsTool
                    ? "结合工具结果生成的最终答案"
                    : "这是模型直接生成的最终答案");
            finishWithAnswer(getFinalAnswer());
            return false;
        }

        @Override
        public String act() {
            actCount++;
            return "工具执行结果";
        }

        private int getActCount() {
            return actCount;
        }
    }
}
