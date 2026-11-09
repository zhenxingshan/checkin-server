package com.kevin.checkin.controller;

import com.kevin.checkin.common.Result;
import com.kevin.checkin.common.IpUtil;
import com.kevin.checkin.dto.LoginDTO;
import com.kevin.checkin.entity.User;
import com.kevin.checkin.service.UserService;
import com.kevin.checkin.service.WxAuthService;
import com.kevin.checkin.vo.LoginVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private WxAuthService wxAuthService;

    @Autowired
    private UserService userService;

    /** POST /api/auth/login { code } -> { token, user }，无需 token。 */
    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody(required = false) LoginDTO dto,
                                 HttpServletRequest request) {
        String openid = wxAuthService.exchangeOpenid(dto == null ? null : dto.getCode());
        String registerIp = IpUtil.getClientIp(request);
        User user = userService.loginByOpenid(openid, registerIp);
        LoginVO vo = new LoginVO();
        vo.setToken(user.getToken());
        vo.setUser(userService.toVOWithPoints(user));
        return Result.ok(vo);
    }
}
