package com.biyao.moses.featureplatform.common;

import static com.biyao.moses.featureplatform.constant.FeatureConstant.*;

/**
 * @program: moses-parent-online
 * @description:
 * @author: zzm
 * @create: 2022-03-22 15:00
 **/
public class IsEsFieldExist {


    /**
     * 判断用户特征索引中是否存在该字段
     * @param field
     * @return
     */
    public static Boolean isHaveFieldInUser(String field){
        return USER_FEATURE_FIELD_LIST_CONST.contains(field);
    }

    /**
     * 判断商品特征索引中是否存在该字段
     * @param field
     * @return
     */
    public static Boolean isHaveFieldInProduct(String field){
        return PRODUCT_FEATURE_FIELD_LIST_CONST.contains(field);
    }

}
