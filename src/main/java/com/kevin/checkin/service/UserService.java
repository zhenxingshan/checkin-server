package com.kevin.checkin.service;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kevin.checkin.common.BusinessException;
import com.kevin.checkin.common.NicknameGenerator;
import com.kevin.checkin.entity.User;
import com.kevin.checkin.mapper.UserMapper;
import com.kevin.checkin.vo.UserVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
public class UserService {

    public static final String DEFAULT_AVATAR = "sprout";

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PointService pointService;

    @Value("${checkin.token-ttl-hours:720}")
    private long tokenTtlHours;

    @Value("${checkin.zone:Asia/Shanghai}")
    private String zone;

    /** 首次登录落库（词库随机昵称/默认头像），每次登录刷新凭证；已有用户不覆盖昵称。 */
    public User loginByOpenid(String openid) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getOpenid, openid)
                .last("limit 1"));
        LocalDateTime now = LocalDateTime.now();
        if (user == null) {
            user = new User();
            user.setOpenid(openid);
            user.setNickname(pickNickname());
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

    /** 优先挑一个还没被占用的组合；连撞几次就允许重名，昵称本来就不要求全局唯一。 */
    private String pickNickname() {
        for (int i = 0; i < 5; i++) {
            String name = NicknameGenerator.random();
            Long used = userMapper.selectCount(new LambdaQueryWrapper<User>()
                    .eq(User::getNickname, name));
            if (used == null || used == 0L) {
                return name;
            }
        }
        return NicknameGenerator.random();
    }

    public UserVO current(Long userId) {
        return toVOWithPoints(getById(userId));
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
        return toVOWithPoints(user);
    }

    /** 带积分总览的用户视图：登录响应也走这里，避免小程序首屏积分显示为 0。 */
    public UserVO toVOWithPoints(User user) {
        UserVO vo = toVO(user);
        vo.setPoints(pointService.view(user, LocalDate.now(ZoneId.of(zone)), 0));
        return vo;
    }

    public static UserVO toVO(User user) {
        UserVO vo = new UserVO();
        vo.setOpenid(user.getOpenid());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setLoggedIn(true);
        vo.setCreatedAt(format(user.getCreatedAt()));
        return vo;
    }

    private static String format(LocalDateTime time) {
        return time == null ? "" : time.format(FMT);
    }
}
