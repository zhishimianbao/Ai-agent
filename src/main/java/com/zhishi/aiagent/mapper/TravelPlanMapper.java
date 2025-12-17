package com.zhishi.aiagent.mapper;

import com.zhishi.aiagent.dto.TravelPlanDTO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;

@Mapper
public interface TravelPlanMapper {

    @Insert("INSERT INTO travel_plan_records (chat_id, model_name, input_tokens, output_tokens, total_tokens, created_time) " +
            "VALUES (#{chatId}, #{modelName}, #{inputTokens}, #{outputTokens}, #{totalTokens}, #{createdTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertCost(TravelPlanDTO dto);
}