package com.kevin.checkin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kevin.checkin.entity.CheckinRecord;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;

public interface CheckinRecordMapper extends BaseMapper<CheckinRecord> {

    /**
     * 查「任意状态」的当天记录。@TableLogic 会给 BaseMapper 的查询自动追加 deleted=0，
     * 这里用自定义 SQL 绕开，才能找到被撤销的那一行并恢复它。
     */
    @Select("SELECT id, user_id, task_id, checkin_date, checkin_time, points, created_at, deleted "
            + "FROM t_checkin_record "
            + "WHERE user_id = #{userId} AND task_id = #{taskId} AND checkin_date = #{date} LIMIT 1")
    CheckinRecord selectAnyByUserTaskDate(@Param("userId") Long userId,
                                          @Param("taskId") Long taskId,
                                          @Param("date") LocalDate date);

    /** 同日再签到：恢复被撤销的行并刷新签到时间、本次积分；updated_at 由数据库自动更新。 */
    @Update("UPDATE t_checkin_record SET deleted = 0, checkin_time = #{time}, points = #{points} "
            + "WHERE id = #{id} AND deleted = 1")
    int restore(@Param("id") Long id, @Param("time") String time, @Param("points") int points);
}
