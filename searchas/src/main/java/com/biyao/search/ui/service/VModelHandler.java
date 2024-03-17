package com.biyao.search.ui.service;

import com.alibaba.dubbo.common.utils.StringUtils;
import com.biyao.css.dubbo.dto.sync.SyncCelebrityDto;
import com.biyao.mag.dubbo.client.tob.dto.WorksShopDto;
import com.biyao.search.ui.cache.SyncVDataCache;
import com.biyao.search.ui.cache.SyncVStoreCache;
import com.biyao.search.ui.enums.VModelTypeEnum;
import com.biyao.search.ui.model.VModel;
import com.biyao.search.ui.remote.common.CommonService;
import com.biyao.search.ui.remote.request.UISearchRequest;
import com.biyao.search.ui.util.RouterUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * @program: search-ui
 * @description VModel数据处理类
 * @author: xiafang
 * @create: 2019-10-16 11:11
 **/
@Component
@Slf4j
public class VModelHandler {

    @Autowired
    SyncVDataCache syncVDataCache;

    @Autowired
    CommonService commonService;

    @Autowired
    SyncVStoreCache syncVStoreCache;

    private static final String V_IDETIFY_DEFAULT_TEXT = "认证用户";
    private static final String V_DESC_DEFAULT_TEXT = "从现在起，你就是我的人了~";

    /**
     * 根据搜索词匹配大V、 企业定制用户、平台号
     * add：新增平台号露出需求，优先级高于大V和企业定制用户 udpateDate：20200518
     *
     * @param request search接口请求
     * @return 返回匹配的卡片信息,展示顺序：平台号、大V、企业定制用户　
     */
    public List<VModel> getVmodelList(UISearchRequest request) {
        List<VModel> vModelList = new ArrayList<>();
        //200422 新增梦工厂流量分流控制
        if(commonService.checkFlowLimit(request)){
        	
        	//0、平台号 
        	VModel platformModel = getVmodelByType(request, VModelTypeEnum.PLATFORM.getCode());
        	if(platformModel != null) {
        		vModelList.add(platformModel);
        	}
        	
            //1、搜索匹配的大V用户,有顺序要求
            VModel vmodel = getVmodelByType(request, VModelTypeEnum.DAV.getCode());
            if (vmodel != null) {
                vModelList.add(vmodel);
            }
            //2、搜索匹配的企业定制用户
            VModel enterpriseModel = getVmodelByType(request, VModelTypeEnum.ENTERPRISE.getCode());
            if (enterpriseModel != null) {
                vModelList.add(enterpriseModel);
            }
        }
        return vModelList;
    }

    /**
     * 大运河V1.1版本只有大V数据，通过该方法搜索匹配的大V
     *
     * @param request search接口请求
     * @return 大V卡片信息
     */
    public VModel getVmodel(UISearchRequest request) {
        //200422 新增梦工厂流量分流控制
        if(commonService.checkFlowLimit(request)){
            VModel vmodel = getVmodelByType(request, VModelTypeEnum.DAV.getCode());
            return vmodel;
        }
        return null;
    }

    /**
     * 根据用户类型匹配对应的用户，并转换为前端展示的卡片信息
     *
     * @param request search接口请求
     * @param vtype   卡片类型
     * @return 前端展示的卡片信息
     */
    private VModel getVmodelByType(UISearchRequest request, String vtype) {
        SyncCelebrityDto syncCelebrityDto = matchCelebrityDto(request.getOriginalQuery(), vtype);
        return transfer2VModel(syncCelebrityDto, request, vtype);
    }
 
