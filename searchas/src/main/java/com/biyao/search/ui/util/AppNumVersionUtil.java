package com.biyao.search.ui.util;

import com.alibaba.dubbo.common.utils.StringUtils;
import com.biyao.search.common.enums.PlatformEnum;
import com.biyao.search.ui.enums.EnumSymbolType;
import com.biyao.search.ui.remote.request.UISearchRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import static com.biyao.search.ui.constant.CommonConstant.*;

/**
 * @author yangy
 * @ClassName: AppNumVersionUtil
 * @Description: app数字版本工具类
 * @date 18:01 2018/3/21
 */
public class AppNumVersionUtil {

    private static Logger logger = LoggerFactory.getLogger(AppNumVersionUtil.class);

    /**
     * @param baseNumVersion    基础数字版本号
     * @param compareNumVersion 比对数字版本号
     * @param symbolType        比对类型
     * @Description: 校验App当前数字版本号
     * @author yangy
     * @Date 18:29 2018/3/21
     */
    public static boolean checkAppNumVersion(Integer baseNumVersion, Integer compareNumVersion, EnumSymbolType symbolType) {
        //大于
        if (symbolType == EnumSymbolType.BIG) {
            return compareNumVersion.intValue() > baseNumVersion.intValue();
        }
        //大于或等于
        if (symbolType == EnumSymbolType.BIG_OR_EQUAL) {
            return compareNumVersion.intValue() >= baseNumVersion.intValue();
        }
        //等于
        if (symbolType == EnumSymbolType.EQUAL) {
            return compareNumVersion.intValue() == baseNumVersion.intValue();
        }
        //小于
        if (symbolType == EnumSymbolType.LESS) {
            return compareNumVersion.intValue() < baseNumVersion.intValue();
        }
        //小于或等于
        if (symbolType == EnumSymbolType.LESS_OR_EQUAL) {
            return compareNumVersion.intValue() <= baseNumVersion.intValue();
        }
        return false;
    }

    /**
     * 功能描述: <检查当前app版本是否属于指定的版本>
     *
     * @param androidBaseNumVersion android指定版本号
     * @param iosBaseNumVersion     ios指定版本号
     * @param symbolType            比对类型
     * @since: 1.0.0
     * @Author:guokaikai
     * @Date: 2018/6/12 10:33
     */
    public static boolean isSpecialAppNumVersions(String platformName, Integer appVersionNum,Integer androidBaseNumVersion, Integer iosBaseNumVersion, EnumSymbolType symbolType) {
    	boolean isSpecialAppNumVersion = false;
        Integer compareNumVersion = appVersionNum;
        Integer baseNumVersion = 0;
        String comparePlatform = platformName;
        if (!com.by.profiler.util.StringUtil.isBlank(comparePlatform)) {
            if (comparePlatform.equalsIgnoreCase("android")) {
                baseNumVersion = androidBaseNumVersion;
            } else if (comparePlatform.equalsIgnoreCase("ios")) {
                baseNumVersion = iosBaseNumVersion;
            }
        }
        if (baseNumVersion.intValue() != 0 && !com.by.profiler.util.StringUtil.isBlank(comparePlatform)) {
            //大于
            if (symbolType == EnumSymbolType.BIG) {
                isSpecialAppNumVersion = compareNumVersion.intValue() > baseNumVersion.intValue();
                return isSpecialAppNumVersion;
            }
            //大于或等于
            if (symbolType == EnumSymbolType.BIG_OR_EQUAL) {
                isSpecialAppNumVersion = compareNumVersion.intValue() >= baseNumVersion.intValue();
                return isSpecialAppNumVersion;
            }
            //等于
            if (symbolType == EnumSymbolType.EQUAL) {
                isSpecialAppNumVersion = compareNumVersion.intValue() == baseNumVersion.intValue();
                return isSpecialAppNumVersion;
            }
            //小于
            if (symbolType == EnumSymbolType.LESS) {
                isSpecialAppNumVersion = compareNumVersion.intValue() < baseNumVersion.intValue();
                return isSpecialAppNumVersion;
            }
            //小于或等于
            if (symbolType == EnumSymbolType.LESS_OR_EQUAL) {
                isSpecialAppNumVersion = compareNumVersion.intValue() <= baseNumVersion.intValue();
                return isSpecialAppNumVersion;
            }
        }
        return isSpecialAppNumVersion;
    }
    /**
     * 大运河V1.2版本
     * @param platformName
     * @param appBaseVersionNum
     * @param miniBaseVersionNum
     * @return
     */
    public static boolean isAfterDayunheVersion1_2(String platformName, Integer appBaseVersionNum, Integer miniBaseVersionNum) {
        if(PlatformEnum.IOS.getName().equals(platformName)){
            return appBaseVersionNum >= IOS_DAYUNHE_VERSION_DAYUNHE1_2;
        }else if(PlatformEnum.ANDROID.getName().equals(platformName)){
            return appBaseVersionNum >= ANDROID_DAYUNHE_VERSION_DAYUNHE1_2;
        }else if(PlatformEnum.MINI.getName().equals(platformName)){
            return miniBaseVersionNum >= MINIAPP_DAYUNHE_VERSION_DAYUNHE1_2;
        }else {
            return PlatformEnum.M.getName().equals(platformName);
        }
    }
   // 该版本号在2020年4月22日紧急上线需求中要求app版本号升级，只对当前最新版本露出一次定制商品
    private final static int IOS_DaYunHeVersion1_4 = 147;
    private final static int ANDROID_DaYunHeVersion1_4 = 286;
    private final static int MINI_DaYunHeVersion1_4 = 213;

