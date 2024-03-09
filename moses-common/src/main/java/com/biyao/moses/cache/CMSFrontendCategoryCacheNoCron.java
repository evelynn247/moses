package com.biyao.moses.cache;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.biyao.cms.client.category.dto.*;
import com.biyao.cms.client.category.service.ICategoryDubboService;
import com.biyao.cms.client.common.bean.Result;
import com.biyao.cms.client.homepage.dto.HomepageTabAndCategoryDto;
import com.biyao.cms.client.homepage.service.IHomePageDubboService;
import com.biyao.moses.common.enums.ActCategoryEnum;
import com.biyao.moses.common.enums.PlatformEnum;
import com.biyao.moses.model.template.FrontendCategory;
import com.biyao.moses.model.template.FrontendCategoryForAct;
import com.biyao.moses.model.template.JumpContent;
import com.biyao.moses.util.HttpClientUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class CMSFrontendCategoryCacheNoCron {

//    private List<FrontendCategory> frontendCategoryList = new ArrayList<>();

    /**
     * 首页导航前台类目
     */
    private Map<String, List<FrontendCategory>> homeFrontendCategoryMap = new HashMap<>();
    /**
     * 全部前台类目
     */
    private Map<String, List<FrontendCategory>> allFrontendCategoryMap = new HashMap<>();

    /**
     * 活动类目页前台类目  key1:端id     key2: 场景id
     */
    private Map<String, Map<String,List<FrontendCategoryForAct>>> actFrontendCategoryMap = new HashMap<>();

    private Map<String, Set<String>> customFcateMap = new HashMap<>();

    private final static String CMS_ALL_FRONTEND_CATEGORY_URL = "http://cmsapi.biyao.com/category/getAllFrontendCategory";

    @Resource
    IHomePageDubboService homePageDubboService;
    @Resource
    ICategoryDubboService categoryDubboService;

//    public List<FrontendCategory> getHomeCategoryList(){
//        if (frontendCategoryList != null && frontendCategoryList.size() > 0){
//            return frontendCategoryList;
//        }
//
//        return null;
//    }

    /**
     * 获取首页tab前台类目数据
     * @param siteId
     * @return
     */
    public List<FrontendCategory> getHomeCategoryList(String siteId){
        if (homeFrontendCategoryMap != null && homeFrontendCategoryMap.containsKey(siteId)){
            return homeFrontendCategoryMap.get(siteId);
        }

        return null;
    }
    /**
     * 获取全部前台类目
     * @param siteId
     * @return
     */
    public List<FrontendCategory> getAllCategoryList(String siteId){
        if (allFrontendCategoryMap != null && allFrontendCategoryMap.containsKey(siteId)){
            return allFrontendCategoryMap.get(siteId);
        }

        return null;
    }


    /**
     * 获取全部前台类目
     * @param siteId
     * @return
     */
    public List<FrontendCategoryForAct> getActCategoryList(String siteId, String sceneId) {
        if (actFrontendCategoryMap != null && actFrontendCategoryMap.get(siteId) != null) {
            return actFrontendCategoryMap.get(siteId).get(sceneId);
        }
        return null;
    }

    /**
     * 获取该类目是否是定制类目（包括定制一级类目、二级类目、三级类目）
     * @param fcate
     * @return
     */
    public boolean isCustomFcate(String siteId, String fcate){
        if(customFcateMap == null || customFcateMap.size() == 0 || StringUtils.isBlank(fcate)){
            return false;
        }

        if(!customFcateMap.containsKey(siteId)){
            return false;
        }

        Set<String> customFcateSet = customFcateMap.get(siteId);
        if(customFcateSet.contains(fcate)){
            return true;
        }
        return false;
    }

    protected void init() {
        refreshCMSFrontendCategoryCache();
    }

    /**
     * 刷新缓存内容
     * 20190422 由于各端的前台类目需要过滤，所以缓存3份
     */
    protected void refreshCMSFrontendCategoryCache(){
        refreshHomeFrontendCategory();
        refreshAllFrontendCategory();
        //刷新活动页类目
        refreshAllActFrontendCategory();
    }
    /**
     * 将CMS的Dto转为moses的Dto
     * @param frontendCategoryPropertyDto
     * @return
     */
    private FrontendCategory convertToFrontendCategoryDto(FrontendCategoryDto frontendCategoryPropertyDto){
        FrontendCategory frontendCategory = new FrontendCategory();
        frontendCategory.setCategoryId(frontendCategoryPropertyDto.getCategoryId());
        frontendCategory.setCategoryName(frontendCategoryPropertyDto.getCategoryName());
        frontendCategory.setCategoryLevel(frontendCategoryPropertyDto.getCategoryLevel());
        frontendCategory.setCategoryType(frontendCategoryPropertyDto.getCategoryType());
        frontendCategory.setImageUrl(frontendCategoryPropertyDto.getImageUrl());
        frontendCategory.setWebpImageUrl(frontendCategoryPropertyDto.getWebpImageUrl());
        frontendCategory.setTagIdList(frontendCategoryPropertyDto.getTagIdList());
        //log.error("frontendCategoryJSON：{}",JSONObject.toJSONString(frontendCategory));
        List<FrontendCategory> subFrontendCategoryList = new ArrayList<FrontendCategory>();
        if (frontendCategoryPropertyDto.getSubCategoryDtoList() != null) {
            subFrontendCategoryList = frontendCategoryPropertyDto.getSubCategoryDtoList().stream().map(it -> {
                return convertToFrontendCategoryDto(it);
            }).collect(Collectors.toList());

        }
        frontendCategory.setSubCategoryList(subFrontendCategoryList);

        List<FrontendCategory> thirdFrontendCategoryList = new ArrayList<>();
        if (frontendCategoryPropertyDto.getSubCategoryDtoList() != null) {
            thirdFrontendCategoryList = frontendCategoryPropertyDto.getThirdCategoryDtoList().stream().map(it -> {
                return convertToFrontendCategoryDto(it);
            }).collect(Collectors.toList());
        }
        frontendCategory.setThirdCategoryDtoList(thirdFrontendCategoryList);
        frontendCategory.setBackendCategoryIdList(frontendCategoryPropertyDto.getBackendCategoryIdList());
        return frontendCategory;
    }

    /**
     * 刷新首页导航前台类目
     */
    protected void refreshHomeFrontendCategory(){
        log.info("[任务进度][前台类目]获取首页前台类目信息开始");
        long start = System.currentTimeMillis();
        Map<String, List<FrontendCategory>> tmpFrontendCategoryMap = new HashMap<>();
        try {
            for (PlatformEnum platformEnum : PlatformEnum.values()) {
                CategoryParamDto categoryParamDto = new CategoryParamDto();
                Integer cmsPlatform=getCmsPlataformNum(platformEnum);
                categoryParamDto.setPlatform(cmsPlatform);
                categoryParamDto.setToggroupCategory(false); // 是否是支持一起拼类目
                categoryParamDto.setCaller("moses"); // 调用方写死为moses
                Result<HomepageTabAndCategoryDto> homepageTabAndCategoryDtoResult = homePageDubboService.getHomepageTabAndCategory(categoryParamDto);
                if (homepageTabAndCategoryDtoResult.isSuccess() && homepageTabAndCategoryDtoResult.getData() != null) {
                    List<FrontendCategory> tmpFrontendCategoryList = new ArrayList<>();
                    List<FrontendCategoryPropertyDto> cmsFrontendCategoryDtoList = homepageTabAndCategoryDtoResult.getData().getCategoryList();
                    if (cmsFrontendCategoryDtoList != null && cmsFrontendCategoryDtoList.size() > 0) {
                        tmpFrontendCategoryList = cmsFrontendCategoryDtoList.stream().map(it -> {
                            return convertToFrontendCategoryDto(it);
                        }).collect(Collectors.toList());
                    }
                    if (tmpFrontendCategoryList.size() > 0){
                        tmpFrontendCategoryMap.put(platformEnum.getNum().toString(), tmpFrontendCategoryList);
                    }
                }
            }
        }catch (Exception e){
            log.error("[严重异常][前台类目]从CMS获取首页前台类目信息失败，", e);
            tmpFrontendCategoryMap.clear();
        }
        if (tmpFrontendCategoryMap.size() > 0) {
            this.homeFrontendCategoryMap = tmpFrontendCategoryMap;
            log.info("[任务进度][前台类目]从CMS获取首页前台类目信息结束，耗时{}ms",System.currentTimeMillis()-start);
        }else{
            log.error("[严重异常][前台类目]从CMS获取首页前台类目信息结束，信息为空");
        }
    }

    /**
     * 刷新所有活动前台类目
     */
    protected void  refreshAllActFrontendCategory(){
        log.info("[任务进度][前台类目]获取全部活动前台类目信息开始");
        long start = System.currentTimeMillis();
        // 初始化结果集
        Map<String, Map<String,List<FrontendCategoryForAct>>> tmpActFrontendCategoryMap = new HashMap<>();
        try {
            for (PlatformEnum platformEnum : PlatformEnum.values()){
                for (ActCategoryEnum value : ActCategoryEnum.values()) {
                    if(ActCategoryEnum.isMatchSiteId(value,platformEnum.getNum().toString())){
                        List<FrontendCategoryForAct> recommendResult = null;
                        Result<List<FrontendSecondCategoryDto>> result =
                                categoryDubboService.queryMakeProductSecondCategory(buildActivityCategoryParamDto(platformEnum, value));
                        if(Objects.nonNull(result) && result.success && !CollectionUtils.isEmpty(result.getData())){
                            recommendResult =
                                    result.getData().stream().map(this::converTOFrontendCategoryForAct).collect(Collectors.toList());
                        }
                        Map<String,List<FrontendCategoryForAct>> map = new HashMap<>();
                        map.put(value.getSceneId(),recommendResult);
                        tmpActFrontendCategoryMap.put(platformEnum.getNum().toString(),map);
                    }
                }
            }
        }catch (Exception e){
            log.error("[严重异常]活动类目页前台类目查询失败，异常信息：",e);
            //异常情况下清空 tmpActFrontendCategoryMap
            tmpActFrontendCategoryMap.clear();
        }
        //非异常情况更新缓存
        if(!tmpActFrontendCategoryMap.isEmpty()){
            actFrontendCategoryMap =tmpActFrontendCategoryMap;
        }
    }
    /**
     * @Des
     * @Param [dto]将cms返回的活动类目信息转化成推荐的数据结构
     * @return com.biyao.moses.model.template.FrontendCategoryForAct
     * @Author changxiaowei
     * @Date  2022/2/14
     */
    private FrontendCategoryForAct converTOFrontendCategoryForAct(FrontendSecondCategoryDto dto){
        FrontendCategoryForAct frontendCategoryForAct = new FrontendCategoryForAct();
        if(Objects.nonNull(dto)){
            frontendCategoryForAct.setCategoryId(dto.getSecondCategoryId());
            frontendCategoryForAct.setCategoryLevel(2);
            frontendCategoryForAct.setCategoryName(dto.getSecondCategoryName());
            frontendCategoryForAct.setThirdCategoryId(dto.getBackendCategoryIdList());
            frontendCategoryForAct.setTagId(dto.getTagIdList());
        }
        return  frontendCategoryForAct;
    }

    /**
     * @Des
     * @Param 构建cms接口参数
     * @return com.biyao.cms.client.category.dto.ActivityCategoryParamDto
     * @Author changxiaowei
     * @Date  2022/2/14
     */
    private ActivityCategoryParamDto buildActivityCategoryParamDto(PlatformEnum platformEnum,ActCategoryEnum actCategoryEnum){
        ActivityCategoryParamDto paramDto = new ActivityCategoryParamDto();
        paramDto.setCaller("moses");
        paramDto.setPlatform(getCmsPlataformNum(platformEnum));
        paramDto.setActivityType(getCmsActivityType(actCategoryEnum));
        return paramDto;
    }

    /**
     * 根据推荐的场景id 获取cms 对应的场景值
     * @param actCategoryEnum
     * @return
     */
    private  Integer getCmsActivityType(ActCategoryEnum actCategoryEnum){
        // 必要造物场景  cms 对应为 20
        if(ActCategoryEnum.BYZW.equals(actCategoryEnum)){
            return 20;
        }
        return null;
    }
    /**
     * 刷新全部前台类目
     */
    protected void refreshAllFrontendCategory(){
        log.info("[任务进度][前台类目]获取全部前台类目信息开始");
        long start = System.currentTimeMillis();
        Map<String, List<FrontendCategory>> tmpAllFrontendCategoryMap = new HashMap<>();
        Map<String, Set<String>> tmpCustomFcateMap = new HashMap<>();
        boolean isException = false;
        try{
            for (PlatformEnum platformEnum : PlatformEnum.values()){
                Integer cmsPlatform=getCmsPlataformNum(platformEnum);
                String url = CMS_ALL_FRONTEND_CATEGORY_URL + "?platform=" + cmsPlatform;
                // 网关之前调cmsapi的超时用的是5s
                String jsonStr = HttpClientUtil.sendGetRequest(url, 5000);
                JSONObject resultJson = JSON.parseObject(jsonStr);
                if ("1".equals(resultJson.getString("success"))){
                    JSONArray categoryArr = resultJson.getObject("data", JSONArray.class);
                    List<FrontendCategory> frontendCategoryList = categoryArr.stream().map(it -> {
                        return convertJsonToFrontendCategory(it.toString());
                    }).collect(Collectors.toList());
                    if (frontendCategoryList.size() > 0){
                        tmpAllFrontendCategoryMap.put(platformEnum.getNum().toString(), frontendCategoryList);
                        refreshcustomFcateCache(frontendCategoryList, platformEnum.getNum().toString(), tmpCustomFcateMap);
                    }
                }

            }
        }catch (Exception e){
            log.error("[严重异常][前台类目]从CMSAPI获取全部前台类目失败，", e);
            tmpAllFrontendCategoryMap.clear();
            isException = true;
        }

        if(!isException) {
            this.customFcateMap = tmpCustomFcateMap;
        }

        if (tmpAllFrontendCategoryMap.size() > 0){
            this.allFrontendCategoryMap = tmpAllFrontendCategoryMap;
            log.info("[任务进度][前台类目]从CMSAPI获取全部前台类目信息结束，耗时{}ms",System.currentTimeMillis()-start);
        }else{
            log.error("[严重异常][前台类目]从CMSAPI获取全部前台类目信息结束，信息为空");
        }
    }

    /**
     * 刷新定制类目信息
     * @param frontendCategoryList
     */
    private void refreshcustomFcateCache(List<FrontendCategory> frontendCategoryList, String siteId, Map<String, Set<String>> tmpCustomFcateMap){
        if(StringUtils.isBlank(siteId)){
            log.error("[严重异常][前台类目]刷新定制类目信息入参失败， siteId = {}", siteId);
            return;
        }

        Set<String> tmpPlatformCustomFcateSet;
        if(tmpCustomFcateMap.containsKey(siteId)){
            tmpPlatformCustomFcateSet = tmpCustomFcateMap.get(siteId);
        }else{
            tmpPlatformCustomFcateSet = new HashSet<>();
            tmpCustomFcateMap.put(siteId, tmpPlatformCustomFcateSet);
        }

        if(CollectionUtils.isEmpty(frontendCategoryList)){
            return;
        }

        try{
            for(FrontendCategory frontendCategory : frontendCategoryList){
                if(frontendCategory == null || frontendCategory.getCategoryType() == null || frontendCategory.getCategoryId() == null){
                    continue;
                }

                if("1".equals(frontendCategory.getCategoryType().toString())){
                    tmpPlatformCustomFcateSet.add(frontendCategory.getCategoryId().toString());
                    List<FrontendCategory> subCategoryList = frontendCategory.getSubCategoryList();
                    if(CollectionUtils.isNotEmpty(subCategoryList)){
                        for(FrontendCategory frontendCategory1 : subCategoryList){
                            if(frontendCategory1 == null || frontendCategory1.getCategoryId() == null){
                                continue;
                            }
                            tmpPlatformCustomFcateSet.add(frontendCategory1.getCategoryId().toString());
                            List<FrontendCategory> subCategoryList1 = frontendCategory1.getSubCategoryList();
                            if(CollectionUtils.isNotEmpty(subCategoryList1)){
                                for(FrontendCategory frontendCategory2 : subCategoryList1){
                                    if(frontendCategory2 == null || frontendCategory2.getCategoryId() == null){
                                        continue;
                                    }
                                    tmpPlatformCustomFcateSet.add(frontendCategory2.getCategoryId().toString());
                                }
                            }
                        }
                    }
                }
            }
        }catch(Exception e){
            log.error("[严重异常][前台类目]刷新定制类目信息异常 siteId = {}， frontendCategoryList = {}，",siteId, JSON.toJSONString(frontendCategoryList), e);
        }
    }

    /**
     * 将JSON
     * @param jsonObjStr
     * @return
     */
    private FrontendCategory convertJsonToFrontendCategory(String jsonObjStr){
        FrontendCategory frontendCategory = new FrontendCategory();
        JSONObject jsonObj = JSON.parseObject(jsonObjStr);
        frontendCategory.setCategoryId(jsonObj.getInteger("categoryId"));
        frontendCategory.setCategoryName(jsonObj.getString("categoryName"));
        frontendCategory.setCategoryType(jsonObj.getInteger("categoryType"));
        frontendCategory.setImageUrl(jsonObj.getString("imageUrl"));
        frontendCategory.setWebpImageUrl(jsonObj.getString("webpImageUrl"));
        frontendCategory.setJumpType(jsonObj.getInteger("jumpType"));
        JSONObject jumpObj = jsonObj.getJSONObject("jumpContent");
        if (jumpObj != null) {
            JumpContent jumpContent = new JumpContent();
            jumpContent.setJumpParam(jumpObj.getString("jumpParam"));
            jumpContent.setJumpPageTitle(jumpObj.getString("jumpPageTitle"));
            frontendCategory.setJumpContent(jumpContent);
        }
        List<FrontendCategory> subFrontendCategoryList = new ArrayList<FrontendCategory>();
        if (jsonObj.getJSONArray("subCategoryList") != null) {
            subFrontendCategoryList = jsonObj.getJSONArray("subCategoryList").stream().map(it -> {
                return convertJsonToFrontendCategory(it.toString());
            }).collect(Collectors.toList());

        }
        frontendCategory.setSubCategoryList(subFrontendCategoryList);

        return frontendCategory;
    }

    public Integer getCmsPlataformNum(PlatformEnum platformEnum){

        Integer cmsPlatform = 3; //  2:PC, 3:M, 4:MINI_APP,5:IOS 6:ANDROID
        if (platformEnum.equals(PlatformEnum.MINI)){
            cmsPlatform = 4;
        }else if (platformEnum.equals(PlatformEnum.PC)){
            cmsPlatform = 2;
        }else if (platformEnum.equals(PlatformEnum.ANDROID)){
            cmsPlatform = 6;
        }else if(platformEnum.equals(platformEnum.IOS)){
            cmsPlatform = 5;
        }
        return cmsPlatform;
    }

}