    /**
     * 用户搜索词和（大V昵称及运营配置搜索词及同义词）的匹配方法
     *
     * @param query 为用户搜索词，用户搜索词来自接口/newSearch的入参UISearchRequest的searchParam字段，处理后赋值给query字段
     * @return 返回匹配卡片信息
     */
    private SyncCelebrityDto matchCelebrityDto(String query, String vtype) {
        if (StringUtils.isEmpty(query)) {
            return null;
        }
        Map<String, Long> searchWordSynonmsMap = new HashMap<>();
        Map<Long, SyncCelebrityDto> syncCelebrityDtoMap = new HashMap<>();

        if (VModelTypeEnum.DAV.getCode().equals(vtype)) {
            //key为大V昵称和搜索词及同义词vSearchWordSynonm，value为大VId
            searchWordSynonmsMap = syncVDataCache.getCelebritySearchWordMap();
            syncCelebrityDtoMap = syncVDataCache.getCelebrityDtoMap();
        } else if (VModelTypeEnum.ENTERPRISE.getCode().equals(vtype)) {
            //key为企业定制用户昵称和搜索词及同义词vSearchWordSynonm，value为大VId
            searchWordSynonmsMap = syncVDataCache.getEnterpriseSearchWordMap();
            syncCelebrityDtoMap = syncVDataCache.getEnterpriseDtoMap();
        } else if(VModelTypeEnum.PLATFORM.getCode().equals(vtype)) {
        	//平台号
        	searchWordSynonmsMap = syncVDataCache.getPlatformSearchWordMap();
            syncCelebrityDtoMap = syncVDataCache.getPlatformDtoMap();
        }
        try {
        	query = query.toUpperCase();
            //1）完全匹配
            if (searchWordSynonmsMap.containsKey(query)) {
                return syncCelebrityDtoMap.get(searchWordSynonmsMap.get(query));
            }
            //2)部分匹配，包括正想匹配和逆向匹配
            Set<SyncCelebrityDto> syncCelebrityDtoSet = new HashSet<>();
            for (String searchWord : searchWordSynonmsMap.keySet()) {
                if (query.contains(searchWord) || searchWord.contains(query)) {
                    syncCelebrityDtoSet.add(syncCelebrityDtoMap.get(searchWordSynonmsMap.get(searchWord)));
                }
            }
            if (syncCelebrityDtoSet.size() > 0) {
                // 模糊匹配到多条大V信息时,对当前发布商品数进行降序排序；
                // 发布商品数相同时按当前动态数降序排序；
                // 当前动态数相同时,按照大V账号创建时间排序,取创建时间最近的
                List<SyncCelebrityDto> syncCelebrityDtoList = new ArrayList<>(syncCelebrityDtoSet);
                Collections.sort(syncCelebrityDtoList, new Comparator<SyncCelebrityDto>() {
                    @Override
                    public int compare(SyncCelebrityDto o1, SyncCelebrityDto o2) {
                        Long releaseGoodsNum1 = (o1.getReleaseGoodsNum() == null) ? 0 : o1.getReleaseGoodsNum();
                        Long releaseGoodsNum2 = (o2.getReleaseGoodsNum() == null) ? 0 : o2.getReleaseGoodsNum();
                        if (releaseGoodsNum2.compareTo(releaseGoodsNum1) == 0) {
                            Long postNum1 = (o1.getPostNum() == null) ? 0 : o1.getPostNum();
                            Long postNum2 = (o2.getPostNum() == null) ? 0 : o2.getPostNum();
                            if (postNum2.compareTo(postNum1) == 0 && o1.getCreateTime() != null && o2.getCreateTime() != null) {
                                return o2.getCreateTime().compareTo(o1.getCreateTime());
                            }
                            return postNum2.compareTo(postNum1);
                        }
                        return releaseGoodsNum2.compareTo(releaseGoodsNum1);
                    }
                });
                return syncCelebrityDtoList.get(0);
            }
        } catch (Exception e) {
            log.error("[严重异常]用户搜索词:{}匹配大V数据异常", query, e);
        }
        return null;

    }

