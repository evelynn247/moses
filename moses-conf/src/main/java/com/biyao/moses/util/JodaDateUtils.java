package com.biyao.moses.util;

import java.sql.Timestamp;
import java.text.ParseException;
import java.util.Date;
import java.util.TimeZone;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 日期处理工具类
 * 
 * @author monkey
 */
public class JodaDateUtils {
	private static  Logger logger =  LoggerFactory.getLogger(JodaDateUtils.class);
    /** 定义常量 **/
	/**
	 * yyyy-MM-dd HH:mm:ss
	 */
    public static final String DATE_FULL_STR = "yyyy-MM-dd HH:mm:ss";
    /**
     * yyyy-MM-dd kk:mm:ss.SSS
     */
    public static final String DATE_LONG_STR = "yyyy-MM-dd kk:mm:ss.SSS";
    /**
     * yyyy-MM-dd
     */
    public static final String DATE_SMALL_STR = "yyyy-MM-dd";
    /**
     * yyyy/MM/dd
     */
    public static final String DATE_FORMART_STR = "yyyy/MM/dd";
    /**
     * yyyy-MM
     */
    public static final String DATE_MONTH = "yyyy-MM";
    /**
     * HH:mm:ss
     */
    public static final String DATE_TIME = "HH:mm:ss";
    /**
     * yyMMddHHmmss
     */
    public static final String DATE_KEY_STR = "yyMMddHHmmss";
    /**
     * yyyyMMddHHmmss
     */
    public static final String DATE_All_KEY_STR = "yyyyMMddHHmmss";
    /**
     * yyyyMMdd
     */
    public static final String DATE_SIMPLE_STR = "yyyyMMdd";
    /**
     * yyyyMMddHHmm
     */
    public static final String DATE_SIMPLE_MS_STR = "yyyyMMddHHmm";
    /**
     * MM
     */
    public static final String DATE_MM_STR = "MM";
    /**
     * dd
     */
    public static final String DATE_DD_STR = "dd";
  
    
    
    /**
     * 根据 pattern 格式化  dateTime
     * @param dateTime
     * @param pattern
     * @return
     */
    public static String dateToString( DateTime dateTime,String pattern){
    	org.joda.time.format.DateTimeFormatter forPattern = DateTimeFormat.forPattern(pattern);
    	return dateTime.toString(forPattern);
    }
    
    /**
     * 给指定的日期加上(减去)月份
     * 
     * @param date
     * @param pattern
     * @param num
     * @return
     */
    public static String addMoth(Date date, String pattern, int num) {
    	DateTime dateTime = new DateTime(date).plusMonths(num);
    	return dateToString(dateTime, pattern);
    }

    /**
     * 给制定的时间加上(减去)天
     * 
     * @param date
     * @param pattern
     * @param num
     * @return
     */
    public static String addDay(Date date, String pattern, int num) {
    	DateTime dateTime = new DateTime(date).plusDays(num);
    	return dateToString(dateTime, pattern);
    }

    /**
     * 给制定的时间加上(减去)分钟
     * 
     * @param date
     * @return
     */
    public static String addMinute(Date date, String pattern, int num) {
    	DateTime dateTime = new DateTime(date).plusMinutes(num);
    	return dateToString(dateTime, pattern);
    }
    /**
	 * 给指定的时间加上(减去)小时
	 * @param date
	 * @param pattern
	 * @param num
	 * @return
	 */
	public static String addHour(Date date, String pattern, int num) {
		DateTime dateTime = new DateTime(date).plusHours(num);
		return dateToString(dateTime, pattern);
	}
    /**
     * 获取系统当前时间
     * pattern 
     * @return
     */
    public static String getNowTime() {
    	return dateToString(DateTime.now(), DATE_FULL_STR);
    }

    /**
     * 获取系统当前时间(指定返回类型)
     * @return
     */
    public static String getNowTime(String format) {
    	return dateToString(DateTime.now(), format);
    }

    /**
     * 使用预设格式提取字符串日期
     * 
     * @param date
     *            日期字符串
     * @return
     */
    public static Date parse(String date) {
        return parse(date, DATE_FULL_STR);
    }

    /**
     * 指定指定日期字符串
     * 
     * @param date
     * @param pattern
     * @return
     */
    public static Date parse(String date, String pattern) {
    	org.joda.time.format.DateTimeFormatter fomatter = DateTimeFormat.forPattern(pattern);
    	return DateTime.parse(date, fomatter).toDate();
    }

