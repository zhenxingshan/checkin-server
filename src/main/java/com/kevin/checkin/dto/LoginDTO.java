package com.kevin.checkin.dto;

import lombok.Data;

@Data
public class LoginDTO {

    /** wx.login 拿到的 code */
    private String code;

    /** 小程序 AppID，仅用于后台多端校验，服务端以自身配置为准 */
    private String appid;
}
