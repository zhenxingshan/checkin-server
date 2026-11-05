package com.kevin.checkin.config;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kevin.checkin.common.Result;
import com.kevin.checkin.entity.User;
import com.kevin.checkin.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;

@Component
public class TokenInterceptor implements HandlerInterceptor {

    @Autowired
    private UserMapper userMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String token = request.getHeader("X-Token");
        User user = null;
        if (StrUtil.isNotBlank(token)) {
            user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                    .eq(User::getToken, token)
                    .gt(User::getTokenExpireAt, LocalDateTime.now())
                    .last("limit 1"));
        }
        if (user == null) {
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(JSONUtil.toJsonStr(
                    Result.fail("UNAUTHORIZED", "登录已过期，请重新登录")));
            return false;
        }
        UserContext.set(user.getId());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        UserContext.clear();
    }
}
