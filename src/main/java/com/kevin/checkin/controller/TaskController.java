package com.kevin.checkin.controller;

import com.kevin.checkin.common.Result;
import com.kevin.checkin.config.UserContext;
import com.kevin.checkin.dto.TaskDTO;
import com.kevin.checkin.service.TaskService;
import com.kevin.checkin.vo.TaskVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    @Autowired
    private TaskService taskService;

    @GetMapping
    public Result<List<TaskVO>> list() {
        return Result.ok(taskService.list(UserContext.get()));
    }

    @PostMapping
    public Result<TaskVO> create(@RequestBody(required = false) TaskDTO dto) {
        return Result.ok(taskService.create(UserContext.get(), dto));
    }

    @PutMapping("/{id}")
    public Result<TaskVO> update(@PathVariable Long id, @RequestBody(required = false) TaskDTO dto) {
        return Result.ok(taskService.update(UserContext.get(), id, dto));
    }

    @DeleteMapping("/{id}")
    public Result<Object> remove(@PathVariable Long id) {
        taskService.remove(UserContext.get(), id);
        return Result.ok(Collections.singletonMap("removed", 1));
    }
}
