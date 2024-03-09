package com.biyao.moses.util;

import com.biyao.moses.Enum.SeasonEnum;
import com.biyao.moses.bo.SourceWeightBo;
import com.biyao.moses.match.MatchItem2;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * @program: moses-parent-online
 * @description:
 * @author: changxiaowei
 * @create: 2021-09-23 16:21
 **/
@Slf4j
public class MatchUtil {


    public static int getHour() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        return calendar.get(Calendar.HOUR_OF_DAY);
    }


    public static int  getWeek() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        return calendar.get(Calendar.DAY_OF_WEEK);

    }

    public  static  float stringToFloat(String str,Float defaultValue){
        Float result =defaultValue;
        if(StringUtils.isEmpty(str)){
            return result;
        }
        try {
          result = Float.valueOf(str);
        }catch (Exception e){
            log.error("[一般异常]字符串转成浮点型数据失败，str:{},defaultValue:{},异常:{}",str,defaultValue,e);
        }
         return result;
    }

    /**
     * 0 -男  1-女    -1 - 未知
     * 转化成相反的季节   男--女  女--男
     * @param sex
     * @return
     */
   public static byte conver2adverseSex(Byte sex){
        if(sex == 1){
            return 0;
        }
        if(sex ==0){
            return 1;
        }
        return -2;
    }

    /**
     * 春季 召回 包含春季 秋季 四季的商品
     * 秋季 召回 包含春季 秋季 四季的商品
     * 夏季 召回 夏季 四季的商品
     * 冬季 召回冬季和四季的商品
     * @param userSeason
     * @return
     */
    public static List<Byte> conver2SeasonListByUserSeason(Byte userSeason){
        List<Byte> seasonList = new ArrayList<>();
        // 用户本身的季节 和 四季商品需要被召回
        seasonList.add(userSeason);
        seasonList.add(SeasonEnum.COMMON.getId().byteValue());
        // 春季需要召回秋季
        if(SeasonEnum.SPRING.getId().byteValue()==userSeason){
            seasonList.add(SeasonEnum.AUTUMN.getId().byteValue());
        }
        // 秋季需要召回春季
        if(SeasonEnum.AUTUMN.getId().byteValue()==userSeason){
            seasonList.add(SeasonEnum.SPRING.getId().byteValue());
        }
        return seasonList;
    }


    /**
     * 解析获取召回源及其权重信息，格式为：
     * source,numWeight,scoreWeight|source,numWeight,scoreWeight
     * @param sourceAndWeightStrs
     * @return
     */
    public static Map<String, SourceWeightBo> parseSourceAndWeight(String sourceAndWeightStrs,String sid) {
        Map<String, SourceWeightBo> result = new HashMap<>();
        if (StringUtils.isEmpty(sourceAndWeightStrs)) {
            return result;
        }
        String[] sourceAndWeightLayerArray = sourceAndWeightStrs.trim().split("\\|");
        //遍历sourceAndWeight配置
        for (String sourceAndWeightStr : sourceAndWeightLayerArray) {
            SourceWeightBo bo = new SourceWeightBo();
            if (StringUtils.isBlank(sourceAndWeightStr)) {
                continue;
            }
            try {
                String[] str = sourceAndWeightStr.trim().split(",");
                String source = str[0].trim();
                result.put(source, bo);
                if (str.length != 3) {
                    log.error("[严重异常][规则引擎配置]参数格式错误，取默认值。sourceAndWeight {},sid:{} ", sourceAndWeightStr,sid);
                    continue;
                }
                bo.setNumWeight(Double.valueOf(str[1].trim()));
                bo.setScoreWeight(Double.valueOf(str[2].trim()));
            } catch (Exception e) {
                log.error("[严重异常][实验配置]参数解析失败，取默认值，sourceAndWeight {},sid:{} ", sourceAndWeightStr,sid ,e);
            }
        }
       return  result;
    }

    /**
     * 过滤不感兴趣商品
     * @param list
     * @param blackList
     * @return
     */
    private void blackProductFilter(List<MatchItem2> list, Set<Long> blackList) {

        try {
            if (CollectionUtils.isEmpty(blackList)) {
                return;
            }
            Iterator<MatchItem2> iterator = list.iterator();
            while (iterator.hasNext()) {
                MatchItem2 next = iterator.next();
                Long disInterestPid=next.getProductId();
                if(Objects.isNull(disInterestPid)){
                    if (!Objects.isNull(next.getId()) && next.getId().length() < 12){
                        disInterestPid = Long.valueOf(next.getId());
                    }else {
                        continue;
                    }
                }
                if (blackList.contains(disInterestPid)) {
                    iterator.remove();
                }
            }
        } catch (Exception e) {
            log.error("[严重异常]黑名单过滤异常 ", e);
        }
    }

}
