package com.kevin.checkin.common;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 新用户默认昵称：形容词 + 名词随机组合，例如「早起的小树苗」「元气满满的月亮」。
 * 只在首次登录落库时调用一次；用户随时可以在「我的 - 设置」里改成自己的昵称。
 * 纯 JDK 实现，无外部依赖，便于单独验证组合数量与长度。
 */
public final class NicknameGenerator {

    private static final String[] PREFIX = {
            "早起的", "爱阅读的", "认真打卡的", "爱运动的", "慢热的", "元气满满的",
            "安静努力的", "坚持的", "闪闪发光的", "贪睡的", "专注的", "温柔的",
            "快乐的", "慢慢变好的", "日日精进的", "迎着晨光的", "爱喝水的", "不拖延的",
            "心怀星光的", "认真生活的", "轻装上阵的", "每天前进的",
    };

    private static final String[] SUFFIX = {
            "小树苗", "月亮", "小太阳", "小星星", "云朵", "小狐狸", "小鲸鱼",
            "小狮子", "小奶猫", "蒲公英", "小火箭", "小书虫", "小闹钟", "小种子",
            "小海豚", "小鹿", "向日葵", "小灯塔", "小风铃", "小宇宙", "小蜗牛",
            "小松果", "小行星", "小茶杯", "小毛豆", "小铃兰", "小橡果", "小蘑菇",
    };

    private NicknameGenerator() {
    }

    public static String random() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        return PREFIX[random.nextInt(PREFIX.length)] + SUFFIX[random.nextInt(SUFFIX.length)];
    }

    /** 可用组合总数，用于评估撞名概率；昵称本身不要求全局唯一。 */
    public static int combinations() {
        return PREFIX.length * SUFFIX.length;
    }
}
