package com.kevin.checkin.vo;

import lombok.Data;

/**
 * id 是前端选中/编辑/删除/签到时必须回传的业务标识，保留；
 * note 与创建时间前端没用到，不下发。
 */
@Data
public class TaskVO {

    private Long id;
    private String title;
    private String icon;
    private String color;
}
