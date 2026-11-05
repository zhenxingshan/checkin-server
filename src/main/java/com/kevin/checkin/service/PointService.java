package com.kevin.checkin.service;

import com.kevin.checkin.entity.User;
import com.kevin.checkin.vo.PointVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * 积分规则：连续签到按档位递增、断签回到初始积分、单次得分封顶。
 * 规则参数都在 application.yml 的 checkin.points 下，改配置即可调档位，不用改代码。
 */
@Service
public class PointService {

    @Value("${checkin.points.base:1}")
    private int base;

    @Value("${checkin.points.step-days:3}")
    private int stepDays;

    @Value("${checkin.points.step-points:1}")
    private int stepPoints;

    @Value("${checkin.points.max:5}")
    private int max;

    /** 连续第 streak 天签到可得积分：每 stepDays 天升一档，最高不超过 max。 */
    public int gainForStreak(int streak) {
        int safeStreak = Math.max(1, streak);
        int step = Math.max(1, stepDays);
        long value = (long) Math.max(0, base)
                + ((long) (safeStreak - 1) / step) * Math.max(0, stepPoints);
        return (int) Math.min(value, Math.max(0, max));
    }

    /** 下一次签到时的连续天数：今天已计分则算明天，昨天计过分则 +1，其余算断签回到 1。 */
    public int nextStreak(User user, LocalDate today) {
        int current = user.getPointStreak() == null ? 0 : user.getPointStreak();
        LocalDate last = user.getLastPointDate();
        if (last == null) {
            return 1;
        }
        if (last.equals(today) || last.plusDays(1).equals(today)) {
            return current + 1;
        }
        return 1;
    }

    /** 组装积分总览；gained 传本次签到得分，资料查询时传 0。 */
    public PointVO view(User user, LocalDate today, int gained) {
        PointVO vo = new PointVO();
        vo.setTotal(user.getTotalPoints() == null ? 0 : user.getTotalPoints());
        vo.setToday(today.equals(user.getLastPointDate())
                ? (user.getLastPointPoints() == null ? 0 : user.getLastPointPoints()) : 0);
        vo.setStreak(user.getPointStreak() == null ? 0 : user.getPointStreak());
        vo.setGained(gained);
        vo.setMax(Math.max(0, max));
        vo.setBase(Math.max(0, base));
        vo.setStepDays(Math.max(1, stepDays));
        vo.setStepPoints(Math.max(0, stepPoints));
        int next = nextStreak(user, today);
        int per = gainForStreak(next);
        vo.setPerCheckin(per);
        vo.setNextStageDays(per >= vo.getMax() ? 0 : daysToNextStage(next));
        return vo;
    }

    /** 距离下一档还需连续签到的次数。 */
    private int daysToNextStage(int nextStreak) {
        int step = Math.max(1, stepDays);
        int stageIndex = (nextStreak - 1) / step;
        return (stageIndex + 1) * step + 1 - nextStreak;
    }
}
