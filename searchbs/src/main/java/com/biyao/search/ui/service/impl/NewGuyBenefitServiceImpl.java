package com.biyao.search.ui.service.impl;

import com.alibaba.fastjson.JSON;
import com.biyao.search.ui.constant.CmsSwitch;
import com.biyao.search.ui.enums.DisplaySwitchEnum;
import com.biyao.search.ui.enums.VisitorTypeEnum;
import com.biyao.search.ui.model.NewGuyAccessStatus;
import com.biyao.search.ui.service.NewGuyBenefitService;
import com.biyao.search.ui.util.CmsUtil;
import com.biyao.search.ui.util.ConstantUtil;
import com.biyao.upc.dubbo.client.business.toc.IBusinessTocDubboService;
import com.biyao.upc.dubbo.dto.VisitorInfoDTO;
import com.biyao.upc.dubbo.param.business.VisitorInfoParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @Auther: sunbaokui
 * @Date: 2019/4/3 11:57
 * @Description:
 */
@Service
public class NewGuyBenefitServiceImpl implements NewGuyBenefitService {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private IBusinessTocDubboService iBusinessTocDubboService;
   

    //推荐服务查询条数限制
    public static final int QUERY_LIMIT_SIZE = 50;

    @Override
    public NewGuyAccessStatus getNewGuyAccessStatus(Integer uid, String uuid) {
        //设置默认值为老客且不能访问新手专享
        NewGuyAccessStatus newGuyAccessStatus = new NewGuyAccessStatus(Boolean.FALSE, Boolean.FALSE);
        Long uidLong=null;
        if(uid != null && uid > 0) {
        	uidLong=Long.valueOf(uid);
        }
        try {
            //从upc获取用户及访客状态
            VisitorInfoDTO visitorInfo = getVisitorInfo(uidLong, uuid);
            //如果是新客
            if (visitorInfo.isMatch()) {
                newGuyAccessStatus.setNewUser(Boolean.TRUE);
                DisplaySwitchEnum displaySwitchEnum = getDisplaySwitch();
                VisitorTypeEnum visitorTypeEnum = VisitorTypeEnum.getByCode(visitorInfo.getVisitorType());
                newGuyAccessStatus.setCanAccessNewGuyBenefit(displaySwitchEnum.isCanAccess(visitorTypeEnum));
            }
        } catch (Exception e) {
            logger.error("[严重异常]获取新手专享访问状态出现异常,uid={}, uuid={}",uid,uuid, e);
        }
        return newGuyAccessStatus;
    }
    
    private DisplaySwitchEnum getDisplaySwitch() {
        DisplaySwitchEnum displaySwitchEnum = null;
        try {
            String status = CmsUtil.getMaterialValue(CmsSwitch.DISPLAY_SWITCH_CODE);
            displaySwitchEnum = DisplaySwitchEnum.getByCode(status);
        } catch (Exception e) {
            logger.error("[严重异常]调用cms获取开关出现异常,id={}",CmsSwitch.DISPLAY_SWITCH_CODE, e);
        }
        if (displaySwitchEnum == null) {
            displaySwitchEnum = DisplaySwitchEnum.ALL_NEW_USER;
        }
        return displaySwitchEnum;
    }
    
    
    public VisitorInfoDTO getVisitorInfo(Long customerId, String uuid){
        VisitorInfoParam visitorInfoParam = new VisitorInfoParam();
        visitorInfoParam.setCustomerId(customerId);
        visitorInfoParam.setUuid(uuid);
        visitorInfoParam.setCallSysName(ConstantUtil.SERVICE_NAME);
        com.biyao.bsrd.common.client.model.result.Result<VisitorInfoDTO> result = null;
        try {
            result = iBusinessTocDubboService.getVisitorInfo(visitorInfoParam);
        } catch (Exception e) {
        	logger.error("[严重异常]rpc调用upc服务iBusinessTocDubboService.getVisitorInfo异常,参数visitorInfoParam={} ",JSON.toJSONString(visitorInfoParam), e);
        }
        if (result != null && result.isSuccess()) {
            return result.getObj();
        } else {
            logger.error("[严重异常]rpc调用upc服务iBusinessTocDubboService.getVisitorInfo异常,参数visitorInfoParam= " + JSON.toJSONString(visitorInfoParam) + " ;结果result：" + JSON.toJSONString(result));
            /*throw new BiyaoBizException(ErrorCode.DUBBO_ERROR);*/
        }
		return null;
    }

}
