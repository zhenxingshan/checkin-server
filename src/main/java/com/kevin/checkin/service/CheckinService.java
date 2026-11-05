package com.kevin.checkin.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kevin.checkin.common.BusinessException;
import com.kevin.checkin.entity.CheckinRecord;
import com.kevin.checkin.entity.User;
import com.kevin.checkin.mapper.CheckinRecordMapper;
import com.kevin.checkin.mapper.UserMapper;
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
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

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
        CheckinRecord exists = recordMapper.selectOne(new LambdaQueryWrapper<CheckinRecord>()
                .eq(CheckinRecord::getUserId, userId)
                .eq(CheckinRecord::getTaskId, taskId)
                .eq(CheckinRecord::getCheckinDate, today)
                .last("limit 1"));
        if (exists != null) {
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

        CheckinRecord record = new CheckinRecord();
        record.setUserId(userId);
        record.setTaskId(taskId);
        record.setCheckinDate(today);
        record.setCheckinTime(normalizeTime(time, zoneId));
        record.setPoints(gained);
        record.setCreatedAt(LocalDateTime.now());
        try {
            recordMapper.insert(record);
        } catch (DuplicateKeyException e) {
            // 事务回滚会把上面的积分更新一并撤销，不会出现「签到失败但加了分」。
            throw new BusinessException("ALREADY_SIGNED", "今天已经签到过了");
        }
        RecordVO vo = toVO(record);
        vo.setPoints(pointService.view(user, today, gained));
        return vo;
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
        vo.setId(record.getId());
        vo.setTaskId(record.getTaskId());
        vo.setDate(record.getCheckinDate() == null ? "" : record.getCheckinDate().format(DATE_FMT));
        vo.setTime(record.getCheckinTime());
        vo.setEarnedPoints(record.getPoints() == null ? 0 : record.getPoints());
        vo.setCreatedAt(record.getCreatedAt() == null ? "" : record.getCreatedAt().format(DATETIME_FMT));
        return vo;
    }
}
