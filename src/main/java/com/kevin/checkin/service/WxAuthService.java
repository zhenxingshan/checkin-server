package com.kevin.checkin.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.kevin.checkin.common.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * wx.login 的 code 只能在服务端换取 openId。
 * AppSecret 只存在于本服务的配置/环境变量中，绝不下发到小程序端。
 */
@Service
public class WxAuthService {

    @Value("${wx.appid}")
    private String appid;

    @Value("${wx.secret}")
    private String secret;

    @Value("${wx.code2session-url}")
    private String code2SessionUrl;

    @Value("${wx.dev-mode:false}")
    private boolean devMode;

    public String exchangeOpenid(String code) {
        if (StrUtil.isBlank(code)) {
            throw new BusinessException("WX_CODE_INVALID", "微信登录凭证为空");
        }
        // 本地联调：跳过微信接口，同一 code 稳定映射到同一模拟 openid。
        if (devMode) {
            return "dev_" + Integer.toHexString(code.hashCode());
        }
        if (StrUtil.isBlank(secret)) {
            throw new BusinessException("WX_NOT_CONFIGURED", "后台未配置 AppSecret，无法换取 openId");
        }
        String url = code2SessionUrl + "?appid=" + appid + "&secret=" + secret
                + "&js_code=" + code + "&grant_type=authorization_code";
        String body;
        try {
            body = HttpUtil.get(url, 5000);
        } catch (Exception e) {
            throw new BusinessException("WX_NETWORK_ERROR", "微信接口请求失败，请稍后重试");
        }
        JSONObject json = JSONUtil.parseObj(body);
        String openid = json.getStr("openid");
        if (StrUtil.isBlank(openid)) {
            String errmsg = json.getStr("errmsg");
            throw new BusinessException("WX_CODE_INVALID",
                    "微信登录失败：" + (StrUtil.isBlank(errmsg) ? "未知错误" : errmsg));
        }
        return openid;
    }
}
