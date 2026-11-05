package com.kevin.checkin.dto;

import lombok.Data;

@Data
public class CheckinDTO {

    private Long taskId;

    /** 仅用于展示；签到日期一律以服务端当天为准，不接受前端补签。 */
    private String time;
}
