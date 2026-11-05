package com.kevin.checkin.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 积分总览：用户资料与签到响应共用同一个结构，前端统一渲染。
 * gained 只有签到响应里才有值，其余场景为 0。
 */
@Data
public class PointVO {

    /** 累计总积分。 */
    private Integer total;

    /** 今日已获得积分（当天未签到为 0）。 */
    private Integer today;

    /** 按自然日统计的连续签到天数，断签归零后重新从 1 开始。 */
    private Integer streak;

    /** 本次签到获得的积分，0 表示当天已在别的任务计过分。 */
    private Integer gained;

    /** 按当前档位，下一次签到可得积分。 */
    @JsonProperty("per_checkin")
    private Integer perCheckin;

    /** 距离下一档还需要连续签到的次数；已达上限为 0。 */
    @JsonProperty("next_stage_days")
    private Integer nextStageDays;

    /** 单次签到积分上限。 */
    private Integer max;
}
