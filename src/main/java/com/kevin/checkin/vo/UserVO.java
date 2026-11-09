package com.kevin.checkin.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 只返回小程序端真正用到的字段。
 * 不下发自增主键 id（可从 id 推断注册用户量），也不下发登录来源、
 * 微信 code 状态、上次登录时间等内部状态（下划线风格由 @JsonProperty 显式映射）。
 */
@Data
public class UserVO {

    private String openid;
    private String nickname;
    private String avatar;

    @JsonProperty("logged_in")
    private Boolean loggedIn;

    @JsonProperty("created_at")
    private String createdAt;

    /** 账号注册时的客户端 IP；老用户无该值返回 null。 */
    @JsonProperty("register_ip")
    private String registerIp;

    /** 积分总览；老版本后台没有该字段时前端按默认值兜底。 */
    @JsonProperty("points")
    private PointVO points;
}
