package com.biyao.moses.model.adapter.impl;


import com.biyao.moses.common.enums.TemplateTypeEnum;
import com.biyao.moses.context.ByUser;
import com.biyao.moses.model.SingleCommonPrivilegeTemplageInfo;
import com.biyao.moses.model.template.TemplateInfo;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;
import com.biyao.moses.params.ProductInfo;
import org.springframework.stereotype.Component;

@Component("singleCommonPrivilege")
public class SingleCommonPrivilegeAdapter extends BaseTemplateAdapter {
    @Override
    public TemplateInfo newTemplateInfoInstance() {
        return new SingleCommonPrivilegeTemplageInfo();
    }

    @Override
    public Integer getCurTemplateDataNum(int actualSize) throws Exception {
        return TemplateTypeEnum.FEED_SINGLE_COMMONPRIVILEGE.getDataSize();
    }

    @Override
    public TemplateInfo specialDataHandle(TemplateInfo templateInfo, TotalTemplateInfo totalData, Integer curDataIndex, ByUser user) throws Exception {
        ProductInfo productInfo = productDetailUtil.preSaveProductInfo(totalData.getId(),user);

        SingleCommonPrivilegeTemplageInfo curTemplateInfo = (SingleCommonPrivilegeTemplageInfo) templateInfo;

        curTemplateInfo = (SingleCommonPrivilegeTemplageInfo) productDetailUtil.buildProductTemplate(curTemplateInfo, productInfo, curDataIndex,user, totalData);

        // 设置方图
        curTemplateInfo.setImage(productInfo.getSquarePortalImg());
        // 设置webp方图
        curTemplateInfo.setImageWebp(productInfo.getSquarePortalImgWebp());
        // 设置好评数
        if(productInfo.getPositiveComment()!=null ){
            curTemplateInfo.setGoodAppraise(productInfo.getPositiveComment());
        }
        //设置特权金抵扣价
        curTemplateInfo.setPriCouponAmount(Integer.valueOf(totalData.getPriDeductAmount()));
        return curTemplateInfo;
    }
}
