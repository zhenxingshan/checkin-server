package com.kevin.checkin.dto;

import lombok.Data;

import java.util.List;

/** 提交完整的任务 id 顺序，后端整列重排 sort_order。 */
@Data
public class TaskOrderDTO {

    private List<Long> ids;
}
