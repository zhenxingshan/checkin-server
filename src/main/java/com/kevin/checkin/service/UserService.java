package com.kevin.checkin.service;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.kevin.checkin.common.BusinessException;
import com.kevin.checkin.entity.User;
import com.kevin.checkin.mapper.UserMapper;
import com.kevin.checkin.vo.UserVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class UserService {

    public static final String DEFAULT_NICKNAME = "签到达人";
    public static final String DEFAULT_AVATAR = "sprout";

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private UserMapper userMapper;

    @Value("${checkin.token-ttl-hours:720}")
    private long tokenTtlHours;

    /** 首次登录落库（默认昵称/头像），每次登录刷新凭证。 */
    public User loginByOpenid(String openid) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getOpenid, openid)
                .last("limit 1"));
        LocalDateTime now = LocalDateTime.now();
        if (user == null) {
            user = new User();
            user.setOpenid(openid);
            user.setNickname(DEFAULT_NICKNAME);
            user.setAvatar(DEFAULT_AVATAR);
            user.setCreatedAt(now);
            userMapper.insert(user);
        }
        user.setToken(IdUtil.fastSimpleUUID());
        user.setTokenExpireAt(now.plusHours(tokenTtlHours));
        user.setLastLoginAt(now);
        user.setUpdatedAt(now);
        userMapper.updateById(user);
        return user;
    }

    public UserVO current(Long userId) {
        return toVO(getById(userId));
    }

    public User getById(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("UNAUTHORIZED", "登录已过期，请重新登录");
        }
        return user;
    }

    public UserVO updateProfile(Long userId, String nickname, String avatar) {
        User user = getById(userId);
        if (nickname != null) {
            String value = nickname.trim();
            if (value.isEmpty()) {
                throw new BusinessException("INVALID_PAYLOAD", "昵称不能为空");
            }
            user.setNickname(value.length() > 16 ? value.substring(0, 16) : value);
        }
        if (avatar != null && !avatar.trim().isEmpty()) {
            user.setAvatar(avatar.trim());
        }
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        return toVO(user);
    }

    /** updateById 默认忽略 null，这里用 set(..., null) 显式清空凭证。 */
    public void logout(Long userId) {
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getToken, null)
                .set(User::getTokenExpireAt, null));
    }

    public static UserVO toVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setOpenid(user.getOpenid());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setOpenidSource(String.valueOf(user.getOpenid()).startsWith("dev_") ? "dev" : "wechat");
        vo.setWechatCodeOk(true);
        vo.setLoggedIn(true);
        vo.setCreatedAt(format(user.getCreatedAt()));
        vo.setLastLoginAt(format(user.getLastLoginAt()));
        return vo;
    }

    private static String format(LocalDateTime time) {
        return time == null ? "" : time.format(FMT);
    }
}
