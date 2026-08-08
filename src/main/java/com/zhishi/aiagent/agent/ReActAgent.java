package com.zhishi.aiagent.agent;

import cn.hutool.core.util.StrUtil;
import com.zhishi.aiagent.agent.model.AgentState;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

/**
 * ReAct (Reasoning and Acting) 模式的代理抽象类
 * 实现了思考-行动的循环模式
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public abstract class ReActAgent extends BaseAgent {

    /**
     * 模型不再调用工具时生成的最终答案。
     */
    private String finalAnswer;

    /**
     * 保存模型最终答案并结束当前推理循环。
     */
    protected void finishWithAnswer(String answer) {
        setFinalAnswer(answer);
        setState(AgentState.FINISHED);
    }

    /**
     * 处理当前状态并决定下一步行动
     *
     * @return 是否需要执行行动，true表示需要执行，false表示不需要执行
     */
    public abstract boolean think();

    /**
     * 执行决定的行动
     *
     * @return 行动执行结果
     */
    public abstract String act();

    /**
     * 执行单个步骤：思考和行动
     *
     * @return 步骤执行结果
     */
    @Override
    public String step() {
        try {
            // 先思考
            boolean shouldAct = think();
            if (!shouldAct) {
                // 不需要工具时，直接把模型生成的文本作为最终答案返回
                return StrUtil.blankToDefault(finalAnswer, "思考完成 - 无需行动");
            }
            // 再行动
            return act();
        } catch (Exception e) {
            // 记录异常日志
            e.printStackTrace();
            return "步骤执行失败：" + e.getMessage();
        }
    }

}