    /**
     * 两个时间比较(时间戳比较)
     * @param
     * @return
     */
    public static int compareDateWithNow(long date) {
        long now = dateToUnixTimestamp();
        if (date > now) {
            return 1;
        } else if (date < now) {
            return -1;
        } else {
            return 0;
        }
    }

    /**
     * 将指定的日期转换成Unix时间戳
     * 
     * @param date
     *            需要转换的日期 yyyy-MM-dd HH:mm:ss
     * @return long 时间戳
     */
    public static long dateToUnixTimestamp(String date) {
        long timestamp = parse(date).getTime();
        return timestamp;
    }

    /*
     * 将指定的日期转换成Unix时间戳
     * 
     * @param date 需要转换的日期 yyyy-MM-dd
     * 
     * @return long 时间戳
     */
    public static long dateToUnixTimestamp(String date, String dateFormat) {
        long timestamp = 0;
        timestamp = parse(date,dateFormat).getTime();
        return timestamp;
    }

    /**
     * 将当前日期转换成Unix时间戳
     * 
     * @return long 时间戳
     */
    public static long dateToUnixTimestamp() {
    	return System.currentTimeMillis();
    }

    /**
     * 将Unix时间戳转换成日期
     * 
     * @param timestamp
     *            时间戳
     * @return String 日期字符串
     */
    public static String unixTimestampToDate(long timestamp) {
        return unixTimestampToDate(timestamp,DATE_FULL_STR);
    }

    /**
     * 将Unix时间戳转换成日期
     * 
     * @param timestamp
     *            时间戳
     * @return String 日期字符串
     */
    public static String unixTimestampToDate(long timestamp, String dateFormat) {
    	
        DateTimeZone zone = DateTimeZone.forTimeZone(TimeZone.getTimeZone("GMT+8"));
        return dateToString(new DateTime(timestamp,zone), dateFormat);
    }
    
    
    /**
     * sql.date 转字符串
     * 
     * @param time
     * @return
     */
    public static String timestamp2Str(Timestamp time) {
        if (null != time) {
            return unixTimestampToDate(time.getTime(), DATE_FULL_STR);
        }
        return null;
    }

    /**
     * sql.date 转字符串
     * 
     * @param time
     * @return
     */
    public static String timestamp2Str(Timestamp time, String format) {
        if (null != time) {
        	return unixTimestampToDate(time.getTime(),format);
        }
        return null;
    }

    /**
     * 字符串转sql.date时间
     * 
     * @param str
     *            yyyy-MM-dd HH:mm:ss 格式时间
     * @return
     */
    public static Timestamp str2Timestamp(String str) {
        if (null != str && !"".equals(str)) {
            Date date = parse(str, DATE_FULL_STR);
            return new Timestamp(date.getTime());
        }
        return null;
    }

    /**
     * 将Timestamp类型对象转换为Date类型对象
     * 
     * @param timestamp
     *            Timestamp类型对象
     * @return Date类型对象
     */
    public static Date timestampToDate(Timestamp timestamp) {
        Date date = timestamp;
        return date;
    }

    /**
     * 将Date类型对象转换为Timestamp类型对象
     * 
     * @param date
     *            Date类型对象
     * @return Timestamp类型对象
     */
    public static Timestamp dateToTimestamp(Date date) {
        return new Timestamp(date.getTime());
    }

    /**
     * 获取当前时间的Timestamp
     * 
     * @param date
     *            Date类型对象
     * @return Timestamp类型对象
     */
    public static Timestamp getCurTimestamp() {
        return new Timestamp(System.currentTimeMillis());
    }

    /**
     * 字符串转sql.date时间
     * 
     * @param str
     *            yyyy-MM-dd 格式时间
     * @return
     */
    public static java.sql.Date getSqlDate(String str) {
        if (null != str && !"".equals(str)) {
            Date date = parse(str, DATE_SMALL_STR);
            return new java.sql.Date(date.getTime());
        }
        return null;
    }
    
    
    /**
     * 获取两个时间的差值，分钟，取整
     * 
     * @author ducongcong
     * @createDate 2015年12月29日
     * @updateDate
     * @param end
     * @param start
     * @return
     * @throws Exception
     */
    public static int getTimeDiff(Timestamp end, Timestamp start) throws ParseException {
        long intDiff = diff(timestamp2Str(start), timestamp2Str(end), DATE_FULL_STR)/(1000 * 60);
        return (int) intDiff;
    }

    
    /**
     * 获取两个时间的差值，天，取整
     * 
     * @author LiuPengYuan
     * @date 2017年3月28日
     * @param end
     * @param start
     * @return
     * @throws Exception
     */
    public static int getTimeDiffDay(Timestamp end, Timestamp start) throws ParseException {
        long intDiff = diff(timestamp2Str(start), timestamp2Str(end), DATE_FULL_STR)/(1000 * 60 * 60 * 24);
        return (int) intDiff;
    }

