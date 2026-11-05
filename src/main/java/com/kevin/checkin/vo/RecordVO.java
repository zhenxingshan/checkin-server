package com.kevin.checkin.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class RecordVO {

    private Long id;

    @JsonProperty("task_id")
    private Long taskId;

    /** 签到日期 yyyy-MM-dd，前端日历直接以该字段为 key。 */
    private String date;

    /** 签到时间 HH:mm。 */
    private String time;

    @JsonProperty("created_at")
    private String createdAt;
}
