package com.today.step.util;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @Description: 时间工具类（时间格式转换方便类）
 */
public class DateUtils {

    private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat();

    private static final DecimalFormat decimalFormat=new DecimalFormat("00");

    public static long getDateMillis(String dateString, String pattern) {
        long millionSeconds = 0;
        SIMPLE_DATE_FORMAT.applyPattern(pattern);
        try {
            millionSeconds = SIMPLE_DATE_FORMAT.parse(dateString).getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }// 毫秒

        return millionSeconds;
    }

    /**
     * 格式化输入的millis
     *
     * @param millis
     * @param pattern yyyy-MM-dd HH:mm:ss E
     * @return
     */
    public static String dateFormat(long millis, String pattern) {
        SIMPLE_DATE_FORMAT.applyPattern(pattern);
        Date date = new Date(millis);
        String dateString = SIMPLE_DATE_FORMAT.format(date);
        return dateString;
    }


    public static String getRunTime(int seconds) {
        int second = seconds % 60;
        int minutes = seconds / 60 % 60;
        int hour = seconds / 60 / 60;
        return decimalFormat.format(hour)+":"+decimalFormat.format(minutes)+":"+decimalFormat.format(second);
    }

}
