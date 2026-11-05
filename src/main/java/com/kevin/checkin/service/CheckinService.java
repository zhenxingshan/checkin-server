package com.kevin.checkin.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kevin.checkin.common.BusinessException;
import com.kevin.checkin.entity.CheckinRecord;
import com.kevin.checkin.mapper.CheckinRecordMapper;
import com.kevin.checkin.vo.RecordVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

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

    /** 签到日期一律取服务端当天，前端无法伪造补签；唯一索引兜底并发重复提交。 */
    public RecordVO sign(Long userId, Long taskId, String time) {
        taskService.owned(userId, taskId);
        ZoneId zoneId = ZoneId.of(zone);
        LocalDate today = LocalDate.now(zoneId);
        CheckinRecord exists = recordMapper.selectOne(new LambdaQueryWrapper<CheckinRecord>()
                .eq(CheckinRecord::getUserId, userId)
                .eq(CheckinRecord::getTaskId, taskId)
                .eq(CheckinRecord::getCheckinDate, today)
                .last("limit 1"));
        if (exists != null) {
            throw new BusinessException("ALREADY_SIGNED", "今天已经签到过了");
        }
        CheckinRecord record = new CheckinRecord();
        record.setUserId(userId);
        record.setTaskId(taskId);
        record.setCheckinDate(today);
        record.setCheckinTime(normalizeTime(time, zoneId));
        record.setCreatedAt(LocalDateTime.now());
        try {
            recordMapper.insert(record);
        } catch (DuplicateKeyException e) {
            throw new BusinessException("ALREADY_SIGNED", "今天已经签到过了");
        }
        return toVO(record);
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
        vo.setCreatedAt(record.getCreatedAt() == null ? "" : record.getCreatedAt().format(DATETIME_FMT));
        return vo;
    }
}
