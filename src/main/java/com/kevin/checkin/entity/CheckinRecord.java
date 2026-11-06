package com.kevin.checkin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("t_checkin_record")
public class CheckinRecord {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long taskId;
    private LocalDate checkinDate;
    private String checkinTime;
    /** 本次签到获得的积分；当天非首次签到为 0。 */
    private Integer points;
    private LocalDateTime createdAt;
    /** 0-有效 1-已撤销；同一天同一任务只保留一行，撤销置 1，同日再签到恢复为 0。 */
    @TableLogic
    private Integer deleted;
}