    /**
     * 大运河V1.4版本及版本之后
     * @param request
     * @return
     */
    public static boolean isAfterDaYunHeVersion1_4(UISearchRequest request){
        if (PlatformEnum.ANDROID.equals(request.getPlatform())){
            // ANDROID
            return request.getAppVersionNum() != null && request.getAppVersionNum() >= ANDROID_DaYunHeVersion1_4;
        }else if (PlatformEnum.IOS.equals(request.getPlatform())){
            // IOS
            return request.getAppVersionNum() != null && request.getAppVersionNum() >= IOS_DaYunHeVersion1_4;
        }else if (PlatformEnum.MINI.equals(request.getPlatform())){
            // MINI
            return getMiniAppVersionNumber(request.getMiniappVersion()) >= MINI_DaYunHeVersion1_4;
        }else {
            // if equals m_web
            return PlatformEnum.M.equals(request.getPlatform());
        }
    }

    /**
     * 获取小程序数字版本号
     * @param miniAppVersionStr
     * @return
     */
    public static int getMiniAppVersionNumber(String miniAppVersionStr){
        int result = 0;
        if(StringUtils.isBlank(miniAppVersionStr)){
            return result;
        }
        try{
            result = Integer.parseInt(miniAppVersionStr.replaceAll("\\D", ""));
        }
        catch (Exception e){
            logger.error("[严重异常]小程序版本号转换异常"+e.getMessage());
        }
        return result;
    }

    // 直播标签和新品标签冲突，直播标签优先展示，不影响旧版本的新品标签展示
    private final static int IOS_LiveBroadcastVersion = 155;
    private final static int ANDROID_LiveBroadcastVersion= 294;
    /**
     * 直播标签和新品标签冲突，isShowIcon需要赋值为0的版本
     * @param platformName
     * @param appBaseVersionNum
     * @return
     */
    public static boolean isLiveBroadcastIconConflict(String platformName, Integer appBaseVersionNum) {
        if(PlatformEnum.IOS.getName().equals(platformName)){
            return appBaseVersionNum >= IOS_LiveBroadcastVersion;
        }else if(PlatformEnum.ANDROID.getName().equals(platformName)){
            return appBaseVersionNum >= ANDROID_LiveBroadcastVersion;
        }else{
            return true;
        }
    }


    //【内容策略V1.0_商品内容视频化】版本
    private final static int IOS_VIDEO_VERSION = 204;
    private final static int ANDROID_VIDEO_VERSION= 343;
    private final static int MINI_VIDEO_VERSION= 262;

    /**
     * 【内容策略V1.0_商品内容视频化】版本版本及版本之后
     * @param request
     * @return
     */
    public static boolean isAfterVideoVersion(UISearchRequest request){
        if (PlatformEnum.ANDROID.equals(request.getPlatform())){
            // ANDROID
            return request.getAppVersionNum() != null && request.getAppVersionNum() >= ANDROID_VIDEO_VERSION;
        }else if (PlatformEnum.IOS.equals(request.getPlatform())){
            // IOS
            return request.getAppVersionNum() != null && request.getAppVersionNum() >= IOS_VIDEO_VERSION;
        }else if (PlatformEnum.MINI.equals(request.getPlatform())){
            // MINI
            return getMiniAppVersionNumber(request.getMiniappVersion()) >= MINI_VIDEO_VERSION;
        }else {
            // if equals m_web
            return PlatformEnum.M.equals(request.getPlatform());
        }
    }


    /**
     * 三端支持必要造物的版本
     */
    private final static int IOS_BYCREATION_VERSION = 205;


    private final static int ANDROID_BYCREATION_VERSION = 344;


    private final static int MINI_BYCREATION_VERSION = 263;

    /**
     * 必要造物版本及版本之后
     * @param request
     * @return
     */
    public static boolean isAfterByCreationVersion(UISearchRequest request){
        if (PlatformEnum.ANDROID.equals(request.getPlatform())){
            // ANDROID
            return request.getAppVersionNum() != null && request.getAppVersionNum() >= ANDROID_BYCREATION_VERSION;
        }else if (PlatformEnum.IOS.equals(request.getPlatform())){
            // IOS
            return request.getAppVersionNum() != null && request.getAppVersionNum() >= IOS_BYCREATION_VERSION;
        }else if (PlatformEnum.MINI.equals(request.getPlatform())){
            // MINI
            return getMiniAppVersionNumber(request.getMiniappVersion()) >= MINI_BYCREATION_VERSION;
        }else {
            // if equals m_web
            return PlatformEnum.M.equals(request.getPlatform());
        }
    }

}
