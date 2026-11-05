package com.kevin.checkin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kevin.checkin.entity.User;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface UserMapper extends BaseMapper<User> {

    /** 创建任务时锁住用户行，把同一用户的并发创建串行化，避免突破任务数上限。 */
    @Select("select id from t_user where id = #{id} for update")
    Long lockById(@Param("id") Long id);
}
