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
import java.util.concurrent.ThreadLocalRandom;

@Service
public class UserService {

    public static final String DEFAULT_AVATAR = "sprout";

    /**
     * 头像标识白名单，必须与小程序端 {@code AVATAR_PRESETS} 的 key 完全一致：
     * 新增/改名任意一项都要前后端同步，否则前端 presetOf() 会 fallback 成 sprout。
     * 新用户从这里随机抽一个；updateProfile 也只允许落在集合内的 key。
     */
    private static final String[] AVATAR_KEYS = {
            "sprout", "sun", "moon", "fire", "star", "leaf",
            "panda", "rabbit", "fox", "bear", "chick", "fish",
    };

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PointService pointService;

    @Value("${checkin.token-ttl-hours:720}")
    private long tokenTtlHours;

    @Value("${checkin.zone:Asia/Shanghai}")
    private String zone;

    /** 首次登录落库（词库随机昵称/随机头像），每次登录刷新凭证；已有用户不覆盖昵称与头像。 */
    public User loginByOpenid(String openid) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getOpenid, openid)
                .last("limit 1"));
        LocalDateTime now = LocalDateTime.now();
        if (user == null) {
            user = new User();
            user.setOpenid(openid);
            user.setNickname(pickNickname());
            user.setAvatar(pickAvatar());
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

    /** 新用户随机抽一个头像标识；数组为空（极端情况）时退回默认苗，保证不写 null。 */
    private String pickAvatar() {
        if (AVATAR_KEYS.length == 0) {
            return DEFAULT_AVATAR;
        }
        return AVATAR_KEYS[ThreadLocalRandom.current().nextInt(AVATAR_KEYS.length)];
    }

    /** 头像只允许落在白名单内，避免脏 key 进库后前端 presetOf 兜底成苗。 */
    private boolean isValidAvatar(String key) {
        for (String k : AVATAR_KEYS) {
            if (k.equals(key)) {
                return true;
            }
        }
        return false;
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
            String value = avatar.trim();
            if (isValidAvatar(value)) {
                user.setAvatar(value);
            }
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
