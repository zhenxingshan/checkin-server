package com.kevin.checkin.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/** 撤销当天签到后的结果：前端直接用 points 覆盖积分视图，不必再拉一次资料。 */
@Data
public class CancelVO {

    /** 被撤销的日期（服务端当天）。 */
    private String date;

    /** 回滚/保留后的积分总览。 */
    private PointVO points;

    /** 本次删除的记录条数，0 或 1。 */
    private Integer removed;

    @JsonProperty("earned_points")
    private Integer earnedPoints;
}
