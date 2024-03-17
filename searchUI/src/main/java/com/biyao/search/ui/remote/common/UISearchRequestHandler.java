package com.biyao.search.ui.remote.common;

import com.alibaba.fastjson.JSON;
import com.biyao.cms.client.material.dto.MaterialElementBaseDTO;
import com.biyao.gba.dubbo.client.common.Result;
import com.biyao.gba.dubbo.client.toggroup.activity.dto.ActivityRuleDto;
import com.biyao.gba.dubbo.client.toggroup.activity.service.ITogGroupActivityToCService;
import com.biyao.mac.client.redbag.shop.privilegebag.dto.ShowPrivilegeLogoResultDto;
import com.biyao.mac.client.redbag.shop.privilegebag.service.IShopRedBagPrivilegeBagService;
import com.biyao.search.common.enums.PlatformEnum;
import com.biyao.search.ui.enums.UserTypeEnum;
import com.biyao.search.ui.enums.VisitorTypeEnum;
import com.biyao.search.ui.remote.request.UISearchRequest;
import com.biyao.search.ui.util.CmsUtil;
import com.biyao.search.ui.util.ConstantUtil;
import com.biyao.uc.service.UcServerService;
import com.biyao.upc.dubbo.client.business.toc.IBusinessTocDubboService;
import com.biyao.upc.dubbo.dto.VisitorInfoDTO;
import com.biyao.upc.dubbo.param.business.VisitorInfoParam;
import com.google.common.collect.Lists;
import com.uc.domain.params.UserRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class UISearchRequestHandler {

    @Autowired
    IShopRedBagPrivilegeBagService shopRedBagPrivilegeBagService;

    @Autowired
    IBusinessTocDubboService iBusinessTocDubboService;

    @Autowired
    private ITogGroupActivityToCService togGroupActivityToCService;

    @Autowired
    UcServerService ucServerService;


    private final Logger logger = LoggerFactory.getLogger(getClass());
    private static final long MATERIAL_ID = 10280101L;

    public void handleRequest(UISearchRequest uiSearchRequest){

        uiSearchRequest.setPrivilegeLogo(isUserHasPrivilege(uiSearchRequest.getUid(),uiSearchRequest.getPlatform().getName()));
        setIsToGroupAndIsNewUser(uiSearchRequest);
        setUserSize(uiSearchRequest,uiSearchRequest.getUid());

    }

    private void setUserSize(UISearchRequest uiSearchRequest, Integer uid) {
        //获取用户个性化尺码
        Map<Long, List<String>> userSizeMap = new HashMap<>();
        if (uid != null) {
            try {
                UserRequest userRequest = new UserRequest();
                userRequest.setUid(uid.toString());
                userRequest.setCaller("search");
                userRequest.setFields(Lists.newArrayList("personalSize"));

                Map<Long, String> tempMap = ucServerService.query(userRequest).getData().getPersonalSize();
                if (tempMap != null) {
                    for (Map.Entry<Long, String> temp : tempMap.entrySet()) {
                        userSizeMap.put(temp.getKey(), Arrays.asList(temp.getValue().split(",")));
                    }
                }
            } catch (Exception e) {
                logger.info("uc获取用户个性化尺码失败：" + e.getMessage());
            }
        }
        uiSearchRequest.setUserSizeMap(userSizeMap);
    }

    /**
     * 用户是否拥有特权金
     */
    private ShowPrivilegeLogoResultDto isUserHasPrivilege(Integer uid, String platform) {
        // 显示支持特权金
        com.biyao.mac.client.common.bean.Result<ShowPrivilegeLogoResultDto> privilegeRes = null;
        // 未登录用户、M站、PC 不显示支持特权金
        if (null != uid && uid > 0 && !PlatformEnum.M.getName().equals(platform)
                && !PlatformEnum.PC.getName().equals(platform)) {
            try {
                // zhaiweixi 20180822 改调新的方法
                privilegeRes = shopRedBagPrivilegeBagService.isShowPrivilegeLogo(Long.valueOf(uid.toString()));
            } catch (Exception e) {
                logger.error("[严重异常]调用shopRedBagPrivilegeBagService发生异常，uid={}, platform={}", uid, platform, e);
                return null;
            }
        }
        if (privilegeRes != null) {
            return privilegeRes.getData();
        } else {
            return null;
        }
    }


    /**
     * 获取用户身份标识及一起拼标识
     */
    private void setIsToGroupAndIsNewUser(UISearchRequest uiSearchRequest){

        try{
            VisitorInfoParam visitorInfoParam = new VisitorInfoParam();
            Integer userId = uiSearchRequest.getUid();
            if(userId!=null&&userId>0){
                visitorInfoParam.setCustomerId(userId.longValue());
            }
            visitorInfoParam.setUuid(uiSearchRequest.getUuid());
            visitorInfoParam.setCallSysName(ConstantUtil.SERVICE_NAME);
            com.biyao.bsrd.common.client.model.result.Result<VisitorInfoDTO> resultOfVisitorInfo = null;
            resultOfVisitorInfo = iBusinessTocDubboService.getVisitorInfo(visitorInfoParam);
            if (resultOfVisitorInfo.isSuccess()) {
                VisitorInfoDTO visitorInfo = resultOfVisitorInfo.getObj();
                uiSearchRequest.setIsJumpTogroup(isToGroup(visitorInfoParam, visitorInfo));
                uiSearchRequest.setIsNewUser(visitorInfo.isMatch());
            }
        }catch(Exception e){
            uiSearchRequest.setIsJumpTogroup(false);
            uiSearchRequest.setIsNewUser(false);
            logger.error("[严重异常]获取用户信息异常,request={}", JSON.toJSONString(uiSearchRequest), e);
        }
    }

    /**
     * 新访客转化专项<br/>
     1.一起拼总开关为开启状态<br/>
     a)若【一起拼站内用户身份展示配置】配置为：新访客，则站内老访客在对应的页面不支持跳转到一起拼编辑器，跳转到普通编辑器；<br/>
     b)若【一起拼站内用户身份展示配置】配置为：老访客，则站内新访客在对应的页面不支持跳转到一起拼相编辑器，跳转到普通编辑器；<br/>
     c)若【一起拼站内用户身份展示配置】配置为：全部新用户，则站内所有新访客&老访客支持跳转到一起拼编辑器（线上逻辑）；<br/>
     d)开关状态获取失败，默认为全部新用户；<br/>
     e)老客的展示逻辑不受此开关影响；<br/>
     * @param visitorInfoParam
     * @param visitorInfo
     * @return
     */
    private boolean isToGroup(VisitorInfoParam visitorInfoParam, VisitorInfoDTO visitorInfo){

        if(visitorInfo.getVisitorType() == null){
            //老客，本次不调整
            return false;
        }

        //一起拼总开关
        Result<ActivityRuleDto> platformActivityRule = togGroupActivityToCService.getPlatformActivityRule();
        ActivityRuleDto activityRuleDto = platformActivityRule.getData();
        if(null !=activityRuleDto){
            Integer activitySwitch = activityRuleDto.getActivitySwitch();
            if(activitySwitch==null ||activitySwitch == 0){
                return false;
            }
        }

        //获取策略
        UserTypeEnum byCode = UserTypeEnum.ALL_NEW_USER;
        MaterialElementBaseDTO queryMaterial = CmsUtil.queryMaterial(MATERIAL_ID);
        if(null!=queryMaterial){
            Object value = queryMaterial.getValue();
            if(!org.springframework.util.StringUtils.isEmpty(value)){
                int parseInt = Integer.parseInt(value.toString());
                byCode = UserTypeEnum.getByCode(parseInt);
            }
        }

        VisitorTypeEnum visitorTypeEnum = VisitorTypeEnum.NEW_VISITOR;
        //如果UUID为空，默认未新访客，否则取实际值
        if(!org.springframework.util.StringUtils.isEmpty(visitorInfoParam.getUuid())){
            visitorTypeEnum = VisitorTypeEnum.getByCode(visitorInfo.getVisitorType());
        }
        //用户身份
        if(null != visitorInfo){
            // 新访客身份
            if(VisitorTypeEnum.NEW_VISITOR.getCode().equals(visitorTypeEnum.getCode())){
                if(UserTypeEnum.NEW_VISITOR.getCode().equals(byCode.getCode()) || UserTypeEnum.ALL_NEW_USER.getCode().equals(byCode.getCode())){
                    // 新访客策略 || 全部新用户
                    return true;
                }
            }else if(VisitorTypeEnum.OLD_VISITOR.getCode().equals(visitorTypeEnum.getCode())){
                // 老访客身份
                if(UserTypeEnum.OLD_VISITOR.getCode().equals(byCode.getCode()) || UserTypeEnum.ALL_NEW_USER.getCode().equals(byCode.getCode())){
                    //老访客策略 ||全部新用户
                    return true;
                }
            }
        }

        return false;
    }

}
