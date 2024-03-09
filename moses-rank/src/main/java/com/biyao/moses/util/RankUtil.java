package com.biyao.moses.util;

import com.biyao.moses.common.constant.CommonConstants;
import com.biyao.moses.common.enums.SeasonEnum;
import com.biyao.moses.params.ProductInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * rankutil
 * rank通用方法
 */
@Slf4j
public class RankUtil {

    /**
     * 过滤掉非用户季节商品，过滤规则如下：
     * 如果用户季节为空或商品季节为空，则不过滤
     * 如果用户季节春（1），则选择适用季节为春或秋或四季的商品
     * 如果用户季节秋（4），则选择适用季节为春或秋或四季的商品
     * 如果用户季节夏（2），则选择适用季节为夏或四季的商品
     * 如果用户季节冬（8），则选择适用季节为冬或四季的商品
     *
     * @param productSeasonValue 商品季节对应的值
     * @param userSeasonValue    用户季节对应的值
     * @return
     */
    public static boolean isFilterByUserSeason(int productSeasonValue, int userSeasonValue) {
        //用户季节或商品季节为空
        if (userSeasonValue == 0 || productSeasonValue == 0) {
            return false;
        }
        //用户季节和商品季节无交集
        if ((productSeasonValue & userSeasonValue) == 0) {
            return true;
        }
        return false;
    }

    /**
     * 用户性别与商品性别相反，则返回true，认为需要被过滤
     *
     * @param productInfo
     * @param userSex
     * @return
     */
    public static boolean isFilterByUserSex(ProductInfo productInfo, Integer userSex) {
        Byte productSex = productInfo.getProductGender();
        if (productSex == null || userSex == null) {
            return false;
        }
        boolean result = false;
        String productSexStr = productSex.toString();
        String userSexStr = userSex.toString();
        if (CommonConstants.FEMALE_SEX.equals(productSexStr) && CommonConstants.MALE_SEX.equals(userSexStr)) {
            result = true;
        } else if (CommonConstants.MALE_SEX.equals(productSexStr) && CommonConstants.FEMALE_SEX.equals(userSexStr)) {
            result = true;
        }

        return result;
    }

    /**
     * 将季节（春、夏、秋、冬）转化为对应的数值
     * 如果季节为春或秋时，则对应的季节ID为春ID+秋ID
     * @param season
     * @return
     */
    public static int convertSeason2int(String season){
        int seasonValue = 0;
        if(StringUtils.isBlank(season)){
            return seasonValue;
        }
        //转化成season对应的id，如果季节为春或秋时，对应的季节ID为春ID+秋ID
        if (SeasonEnum.SPRING.getName().equals(season)) {
            seasonValue = SeasonEnum.SPRING.getId() + SeasonEnum.AUTUMN.getId();
        } else if (SeasonEnum.SUMMER.getName().equals(season)) {
            seasonValue = SeasonEnum.SUMMER.getId();
        } else if (SeasonEnum.AUTUMN.getName().equals(season)) {
            seasonValue = SeasonEnum.AUTUMN.getId() + SeasonEnum.SPRING.getId();
        } else if (SeasonEnum.WINTER.getName().equals(season)) {
            seasonValue = SeasonEnum.WINTER.getId();
        }
        return seasonValue;
    }

}
