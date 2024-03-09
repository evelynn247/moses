package com.biyao.moses.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * @program: moses-parent-online
 * @description: 日期类工具类
 * @author: changxiaowei
 * @Date: 2021-12-06 14:42
 **/
public class DateUtil {

    /**
     * 相差多少天
     * @param time1
     * @param time2
     * @return
     */
    public static Long getDistanceDay(long time1, long time2) {
        long diff = time1 - time2;
        long day = diff / (24 * 60 * 60 * 1000);
        return day;
    }


    /**
     * 得到本月的第一天
     *
     * @return
     */
    public static String getMonthFirstDay() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, calendar
                .getActualMinimum(Calendar.DAY_OF_MONTH));
        java.text.SimpleDateFormat df   =   new   java.text.SimpleDateFormat( "yyyy-MM-dd");
        return df.format(calendar.getTime());
    }

    /**
     * 得到本月的最后一天
     *
     * @return
     */
    public static String getMonthLastDay() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, calendar
                .getActualMaximum(Calendar.DAY_OF_MONTH));
        java.text.SimpleDateFormat df   =   new   java.text.SimpleDateFormat( "yyyy-MM-dd");
        return df.format(calendar.getTime());
    }

    /**
     * 两个时间相差距离多少小时
     * @param time1
     * @param time2
     * @return String 返回值为：xx小时
     */
    public static String getDistanceHour(long time1, long time2) {
        long day = 0;
        long hour = 0;
        long min = 0;
        try {
            long diff ;
            if(time1<time2) {
                diff = time2 - time1;
            } else {
                diff = time1 - time2;
            }
            day = diff / (24 * 60 * 60 * 1000);
            hour = (diff / (60 * 60 * 1000));
            min = ((diff / (60 * 1000)) - day * 24 * 60 - hour * 60);
            if(min >= 55){
                hour +=1 ;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return hour + "小时";
    }
    /**
     * 获取格式化日期
     * @param format
     * @param date
     * @return
     */
    public static String getFormat(String format, Date date){
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        return sdf.format(date);
    }
    /**
     * 获取几天以后的时间
     * @param date
     * @param n
     * @return
     * @author WangWenGuang/Nov 1, 2012/11:00:26 AM
     */
    public static Date getAfterNDay(Date date, int n) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.set(Calendar.DATE, c.get(Calendar.DATE)+n);
        return c.getTime();
    }

    /**
     * 当得到两个日期相差天数。
     *
     * @param begin     第一个日期
     * @param end    第二个日期
     * @return          相差的天数
     */
    public static int selectDateDiff(long begin, long end) {
        int dif = 0;
        try {
            long fDate = (begin>end?begin:end);
            long sDate = (begin>end?end:begin);
            dif = (int) ((fDate- sDate) / 86400000);
        } catch (Exception e) {
            dif = 0;
        }
        return dif;
    }
    /**
     * 日期加（减）月份后的日期
     * @param date
     * @param month
     * @return
     */
    public static Date addMonth(Date date, int month) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.MONTH, month);
        return calendar.getTime();
    }
    /**
     *  日期加（减）天数后的日期
     * @param date
     * @param day
     * @return
     */
    public static Date addDay(Date date, int day) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.DATE, day);
        return calendar.getTime();
    }
    /**
     *  日期加（减）天数后的日期
     * @param date
     * @param day
     * @return
     */
    public static Date addDayOfMohth(Date date, int day) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.DAY_OF_MONTH, day);
        return calendar.getTime();
    }
    /**
     * 日期加（减）年数后的日期
     * @param date
     * @param year
     * @return
     */
    public static Date addYear(Date date, int year) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.YEAR, year);
        return calendar.getTime();
    }
    /**
     *  日期加（减）小时后的日期
     * @param date
     * @param hour
     * @return
     */
    public static Date addHour(Date date, int hour) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.HOUR, hour);
        return calendar.getTime();
    }

    /**
     *  日期加（减）分钟后的日期
     * @param date
     * @param minute
     * @return
     */
    public static Date addMinute(Date date, int minute) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.MINUTE, minute);
        return calendar.getTime();
    }

    /**
     *  日期加（减）秒后的日期
     * @param date
     * @param seconds
     * @return
     */
    public static Date addSeconds(Date date, int seconds) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.SECOND, seconds);
        return calendar.getTime();
    }

}
