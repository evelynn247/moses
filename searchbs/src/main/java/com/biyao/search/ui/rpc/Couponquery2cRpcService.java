package com.biyao.search.ui.rpc;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.biyao.couponquery2c.dubbo.client.common.ICouponCommonQueryClientService;
import com.biyao.couponquery2c.dubbo.dto.allowance.AllowanceInfoDTO;
import com.biyao.couponquery2c.dubbo.dto.common.CouponAssetsDTO;
import com.biyao.couponquery2c.dubbo.param.common.QueryAssetsByUidAndCouponTypesParam;
import com.biyao.search.ui.constant.CommonConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import com.biyao.bsrd.common.client.model.result.Result;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/**
 * @description 商品视频标识优化
 * @project 【内容策略V1.0_商品内容视频化】
 * @author 张志敏
 * @date 2022-02-17
 */

@Service
@Slf4j
public class Couponquery2cRpcService {

    @Autowired
    ICouponCommonQueryClientService iCouponCommonQueryClientService;


    /**
     * 根据用户uid获取用户总津贴数
     * @param uid
     * @return
     */
    public BigDecimal queryUserTotalAllowance(Long uid) {
        /**
         1、入参非空校验：caller（调用方服务）、userId（用户id）、couponTypeList（券类型列表：1通用特权金、2新手特权金、3津贴、4立减金、5参团卡 、6新手专享优惠凭证），如果为空，则日志记录，返回0；
         2、调用卡券系统的查询用户总津贴数接口，返回用户津贴信息；
         3、如果返回为空，或者服务异常，日志记录并处理（处理逻辑由客户端和产品商讨决定:返回用户总津贴数为null);
         4、如果数据返回正常，将所有的津贴的可用金额叠加得到用户的总津贴数；
         5、最后将用户的总津贴数返回
         */

        BigDecimal totalAllowance = BigDecimal.ZERO;
        Result<CouponAssetsDTO> couponAssetsDTOResult = null;
        if (uid == null) {
            log.error("[严重异常][dubbo]查询用户总津贴数时,参数异常 uid为空！");
            return totalAllowance;
        }

        //封装参数
        QueryAssetsByUidAndCouponTypesParam param = new QueryAssetsByUidAndCouponTypesParam();
        param.setCaller(CommonConstant.SYSTEM_NAME);//调用方服务
        param.setUserId(uid);//用户id
        param.setCouponTypeList(Arrays.asList(CommonConstant.USER_COUPON_TYPE_ALLOWANCE_CONS));//券类型

        try {
            //查询用户账户中的可使用的资产信息
            couponAssetsDTOResult = iCouponCommonQueryClientService.queryAssetsByUidAndCouponTypes(param);
        } catch (Exception e) {
            log.error("[严重异常][dubbo]调用Couponquery2c服务查询用户总津贴数(iCouponCommonQueryClientService#queryAssetsByUidAndCouponTypes)时,发生异常", e);
            return totalAllowance;
        }
        if (null == couponAssetsDTOResult || !couponAssetsDTOResult.isSuccess() || couponAssetsDTOResult.getObj() == null ) {
            log.error("[严重异常][dubbo]调用Couponquery2c服务查询用户总津贴数(iCouponCommonQueryClientService#queryAssetsByUidAndCouponTypes)时,发生异常，异常信息：{}", JSONObject.toJSONString(couponAssetsDTOResult));
            return totalAllowance;
        }

        try {
            for (AllowanceInfoDTO allowanceInfoDTO : couponAssetsDTOResult.getObj().getAllowances()) {
                totalAllowance = allowanceInfoDTO.getAvailableNum().add(totalAllowance);
            }
        } catch (Exception e) {
            log.error("[严重异常]获取用户总津贴数时,出现异常",e);
            return BigDecimal.ZERO;
        }

        return totalAllowance;

    }
}
