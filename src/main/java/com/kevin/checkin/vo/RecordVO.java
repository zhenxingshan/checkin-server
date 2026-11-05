package com.kevin.checkin.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 只返回日历/记录列表需要的字段。
 * 不下发记录自增 id、user/task 外键与创建时间戳，避免暴露内部数据规模。
 */
@Data
public class RecordVO {

    /** 签到日期 yyyy-MM-dd，前端日历直接以该字段为 key。 */
    private String date;

    /** 签到时间 HH:mm。 */
    private String time;

    /** 该条记录获得的积分，0 表示当天非首次签到。 */
    @JsonProperty("earned_points")
    private Integer earnedPoints;

    /** 仅签到响应返回：计分完成后的积分总览。 */
    @JsonProperty("points")
    private PointVO points;
}