    /**
     * 两个时间之间的天数差
     * 
     * @param startDate
     * @param endDate
     * @return
     * @author ducongcong
     * @createDate 2016年12月13日
     * @updateDate
     */
    public static int getDateDiff(String startDate, String endDate) throws ParseException  {
        long intDiff = diff(startDate, endDate, DATE_FULL_STR)/(1000 * 60 * 60 * 24);
        return (int) intDiff;
    }
    /**
     * 两日期的间隔天数
     * 
     * @param strDate1
     *            需要进行计较的日期1(yyyy-MM-dd)
     * @param strDate2
     *            需要进行计较的日期2(yyyy-MM-dd)
     * @return 间隔天数（int）
     * @throws ParseException
     */
    public static int diffDay(String strDateBegin, String strDateEnd) throws ParseException {
        long intDiff = diff(strDateBegin, strDateEnd, DATE_SMALL_STR)/(1000*60*60*24);
        return (int) intDiff;
    }
    
    /**
     * 两日期的间隔天数
     * 
     * @param strDate1
     *            需要进行计较的日期1(yyyy-MM-dd HH:mm:ss)
     * @param strDate2
     *            需要进行计较的日期2(yyyy-MM-dd HH:mm:ss)
     * @return 间隔天数（int）
     * @throws ParseException
     */
    public static int diffDay(String strDateBegin, String strDateEnd, String pattern) throws ParseException {
        long intDiff = diff(strDateBegin, strDateEnd, pattern)/(1000*60*60*24);
        return (int) intDiff;
    }

    /**
     * 两日期的间隔天数
     * 
     * @param strDate1
     *            需要进行计较的日期1(yyyy-MM-dd HH:mm:ss)
     * @param strDate2
     *            需要进行计较的日期2(yyyy-MM-dd HH:mm:ss)
     * @return 间隔秒数（int）
     * @throws ParseException
     */
    public static long diffSecond(String strDateBegin, String strDateEnd) throws ParseException {
        return diffSecond(strDateBegin, strDateEnd, DATE_FULL_STR);
    }

    /**
     * 两日期的间隔天数
     * 
     * @param strDate1
     *            需要进行计较的日期1
     * @param strDate2
     *            需要进行计较的日期2
     * @param formart
     *            指定日期格式
     * @return 间隔秒数（int）
     * @throws ParseException
     */
    public static long diffSecond(String strDateBegin, String strDateEnd, String formart) throws ParseException {
        return diff(strDateBegin, strDateEnd, formart)/1000;
    }
    
    /**
     * 相差分钟数
     * @param strDateBegin
     * @param strDateEnd
     * @return
     * @throws ParseException
     */
    public static long diffMinute(String strDateBegin, String strDateEnd) throws ParseException {
    	return diff(strDateBegin, strDateEnd, DATE_FULL_STR)/60000;
    }
    /**
     * 两个时间之间的时间差（毫秒级）
     * @param strDateBegin
     * @param strDateEnd
     * @param formart
     * @return
     * @throws ParseException
     * @updateDate
     */
    public static long diff(String strDateBegin, String strDateEnd, String formart)throws ParseException{
    	 org.joda.time.format.DateTimeFormatter fomatter = DateTimeFormat.forPattern(formart);
    	 Date dateBegin = DateTime.parse(strDateBegin, fomatter).toDate();
    	 Date dateEnd =  DateTime.parse(strDateEnd, fomatter).toDate();
         return dateEnd.getTime() - dateBegin.getTime();
    }

    /**
     * @desc: 获取时间差
     * @param startTime
     * @param endTime
     * @return long
     */
    public static long diffSecond(long startTime, long endTime) {
        long milliSencods = endTime - startTime;
        long intDiff = milliSencods / 1000;
        return intDiff;
    }
    
