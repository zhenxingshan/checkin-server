package com.kevin.checkin.config;

import com.kevin.checkin.common.BusinessException;

/**
 * 当前登录用户，仅由 TokenInterceptor 写入，业务层不信任任何前端传来的 userId。
 */
public final class UserContext {

    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(Long userId) {
        USER_ID.set(userId);
    }

    public static Long get() {
        Long userId = USER_ID.get();
        if (userId == null) {
            throw new BusinessException("UNAUTHORIZED", "登录已过期，请重新登录");
        }
        return userId;
    }

    public static void clear() {
        USER_ID.remove();
    }
}
