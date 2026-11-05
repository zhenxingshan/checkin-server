package com.kevin.checkin.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class TaskVO {

    private Long id;
    private String title;
    private String icon;
    private String color;
    private String note;

    @JsonProperty("created_at")
    private String createdAt;
}