    /**
     * 获取字符串格式日期的天
     * 
     * @param dateStr
     *            yyyy-MM-dd HH:mm:ss
     * @param format
     *            格式 yyyy-MM-dd HH:mm:ss
     * @return
     */
    public static int getCurDayFromStr(String dateStr, String format) {
    	org.joda.time.format.DateTimeFormatter fomatter = DateTimeFormat.forPattern(format);
    	DateTime dateTime = DateTime.parse(dateStr, fomatter);
    	return dateTime.getDayOfMonth();
    }

    /**
     * 返回yyyy-MM-dd HH:mm:ss格式的字符串日期
     * 
     * @param date
     * @return
     */
    public static String formateDateStr(Date date) {
        return formateDateStr(date,DATE_FULL_STR);
    }

    /**
     * 指定格式返回日期
     * 
     * @param date
     * @param farmat
     * @return
     */
    public static String formateDateStr(Date date, String farmat) {
    	return dateToString(new DateTime(date), farmat);
    }



    /**
     * @desc: 获取最近的时间
     * @param time1
     * @param time2
     * @return Timestamp
     */
    public static Timestamp getLastTime(Timestamp time1, Timestamp time2) {
        if (time1 != null && time2 != null) {
            if (time1.after(time2)) {
                return time1;
            } else {
                return time2;
            }
        } else {
            if (time1 != null) {
                return time1;
            } else if (time2 != null) {
                return time2;
            }
        }
        return null;
    }
    
    
    /**
     * 日期比较
     * 
     * @param strDate1
     *            需要进行计较的日期1(yyyy-MM-dd)
     * @param strDate2
     *            需要进行计较的日期2(yyyy-MM-dd)
     * @return 比较的结果（int） -1：strDate1 < strDate2 0：strDate1 = strDate2 1：strDate1 > strDate2
     * @throws ParseException
     */
    public static int compareDate(String strDate1, String strDate2) throws ParseException {
    	org.joda.time.format.DateTimeFormatter fomatter = DateTimeFormat.forPattern(DATE_SMALL_STR);
    	DateTime date1 = DateTime.parse(strDate1, fomatter);
    	DateTime date2 = DateTime.parse(strDate2, fomatter);
    	return date1.compareTo(date2);
    }
    
    /**
     * 日期比较
     * 
     * @param date1
     *            需要进行计较的日期1
     * @param date2
     *            需要进行计较的日期2
     * @return 比较的结果（int） -1：strDate1 < strDate2 0：strDate1 = strDate2 1：strDate1 > strDate2
     * @throws ParseException
     */
    public static int compareDate(Date date1, Date date2){
    	DateTime dt1 = new DateTime(date1);
    	DateTime dt2 =  new DateTime(date2);
    	return dt1.compareTo(dt2);
    }
    
    /**
     * 获取系统当前日期和时间，格式为yyyy-MM-dd HH:mm:ss
     * 兼容用
     * @return 返回计算后的日期时间（String）
     */
    public static String getCurrentDateTime() {
        return formateDateStr(new Date(), DATE_FULL_STR);
    }
    /**
     * 得到当前日期
     * @return
     */
    public static Date getCurrentDate(){
    	return DateTime.now().toDate();
    }
    /**
     * 获取凌晨date
     * yyyy-MM-dd 00:00:00
     * @param pattern
     * @return
     */
    public static Date getDate(String str) {
    	Date date = DateTime.now().dayOfWeek().roundFloorCopy().toDate();
        return date;
    }
    
    public static void main(String[] args) {
    	String date1 = "20180317";
    	String date2 = "201803161902";
    	Date parse = JodaDateUtils.parse(date1, JodaDateUtils.DATE_SIMPLE_STR);
    	Date parse2 = JodaDateUtils.parse(date2, JodaDateUtils.DATE_SIMPLE_MS_STR);
		int compareDate = JodaDateUtils.compareDate(parse, parse2);
		System.out.println(compareDate);
		System.out.println(JodaDateUtils.compareDate(JodaDateUtils.getCurrentDate(), JodaDateUtils.parse("20181108", JodaDateUtils.DATE_SIMPLE_STR)));
		System.out.println(JodaDateUtils.compareDate(JodaDateUtils.getCurrentDate(), JodaDateUtils.parse("20181109", JodaDateUtils.DATE_SIMPLE_STR)));
	}
}