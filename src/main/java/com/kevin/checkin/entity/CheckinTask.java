package com.kevin.checkin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_checkin_task")
public class CheckinTask {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String title;
    private String icon;
    private String color;
    private String note;
    /** 展示顺序，越小越靠前；由 /api/tasks/reorder 整列重排。 */
    @TableField("sort_order")
    private Integer sortOrder;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
