package com.kevin.checkin.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 与小程序端用户对象字段一致（下划线风格由 @JsonProperty 显式映射）。
 */
@Data
public class UserVO {

    private Long id;
    private String openid;
    private String nickname;
    private String avatar;

    @JsonProperty("openid_source")
    private String openidSource;

    @JsonProperty("wechat_code_ok")
    private Boolean wechatCodeOk;

    @JsonProperty("logged_in")
    private Boolean loggedIn;

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("last_login_at")
    private String lastLoginAt;
}