    /**
     * SyncCelebrityDto封装成VModel返回给前端
     * 兜底处理
     * 昵称：如果为空，昵称为空的用户不应该出现搜索结果中
     * 身份：认证用户
     * 简介：从现在起，你就是我的人了~
     *
     * @param syncCelebrityDto 来自SRM Dubbo接口的原始数据
     * @return 与客户端约定的大V数据结构模型
     */
    private VModel transfer2VModel(SyncCelebrityDto syncCelebrityDto, UISearchRequest request, String vtype) {
        if (syncCelebrityDto == null) {
            return null;
        }
        if (StringUtils.isEmpty(syncCelebrityDto.getNickname())) {
            return null;
        }
        VModel vmodel = new VModel();

        //平台号“V”图标为蓝色，当前vtype 为2时是黄色，为3时是蓝色，故当为平台号时，vtype设置为3.
        if(VModelTypeEnum.PLATFORM.getCode().equalsIgnoreCase(vtype)) {
        	vmodel.setVtype(VModelTypeEnum.ENTERPRISE.getCode());	
        } else {
        	vmodel.setVtype(syncCelebrityDto.getRoleType().toString());
        }
        if (StringUtils.isNotEmpty(syncCelebrityDto.getRealName())) {
            vmodel.setVnickName(syncCelebrityDto.getRealName());
        } else {
            vmodel.setVnickName(syncCelebrityDto.getNickname());
        }
        vmodel.setVidentity(StringUtils.isEmpty(syncCelebrityDto.getIdentity()) ? V_IDETIFY_DEFAULT_TEXT : syncCelebrityDto.getIdentity());
        vmodel.setVdesc(StringUtils.isEmpty(syncCelebrityDto.getIntroduction()) ? V_DESC_DEFAULT_TEXT : syncCelebrityDto.getIntroduction());
        //前端做兜底
        vmodel.setVdisplayPhotoUrl(StringUtils.isEmpty(syncCelebrityDto.getAvatarUrl()) ? "" : syncCelebrityDto.getAvatarUrl());
        vmodel.setVbackgroundImageUrl(StringUtils.isEmpty(syncCelebrityDto.getHeadImage()) ? "" : syncCelebrityDto.getHeadImage());
        vmodel.setVhomePageUrl(RouterUtil.buildByFriendHomePageURL(request, syncCelebrityDto.getCustomerId(), vtype));


        //梦工厂V1.1-2期，替换大V/企业定制号卡片部分字段字段为对应店铺字段
        if(!VModelTypeEnum.PLATFORM.getCode().equals(vmodel.getVtype())){
            //根据大V/企业定制号ID在缓存中获取对应的店铺对象
            WorksShopDto worksShopDto = syncVStoreCache.getWorksShopById(syncCelebrityDto.getCustomerId());
            if(worksShopDto != null){
                //更新对应的店铺数据字段
                //店铺头像、店铺背景图、店铺名称、店铺身份、店铺简介均不为空时，更新相应信息
                if(!StringUtils.isBlank(worksShopDto.getShopImage()) && !StringUtils.isBlank(worksShopDto.getCoverImage()) && !StringUtils.isBlank(worksShopDto.getShopName()) && !StringUtils.isBlank(worksShopDto.getIdentity())&& !StringUtils.isBlank(worksShopDto.getIntroduction())){
                    log.info("大V/企业定制号头像替换，原值："+vmodel.getVdisplayPhotoUrl());
                    vmodel.setVdisplayPhotoUrl(worksShopDto.getShopImage());
                    log.info("大V/企业定制号头像替换，替换后值："+worksShopDto.getShopImage());
                    log.info("大V/企业定制号背景图替换，原值："+vmodel.getVbackgroundImageUrl());
                    vmodel.setVbackgroundImageUrl(worksShopDto.getCoverImage());
                    log.info("大V/企业定制号背景图替换，替换后值："+worksShopDto.getCoverImage());
                    log.info("大V/企业定制号名称替换，原值："+vmodel.getVnickName());
                    vmodel.setVnickName(worksShopDto.getShopName());
                    log.info("大V/企业定制号名称替换，替换后值："+worksShopDto.getShopName());
                    log.info("大V/企业定制号身份替换，原值："+vmodel.getVidentity());
                    vmodel.setVidentity(worksShopDto.getIdentity());
                    log.info("大V/企业定制号身份替换，替换后值："+worksShopDto.getIdentity());
                    log.info("大V/企业定制号简介替换，原值："+vmodel.getVdesc());
                    vmodel.setVdesc(worksShopDto.getIntroduction());
                    log.info("大V/企业定制号简介替换，替换后值："+worksShopDto.getIntroduction());
                }
            }
        }

        return vmodel;
    }
}
