package com.kevin.checkin.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kevin.checkin.common.BusinessException;
import com.kevin.checkin.dto.TaskDTO;
import com.kevin.checkin.entity.CheckinRecord;
import com.kevin.checkin.entity.CheckinTask;
import com.kevin.checkin.mapper.CheckinRecordMapper;
import com.kevin.checkin.mapper.CheckinTaskMapper;
import com.kevin.checkin.mapper.UserMapper;
import com.kevin.checkin.vo.TaskVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TaskService {

    @Autowired
    private CheckinTaskMapper taskMapper;

    @Autowired
    private CheckinRecordMapper recordMapper;

    @Autowired
    private UserMapper userMapper;

    @Value("${checkin.task-limit:10}")
    private int taskLimit;

    public List<TaskVO> list(Long userId) {
        return taskMapper.selectList(new LambdaQueryWrapper<CheckinTask>()
                        .eq(CheckinTask::getUserId, userId)
                        .orderByAsc(CheckinTask::getId))
                .stream().map(TaskService::toVO).collect(Collectors.toList());
    }

    /**
     * 每人最多 taskLimit 个任务。先锁用户行把同一用户的并发创建串行化，
     * 再统计未删除的任务数（@TableLogic 自动排除已删除），最后才插入。
     */
    @Transactional(rollbackFor = Exception.class)
    public TaskVO create(Long userId, TaskDTO dto) {
        userMapper.lockById(userId);
        Long count = taskMapper.selectCount(new LambdaQueryWrapper<CheckinTask>()
                .eq(CheckinTask::getUserId, userId));
        if (count != null && count >= taskLimit) {
            throw new BusinessException("TASK_LIMIT", "最多只能创建 " + taskLimit + " 个签到任务");
        }
        TaskDTO payload = dto == null ? new TaskDTO() : dto;
        String title = StrUtil.nullToEmpty(payload.getTitle()).trim();
        if (title.isEmpty()) {
            throw new BusinessException("INVALID_PAYLOAD", "请输入任务名称");
        }
        CheckinTask task = new CheckinTask();
        task.setUserId(userId);
        task.setTitle(title.length() > 20 ? title.substring(0, 20) : title);
        task.setIcon(StrUtil.isBlank(payload.getIcon()) ? "🌱" : payload.getIcon());
        task.setColor(StrUtil.isBlank(payload.getColor()) ? "#4C6FFF" : payload.getColor());
        task.setNote(StrUtil.nullToEmpty(payload.getNote()));
        task.setDeleted(0);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        taskMapper.insert(task);
        return toVO(task);
    }

    public TaskVO update(Long userId, Long id, TaskDTO dto) {
        CheckinTask task = owned(userId, id);
        TaskDTO payload = dto == null ? new TaskDTO() : dto;
        if (StrUtil.isNotBlank(payload.getTitle())) {
            String title = payload.getTitle().trim();
            task.setTitle(title.length() > 20 ? title.substring(0, 20) : title);
        }
        if (StrUtil.isNotBlank(payload.getIcon())) {
            task.setIcon(payload.getIcon());
        }
        if (StrUtil.isNotBlank(payload.getColor())) {
            task.setColor(payload.getColor());
        }
        if (payload.getNote() != null) {
            task.setNote(payload.getNote());
        }
        task.setUpdatedAt(LocalDateTime.now());
        taskMapper.updateById(task);
        return toVO(task);
    }

    /** 删除任务时同事务清掉该任务下的所有签到记录，避免残留脏数据。 */
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long userId, Long id) {
        owned(userId, id);
        recordMapper.delete(new LambdaQueryWrapper<CheckinRecord>()
                .eq(CheckinRecord::getUserId, userId)
                .eq(CheckinRecord::getTaskId, id));
        taskMapper.deleteById(id);
    }

    /** 所有任务与签到操作都必须先校验归属，防止越权访问他人数据。 */
    public CheckinTask owned(Long userId, Long id) {
        if (id == null) {
            throw new BusinessException("NOT_FOUND_OR_FORBIDDEN", "任务不存在");
        }
        CheckinTask task = taskMapper.selectOne(new LambdaQueryWrapper<CheckinTask>()
                .eq(CheckinTask::getId, id)
                .eq(CheckinTask::getUserId, userId)
                .last("limit 1"));
        if (task == null) {
            throw new BusinessException("NOT_FOUND_OR_FORBIDDEN", "任务不存在");
        }
        return task;
    }

    public static TaskVO toVO(CheckinTask task) {
        TaskVO vo = new TaskVO();
        vo.setId(task.getId());
        vo.setTitle(task.getTitle());
        vo.setIcon(task.getIcon());
        vo.setColor(task.getColor());
        return vo;
    }
}
