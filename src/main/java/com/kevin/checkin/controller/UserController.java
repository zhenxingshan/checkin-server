package com.kevin.checkin.controller;

import com.kevin.checkin.common.Result;
import com.kevin.checkin.config.UserContext;
import com.kevin.checkin.dto.ProfileDTO;
import com.kevin.checkin.service.UserService;
import com.kevin.checkin.vo.UserVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/profile")
    public Result<UserVO> profile() {
        return Result.ok(userService.current(UserContext.get()));
    }

    @PutMapping("/profile")
    public Result<UserVO> update(@RequestBody(required = false) ProfileDTO dto) {
        ProfileDTO payload = dto == null ? new ProfileDTO() : dto;
        return Result.ok(userService.updateProfile(
                UserContext.get(), payload.getNickname(), payload.getAvatar()));
    }
}
