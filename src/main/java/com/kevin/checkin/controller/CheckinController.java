package com.kevin.checkin.controller;

import com.kevin.checkin.common.Result;
import com.kevin.checkin.config.UserContext;
import com.kevin.checkin.dto.CheckinDTO;
import com.kevin.checkin.service.CheckinService;
import com.kevin.checkin.vo.RecordVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/checkins")
public class CheckinController {

    @Autowired
    private CheckinService checkinService;

    /** GET /api/checkins?taskId=1[&from=2026-11-01&to=2026-11-30] */
    @GetMapping
    public Result<List<RecordVO>> list(@RequestParam Long taskId,
                                       @RequestParam(required = false) String from,
                                       @RequestParam(required = false) String to) {
        return Result.ok(checkinService.list(UserContext.get(), taskId, from, to));
    }

    /** POST /api/checkins { taskId, time }；日期由服务端决定。 */
    @PostMapping
    public Result<RecordVO> sign(@RequestBody(required = false) CheckinDTO dto) {
        CheckinDTO payload = dto == null ? new CheckinDTO() : dto;
        return Result.ok(checkinService.sign(UserContext.get(), payload.getTaskId(), payload.getTime()));
    }
}
