package com.kevin.checkin.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kevin.checkin.common.BusinessException;
import com.kevin.checkin.dto.TaskDTO;
import com.kevin.checkin.dto.TaskOrderDTO;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
                        .orderByAsc(CheckinTask::getSortOrder)
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
        // 一次查出全部任务：既校验数量上限，又算出新任务应排在第几位。
        List<CheckinTask> existing = taskMapper.selectList(new LambdaQueryWrapper<CheckinTask>()
                .eq(CheckinTask::getUserId, userId));
        if (existing.size() >= taskLimit) {
            throw new BusinessException("TASK_LIMIT", "最多只能创建 " + taskLimit + " 个签到任务");
        }
        int nextOrder = 0;
        for (CheckinTask item : existing) {
            int order = item.getSortOrder() == null ? 0 : item.getSortOrder();
            if (order >= nextOrder) nextOrder = order + 1;
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
        task.setSortOrder(nextOrder);
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

    /**
     * 整列重排：前端提交完整的 id 顺序，后端校验无遗漏、无重复且全部属于当前用户后，
     * 按数组下标写入 sort_order（0..n-1）。一次请求完成，避免多次交换产生中间态。
     */
    @Transactional(rollbackFor = Exception.class)
    public List<TaskVO> reorder(Long userId, TaskOrderDTO dto) {
        List<Long> ids = dto == null ? null : dto.getIds();
        if (ids == null || ids.isEmpty() || ids.contains(null)) {
            throw new BusinessException("INVALID_PAYLOAD", "排序数据不能为空");
        }
        userMapper.lockById(userId);
        List<CheckinTask> all = taskMapper.selectList(new LambdaQueryWrapper<CheckinTask>()
                .eq(CheckinTask::getUserId, userId));
        Set<Long> ownedIds = new HashSet<Long>();
        for (CheckinTask task : all) {
            ownedIds.add(task.getId());
        }
        if (ids.size() != all.size() || new HashSet<Long>(ids).size() != ids.size()
                || !ownedIds.containsAll(ids)) {
            throw new BusinessException("INVALID_PAYLOAD", "任务列表已变化，请刷新后重试");
        }
        for (int i = 0; i < ids.size(); i += 1) {
            CheckinTask patch = new CheckinTask();
            patch.setId(ids.get(i));
            patch.setSortOrder(i);
            taskMapper.updateById(patch);
        }
        return list(userId);
    }

    /** 删除任务时同事务把该任务下的签到记录一并逻辑删除，避免留下仍然有效的记录。 */
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
