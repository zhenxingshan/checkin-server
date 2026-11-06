package com.kevin.checkin.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kevin.checkin.common.BusinessException;
import com.kevin.checkin.entity.CheckinRecord;
import com.kevin.checkin.entity.User;
import com.kevin.checkin.mapper.CheckinRecordMapper;
import com.kevin.checkin.mapper.UserMapper;
import com.kevin.checkin.vo.CancelVO;
import com.kevin.checkin.vo.RecordVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CheckinService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    @Autowired
    private CheckinRecordMapper recordMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PointService pointService;

    @Autowired
    private TaskService taskService;

    @Value("${checkin.zone:Asia/Shanghai}")
    private String zone;

    public List<RecordVO> list(Long userId, Long taskId, String from, String to) {
        taskService.owned(userId, taskId);
        LambdaQueryWrapper<CheckinRecord> query = new LambdaQueryWrapper<CheckinRecord>()
                .eq(CheckinRecord::getUserId, userId)
                .eq(CheckinRecord::getTaskId, taskId);
        LocalDate fromDate = parseDate(from);
        LocalDate toDate = parseDate(to);
        if (fromDate != null) {
            query.ge(CheckinRecord::getCheckinDate, fromDate);
        }
        if (toDate != null) {
            query.le(CheckinRecord::getCheckinDate, toDate);
        }
        query.orderByDesc(CheckinRecord::getCheckinDate).orderByDesc(CheckinRecord::getId);
        return recordMapper.selectList(query)
                .stream().map(CheckinService::toVO).collect(Collectors.toList());
    }

    /**
     * 签到日期一律取服务端当天，前端无法伪造补签；唯一索引兜底并发重复提交。
     * 积分按「用户 + 自然日」计：一天内不管签几个任务，只有首次签到加分。
     * 用户行锁 + 事务把并发签到串行化，避免两个任务同时签到各加一次分。
     * 记录用 deleted 软删：当天被撤销过的行再次签到时直接恢复，不新增行，也不堆 id。
     */
    @Transactional(rollbackFor = Exception.class)
    public RecordVO sign(Long userId, Long taskId, String time) {
        taskService.owned(userId, taskId);
        ZoneId zoneId = ZoneId.of(zone);
        LocalDate today = LocalDate.now(zoneId);
        userMapper.lockById(userId);
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("UNAUTHORIZED", "登录已过期，请重新登录");
        }
        // 不能只查有效记录：同一天被撤销的行还在（deleted=1），再签到要恢复它而不是插新行。
        CheckinRecord exists = recordMapper.selectAnyByUserTaskDate(userId, taskId, today);
        if (exists != null && !Integer.valueOf(1).equals(exists.getDeleted())) {
            throw new BusinessException("ALREADY_SIGNED", "今天已经签到过了");
        }

        int gained = 0;
        if (!today.equals(user.getLastPointDate())) {
            int streak = user.getPointStreak() == null ? 0 : user.getPointStreak();
            boolean continued = user.getLastPointDate() != null
                    && user.getLastPointDate().plusDays(1).equals(today);
            streak = continued ? streak + 1 : 1; // 断签回到第 1 天，也就是初始积分
            gained = pointService.gainForStreak(streak);
            user.setTotalPoints((user.getTotalPoints() == null ? 0 : user.getTotalPoints()) + gained);
            user.setPointStreak(streak);
            user.setLastPointDate(today);
            user.setLastPointPoints(gained);
            userMapper.updateById(user);
        }

        CheckinRecord record;
        String checkinTime = normalizeTime(time, zoneId);
        if (exists != null) {
            // 唯一索引 (user_id, task_id, checkin_date) 不允许插第二条，只能恢复旧行。
            // points 必须刷成本次得分：旧行可能留着撤销前的分数，否则日后回滚会算错。
            int restored = recordMapper.restore(exists.getId(), checkinTime, gained);
            if (restored == 0) {
                throw new BusinessException("ALREADY_SIGNED", "今天已经签到过了");
            }
            record = exists;
            record.setDeleted(0);
            record.setCheckinTime(checkinTime);
            record.setPoints(gained);
        } else {
            record = new CheckinRecord();
            record.setUserId(userId);
            record.setTaskId(taskId);
            record.setCheckinDate(today);
            record.setCheckinTime(checkinTime);
            record.setPoints(gained);
            record.setDeleted(0);
            record.setCreatedAt(LocalDateTime.now());
            try {
                recordMapper.insert(record);
            } catch (DuplicateKeyException e) {
                // 事务回滚会把上面的积分更新一并撤销，不会出现「签到失败但加了分」。
                throw new BusinessException("ALREADY_SIGNED", "今天已经签到过了");
            }
        }
        RecordVO vo = toVO(record);
        vo.setPoints(pointService.view(user, today, gained));
        return vo;
    }

    /**
     * 撤销当天签到：日期由服务端决定，前端无法指定历史日期。
     * deleteById 在 @TableLogic 下是 UPDATE deleted=1，行保留可追溯；同日再签到会恢复这一行。
     * 一天只计一次分，删掉「持分那条」时有两种情况：
     *   1) 当天还有别的任务签到 -> 积分转给剩余最早一条，用户总积分不变；
     *   2) 当天已无任何记录 -> 回滚总积分/连续天数/计分日期，重新签到会按原档位加回来。
     */
    @Transactional(rollbackFor = Exception.class)
    public CancelVO cancelToday(Long userId, Long taskId) {
        taskService.owned(userId, taskId);
        LocalDate today = LocalDate.now(ZoneId.of(zone));
        userMapper.lockById(userId);
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("UNAUTHORIZED", "登录已过期，请重新登录");
        }
        CheckinRecord target = recordMapper.selectOne(new LambdaQueryWrapper<CheckinRecord>()
                .eq(CheckinRecord::getUserId, userId)
                .eq(CheckinRecord::getTaskId, taskId)
                .eq(CheckinRecord::getCheckinDate, today)
                .last("limit 1"));
        if (target == null) {
            throw new BusinessException("NOT_SIGNED_TODAY", "今天还没有签到记录");
        }
        int gained = target.getPoints() == null ? 0 : target.getPoints();
        recordMapper.deleteById(target.getId()); // 逻辑删除，不物理删行

        // @TableLogic 自动排除已撤销的行，只统计当天仍有效的签到。
        List<CheckinRecord> remaining = recordMapper.selectList(new LambdaQueryWrapper<CheckinRecord>()
                .eq(CheckinRecord::getUserId, userId)
                .eq(CheckinRecord::getCheckinDate, today)
                .orderByAsc(CheckinRecord::getId));
        if (gained > 0) {
            if (remaining.isEmpty()) {
                rollbackTodayPoints(user, today, gained);
            } else {
                // 当天还有别的任务签到：保持「一天恰好一条记录持分」的不变量。
                CheckinRecord carrier = remaining.get(0);
                carrier.setPoints(gained);
                recordMapper.updateById(carrier);
            }
        }

        CancelVO vo = new CancelVO();
        vo.setDate(today.format(DATE_FMT));
        vo.setRemoved(1);
        vo.setEarnedPoints(gained);
        vo.setPoints(pointService.view(user, today, 0));
        return vo;
    }

    private void rollbackTodayPoints(User user, LocalDate today, int gained) {
        if (!today.equals(user.getLastPointDate())) {
            return; // 当天本来就没计过分，保守处理不乱扣
        }
        int streak = user.getPointStreak() == null ? 0 : user.getPointStreak();
        int rolled = Math.max(0, streak - 1);
        user.setTotalPoints(Math.max(0, (user.getTotalPoints() == null ? 0 : user.getTotalPoints()) - gained));
        user.setPointStreak(rolled);
        user.setLastPointDate(rolled > 0 ? today.minusDays(1) : null);
        user.setLastPointPoints(rolled > 0 ? pointService.gainForStreak(rolled) : 0);
        userMapper.updateById(user);
    }

    private static String normalizeTime(String time, ZoneId zoneId) {
        if (StrUtil.isBlank(time)) {
            return LocalTime.now(zoneId).format(TIME_FMT);
        }
        String value = time.trim();
        return value.length() > 5 ? value.substring(0, 5) : value;
    }

    private static LocalDate parseDate(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim(), DATE_FMT);
        } catch (Exception e) {
            throw new BusinessException("INVALID_PAYLOAD", "日期格式应为 yyyy-MM-dd");
        }
    }

    public static RecordVO toVO(CheckinRecord record) {
        RecordVO vo = new RecordVO();
        vo.setDate(record.getCheckinDate() == null ? "" : record.getCheckinDate().format(DATE_FMT));
        vo.setTime(record.getCheckinTime());
        vo.setEarnedPoints(record.getPoints() == null ? 0 : record.getPoints());
        return vo;
    }
}
