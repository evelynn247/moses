package com.biyao.moses.service.imp;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.biyao.client.model.RecommendOperationalConfig;
import com.biyao.moses.cache.CmsMaterialValueCache;
import com.biyao.moses.cache.ProductDetailCache;
import com.biyao.moses.cache.RecommendOperationConfigCache;
import com.biyao.moses.cache.SwitchConfigCache;
import com.biyao.moses.cache.drools.RuleConfigCache;
import com.biyao.moses.common.enums.SortTypeEnum;
import com.biyao.moses.constants.CommonConstants;
import com.biyao.moses.constants.ERouterType;
import com.biyao.moses.context.ByUser;
import com.biyao.moses.drools.DroolsCommonUtil;
import com.biyao.moses.exp.MosesExpConst;
import com.biyao.moses.exp.UIExperimentSpace;
import com.biyao.moses.model.drools.BuildBaseFactParam;
import com.biyao.moses.model.drools.RuleBaseFact;
import com.biyao.moses.model.drools.RuleFact;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;
import com.biyao.moses.model.video.Video;
import com.biyao.moses.params.*;
import com.biyao.moses.params.match2.MatchResponse2;
import com.biyao.moses.util.PartitionUtil;
import com.biyao.moses.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.biyao.moses.common.constant.CommonConstants.*;
import static com.biyao.moses.common.constant.RsmConfigConstants.VIDEO_BASE_NUM;
import static com.biyao.moses.common.constant.RsmConfigConstants.VIDEO_BASE_NUM_DEFAULT;
import static com.biyao.moses.constants.CommonConstants.SHOW_TYPE_ADVERT_GLOBAL;
import static com.biyao.moses.constants.CommonConstants.SHOW_TYPE_PRO_GROUP;

/**
 * @ClassName AdvertInfoService
 * @Description 广告信息相关处理
 * @Author xiaojiankai
 * @Date 2020/6/1 10:48
 * @Version 1.0
 **/
@Slf4j
@Component
public class AdvertInfoService {

    @Autowired
    private CmsMaterialValueCache cmsMaterialValueCache;
    @Autowired
    private SwitchConfigCache switchConfigCache;
    @Autowired
    private UIExperimentSpace uiExperimentSpace;
    @Autowired
    private RecommendOperationConfigCache recommendOperationConfigCache;
    @Autowired
    DroolsCommonUtil droolsCommonUtil;
    @Autowired
    RuleConfigCache ruleConfigCache;
    @Autowired
    ProductDetailCache productDetailCache;
    /**
     * 解析入参活动信息
     * @param
     * @return
     */
    public static List<AdvertInfo> parseAdvertInfo(String advertInfo) {

        List<AdvertInfo> advertInfoList = new ArrayList<>();
        if(StringUtils.isBlank(advertInfo)){
            return advertInfoList;
        }

        boolean isCheckError = false;
        try {
            JSONArray jsonArray = JSON.parseArray(advertInfo);
            for (int i = 0; i < jsonArray.size(); i++) {
                try {
                    AdvertParam advertParam = jsonArray.getObject(i, AdvertParam.class);
                    String advertId = advertParam.getId();
                    String showPosition = advertParam.getPosition();
                    String imageSingle = advertParam.getImageSingle();
                    String imageWebpSingle = advertParam.getImageWebpSingle();
                    String imageDouble = advertParam.getImageDouble();
                    String imageWebpDouble = advertParam.getImageWebpDouble();
                    String router = advertParam.getRouter();
                    //如果其中之一为空，则认为该活动信息不全，过滤掉该活动信息
                    if (StringUtils.isBlank(advertId) || StringUtils.isBlank(showPosition)
                            || StringUtils.isBlank(imageSingle) || StringUtils.isBlank(imageWebpSingle)
                            || StringUtils.isBlank(imageDouble) || StringUtils.isBlank(imageWebpDouble)
                            || StringUtils.isBlank(router)) {
                        isCheckError = true;
                        continue;
                    }
                    AdvertInfo advertInfoTmp = new AdvertInfo();
                    advertInfoTmp.setPosition(Integer.valueOf(showPosition));
                    Map<String, String> routerParam = new HashMap<>();
                    TotalTemplateInfo info = new TotalTemplateInfo();
                    routerParam.put("router", router);
                    routerParam.put("position",showPosition);
                    if(ONE.toString().equals(advertParam.getIsVideo())){
                        routerParam.put("videoId", advertId);
                        info.setShowType(CommonConstants.SHOW_TYPE_OPE_VIDEO);
                        info.setRouterType(ERouterType.OPEVIDEO.getNum());
                    }else {
                        routerParam.put("activityId", advertId);
                        info.setShowType(CommonConstants.SHOW_TYPE_ADVERT);
                        info.setRouterType(ERouterType.ADVERT.getNum());
                    }
                    info.setRouterParams(routerParam);
                    info.setAdImage(imageDouble);
                    info.setAdImageWebp(imageWebpDouble);
                    info.setAdImageSingle(imageSingle);
                    info.setAdImageSingle(imageWebpSingle);
                    info.setId(CommonConstants.INVALID_PRODUCT_ID);
                    advertInfoTmp.setTotalTemplateInfo(info);
                    advertInfoList.add(advertInfoTmp);
                } catch (Exception e) {
                    isCheckError = true;
                }
            }
        }catch (Exception e){
            isCheckError = true;
        }
        if (isCheckError) {
            log.error("[严重异常]解析可以展示的活动广告列表入参错误， {}，", advertInfo);
        }
        return advertInfoList;
    }

    /**
     * 初步判断感兴趣商品集活动是否应该被展示(一定不会被展示的情况)
     * @return
     */
    public  Boolean isShowGXQSPAdvert(UIBaseRequest request,ByUser user){

        try {
            // 如果用户个性化活动开关关闭 return false
            if(!switchConfigCache.getRecommendSwitchByConfigId(CommonConstants.PERSONALIZE_RECOMMEND_OFF_CONFIG_ID)){
                return false;
            }
            RuleBaseFact ruleBaseFact = droolsCommonUtil.buildRuleBaseFact(BuildBaseFactParam.builder().
                    sceneId(request.getPagePositionId()).
                    siteId(user.getSiteId()).
                    topicId(request.getTopicId()).
                    uid(user.getUid()).uuid(user.getUuid()).utype(user.getUpcUserType().toString()).build());
            ruleBaseFact.setScene("8");
            RuleFact ruleFact = ruleConfigCache.getRuleFactByCondition(ruleBaseFact, "layer_mosesui_feed");
            // 若既没有进入规则 也没有进入实验
            if(ruleFact == null && !isNewExp(user)){
               return false;
            }
            user.setMatchGxqspRuleOrExp(true);
            // 如果当前活动中没有感兴趣商品集活动   return false
            if(!isContainsGxqspAdvert(user.getAdvertInfoList(),CommonConstants.GXQSP_ADVERT_ID)){
                return false;
            }
            // 如果当期页面不支持感兴趣商品集活动 return false
//            if(!isSupportAdvert(request.getPagePositionId(),CommonConstants.GXQSP_ADVERT_ID)){
//                return false;
//            }
        }catch (Exception e){
         log.error("[严重异常]初步判断感兴趣商品集活动是否应该被展示出错，异常信息:",e);
         return false;
        }
        return true;
    }

    public Boolean isSupportAdvert(String pagePositionId,String advertId){
        // 内容策略项目 取消rsm 对运营位的控制
//        if(StringUtils.isEmpty(pagePositionId)||StringUtil.isBlank(advertId)){
//            return  false;
//        }
//        // 从缓存中获取指定页面的运营位配置规则
//        RecommendOperationalConfig operationalConfig =
//                recommendOperationConfigCache.getOperationalConfigByOperationalPositionId(pagePositionId);
//        if(operationalConfig==null || StringUtils.isEmpty(operationalConfig.getSupportOperationalId())){
//            return false;
//        }
//        // 获取配置规则中支持的运营位id
//        String supportAdvertId= StringUtil.isBlank(operationalConfig.getSupportOperationalId()) ? "": operationalConfig.getSupportOperationalId();
//        List<String> supportAdverIdList=Arrays.asList(supportAdvertId.split(","));
//        if(!supportAdverIdList.contains(advertId)){
//            return false;
//        }
         return true;
    }


    /**
     * 判断广告列表中是否包含指定的广告
     * @param advertInfoList 运营位集合
     * @return
     */
    public   Boolean isContainsGxqspAdvert(List<AdvertInfo> advertInfoList,String adverId){

        boolean isContainsGxqspAdvert=false;
        if(CollectionUtils.isEmpty(advertInfoList) || StringUtil.isBlank(adverId)){
            return false;
        }
        for (AdvertInfo advertInfo:advertInfoList){
            // 获取广告模版
            TotalTemplateInfo totalTemplateInfo= advertInfo.getTotalTemplateInfo();
            if(totalTemplateInfo == null || totalTemplateInfo.IsRouterParamsEmpty()){
                continue;
            }
            // 活动id在模版的路由参数中RouterParams 中
            Map<String,String> routerMap=totalTemplateInfo.getRouterParams();
            if(adverId.equals(routerMap.get("activityId"))){
                isContainsGxqspAdvert=true;
            }
        }
        if(!isContainsGxqspAdvert){
            return false;
        }
        return  true;
    }
    /**
     *判断是否进入实验
     * @param user
     * @return
     */
   public Boolean isNewExp(ByUser user){

       // 当前用户实验分流未进入实验  return false
       BaseRequest2 baseRequest2=new BaseRequest2();
       baseRequest2.setFrontPageId(CommonConstants.GXQSP_FRONT_PAGE_ID);
       baseRequest2.setUuid(user.getUuid());
       baseRequest2.setUpcUserType(user.getUpcUserType());
       //实验分流
       uiExperimentSpace.divert(baseRequest2);
       HashMap<String, String> flags = baseRequest2.getFlags();
       if(MosesExpConst.VALUE_GXQSP_NEWEXP.equals(flags.get(MosesExpConst.FLAG_GXQSP_NEWEXP))){
           return true;
       }
       return false;
   }
    /**
     * 获取广告信息
     * @param showAdvert
     * @param userType
     * @param advertInfoList
     * @return 根据规则获取要插入的活动广告信息
     */
    public List<AdvertInfo> getAdvertInfoListByRule(String showAdvert, Integer userType, List<AdvertInfo> advertInfoList,ByUser user,String pagePositionId){


        List<AdvertInfo> result = new ArrayList<>();
        List<AdvertInfo> advertInfos = new ArrayList<>(advertInfoList);

        AdvertInfo xYuanAdvertInfo = cmsMaterialValueCache.getXYuanAdvertInfo(showAdvert, userType);
        if(xYuanAdvertInfo != null && xYuanAdvertInfo.getTotalTemplateInfo() != null ){
            advertInfos.add(xYuanAdvertInfo);
        }
        try {
            //按照展示位置从小到大排序
            advertInfos = advertInfos.stream()
                    .filter(info -> info != null && info.getPosition() != null && info.getTotalTemplateInfo() != null)
                    .sorted(Comparator.comparing(AdvertInfo::getPosition))
                    .collect(Collectors.toList());

            if (CollectionUtils.isEmpty(advertInfos)) {
                return result;
            }

//            //上一个活动广告
//            AdvertInfo lastInfo = null;
//            //活动广告集合用于随机获取一个
//            List<AdvertInfo> randomInfoList = new ArrayList<>();
            for (AdvertInfo info : advertInfos) {

                // 1 获取当前页面的配置　将不支持的运营位id过滤掉
                if (info.getTotalTemplateInfo() == null || info.getTotalTemplateInfo().getRouterParams() == null || info.getTotalTemplateInfo().getRouterParams().size() == 0) {
                    continue;
                }
                String advertId = info.getTotalTemplateInfo().getRouterParams().getOrDefault("activityId", "default");
//                if (!isSupportAdvert(pagePositionId, advertId)) {
//                    continue;
//                }

                if (CommonConstants.GXQSP_ADVERT_ID.equals(advertId)) {
                    try {
                        // 2 个性化活动开关关闭时，过滤感兴趣商品集活动
                        if (!switchConfigCache.getRecommendSwitchByConfigId(CommonConstants.PERSONALIZE_RECOMMEND_OFF_CONFIG_ID)) {
                            continue;
                        }
                        // 3 未进入实验的或者规则的用户  不展示感兴趣商品集活动
                        if (!user.isMatchGxqspRuleOrExp()) {
                            continue;
                        }
                        //4 感兴趣商品活动召回商品数量小于配置的数量 不展示感兴趣商品活动
                        // 获取配置
                        String content = switchConfigCache.getRecommendContentByConfigId(CommonConstants.GXQSP_EXP_MIN_NUM);
                        // 配置为空 或者配置的不为整数  或者配置小于0  取默认值
                        if (!StringUtil.isInteger(content)) {
                            content = "20";
                        }
                        MatchResponse2 matchResponse2 = user.getAsyncMatchResponse().get(400, TimeUnit.MILLISECONDS);
                        if (matchResponse2 == null || matchResponse2.getMatchItemList() == null || matchResponse2.getMatchItemList().size() < Integer.valueOf(content)) {
                            continue;
                        }
                    } catch (Exception e) {
                        log.error("[严重异常]判断感兴趣商品集运营位是否应该被插入时异常，异常信息", e);
                    }
                }

                result.add(info);
                // 内容策略项目 去除rsm系统对运营位的控制
                // 5 不满足间隔规则时，从两个点位中随机选择一个
//                if(getInsertRule(pagePositionId) == null){
//                  continue;
//                }
//
//                // 6 校验最大插入个数，优先选取点位小的
//                if (lastInfo == null) {
//                    lastInfo = info;
//                    continue;
//                }
//
//                if (info.getPosition() - lastInfo.getPosition() > getInsertRule(pagePositionId)) {
//                    //如果当前活动广告展示位置与上一个活动广告位置差大于最小间隔，则说明上一个活动广告可以展示
//                    result.add(lastInfo);
//                    lastInfo = info;
//                } else {
//                    //如果当前活动广告展示位置与上一个活动广告位置差小于等于最小间隔-1，则需要随机选择一个
//                    randomInfoList.add(lastInfo);
//                    randomInfoList.add(info);
//                    Collections.shuffle(randomInfoList);
//                    lastInfo = randomInfoList.get(0);
//                    randomInfoList.clear();
//                }
//            }

//            if (lastInfo != null) {
//                result.add(lastInfo);
//            }
            }
        }catch (Exception e){
            log.error("[严重异常]根据规则选择活动广告出现异常，", e);
        }

        // 6 获取最大插入个数 异常情况不推运营位
//        RecommendOperationalConfig operationalConfig = recommendOperationConfigCache.getOperationalConfigByOperationalPositionId(pagePositionId);
//        if(operationalConfig == null || operationalConfig.getMaxInsert()==null || operationalConfig.getMaxInsert()<=0){
//            result.clear();
//            return result;
//        }
//        if(result.size() > operationalConfig.getMaxInsert()){
//            result= result.subList(0,operationalConfig.getMaxInsert());
//        }
        return result;
    }

    /**
     * 判断该模板信息能否放到最后
     * @param templateInfoList
     * @param templateInfo
     * @result true:能, false:不能
     */
    public  boolean isFitAtLast(List<TotalTemplateInfo> templateInfoList, TotalTemplateInfo templateInfo,String pagePositionId){
        //非活动广告则可以放到最后
        if(!SHOW_TYPE_ADVERT_GLOBAL.contains(templateInfo.getShowType())){
            return true;
        }
        return  false;
//        if(getInsertRule(pagePositionId) == null || getInsertRule(pagePositionId) == 0){
//            return false;
//        }
//        int size = templateInfoList.size();
//        int count = Math.min(getInsertRule(pagePositionId), size);
//        //如果前面5个有活动广告，则不能再最后展示活动广告
//        for(int i = 0; i < count; i++){
//            int index = size - i - 1;
//            TotalTemplateInfo totalTemplateInfo = templateInfoList.get(index);
//            if (totalTemplateInfo != null &&
//                    CommonConstants.SHOW_TYPE_ADVERT.equals(totalTemplateInfo.getShowType())) {
//                result = false;
//                break;
//            }
//        }

    }

    /**
     * 判断该展示商品集能否在最后插入活动位置
     * @param templateInfoList
     * @param advertInfo
     * @result true:能, false:不能
     */
    public  boolean isFitInsertAdvertAtLast(List<TotalTemplateInfo> templateInfoList, AdvertInfo advertInfo,String pagePositionId){

        if(templateInfoList == null || advertInfo == null || advertInfo.getPosition() == null){
            return false;
        }
        int size = templateInfoList.size();
        if(advertInfo.getPosition() <= size){
            return false;
        }

        return isFitAtLast(templateInfoList, advertInfo.getTotalTemplateInfo(),pagePositionId);
    }

    /**
     * 判断当前广告能否插入在指定位置
     * @param templateInfoList
     * @param advertInfo
     * @return
     */
    public boolean isFitInsertAdvert(List<TotalTemplateInfo> templateInfoList, AdvertInfo advertInfo,String pagePositionId){
        boolean result = true;
        if(templateInfoList == null || advertInfo == null || advertInfo.getPosition() == null){
            return false;
        }
//        Integer advertMinInterval = getInsertRule(pagePositionId);
//        if(advertMinInterval==null){
//            return false;
//        }
//        //该广告在当前页展示的位置
//        int index = (advertInfo.getPosition() - 1) % PartitionUtil.GET_PRODUCT_NUM_PER_TIMES;
//        //如果前面已经有了5个空位置，则表示可以插入
//        if(index >= advertMinInterval){
//            return true;
//        }
//        int size = templateInfoList.size();
//        int count = Math.min(size, advertMinInterval - index);
//        for(int i = 0; i < count; i++){
//            TotalTemplateInfo totalTemplateInfo = templateInfoList.get(size - i - 1);
//            if(totalTemplateInfo != null && CommonConstants.SHOW_TYPE_ADVERT.equals(totalTemplateInfo.getShowType())){
//                result = false;
//                break;
//            }
//        }
        return result;
    }

    /**
     * 在待展示商品中直接插入活动广告
     * @param templateInfoList
     * @param advertInfoList
     */
    public void directInsertAdvertInfoList(List<TotalTemplateInfo> templateInfoList, List<AdvertInfo> advertInfoList,String pagePositionId){
        if(CollectionUtils.isEmpty(templateInfoList) || CollectionUtils.isEmpty(advertInfoList)){
            return;
        }

        for(AdvertInfo advertInfo : advertInfoList){
            if(advertInfo == null
                || advertInfo.getPosition() == null || advertInfo.getPosition() < 1
                || advertInfo.getTotalTemplateInfo() == null){
                continue;
            }
            int size = templateInfoList.size();
            int advertPosition = advertInfo.getPosition();
            if(advertPosition > size){
                if(isFitInsertAdvertAtLast(templateInfoList, advertInfo,pagePositionId)) {
                    templateInfoList.add(advertInfo.getTotalTemplateInfo());
                }
            }else{
                templateInfoList.add(advertInfo.getPosition()-1, advertInfo.getTotalTemplateInfo());
            }
        }
    }

    /**
     * 在结果集中直接插入活动广告，用于未进入隔断时需要插入活动广告场景
     * @param feedexpData 结果集
     * @param uiBaseRequest
     * @param byUser
     */
    public void dealDirectInsertAdvertList(Map<String, List<TotalTemplateInfo>> feedexpData, UIBaseRequest uiBaseRequest, ByUser byUser) {
        try {
            //非热门不插入活动
            String sortType = uiBaseRequest.getSortType();
            if(StringUtils.isNotBlank(sortType) && !SortTypeEnum.ALL.getType().equals(sortType)){
                return;
            }

            //通过筛选项筛选时不插入活动
            if(StringUtils.isNotBlank(uiBaseRequest.getSelectedScreenAttrs())){
                return;
            }

            //如果是新手专享、但是选择了分类，则不插入活动
            if (StringUtils.isNotBlank(uiBaseRequest.getNovicefrontcategoryOneId())) {
                return;
            }

            List<AdvertInfo> advertInfoList = getAdvertInfoListByRule(byUser.getShowAdvert(), byUser.getUpcUserType(), byUser.getAdvertInfoList(),byUser,uiBaseRequest.getPagePositionId());
            //如果未获取到活动则不插入活动
            if (CollectionUtils.isEmpty(advertInfoList)) {
                return;
            }
            //插入活动
            for (List<TotalTemplateInfo> infoList : feedexpData.values()) {
                if (infoList != null) {
                    directInsertAdvertInfoList(infoList, advertInfoList,uiBaseRequest.getPagePositionId());
                    break;
                }
            }
        }catch (Exception e){
            log.error("[严重异常][活动广告入口]插入活动发生异常 ", e);
        }
    }

    /**
     * 获取每次循环需要处理的活动入口集合，每次循环的索引从0开始
     * @param advertInfoList
     */
    public Map<Integer, List<AdvertInfo>> getLoopAdvertInfosMap(List<AdvertInfo> advertInfoList){
        Map<Integer, List<AdvertInfo>> result = new HashMap<>();
        if(CollectionUtils.isEmpty(advertInfoList)){
            return result;
        }
        try{
            //每次循环处理的个数
            int countPerLoop = PartitionUtil.GET_PRODUCT_NUM_PER_TIMES;
            for(AdvertInfo advertInfo : advertInfoList){
                if(advertInfo == null || advertInfo.getPosition() == null
                    || advertInfo.getPosition() < 1){
                    continue;
                }
                Integer loopIndex = (advertInfo.getPosition()-1) / countPerLoop;
                if(!result.containsKey(loopIndex)){
                    List<AdvertInfo> advertInfos = new ArrayList<>();
                    advertInfos.add(advertInfo);
                    result.put(loopIndex, advertInfos);
                }else{
                    result.get(loopIndex).add(advertInfo);
                }
            }
        }catch (Exception e){
            log.error("[严重异常][活动广告入口]获取每次循环需要处理的活动广告信息出现异常，", e);
        }

        return result;
    }

    /**
     * 获取页面配置规则 运营位插入最小间隔
     * @param pagePositionId
     * @return
     */
    public Integer getInsertRule(String pagePositionId){

        if(StringUtil.isBlank(pagePositionId)){
            return  null;
        }
        RecommendOperationalConfig operationalConfig = recommendOperationConfigCache.getOperationalConfigByOperationalPositionId(pagePositionId);

        if(operationalConfig==null || operationalConfig.getInsertRule()==null){
            return null;
        }
        return operationalConfig.getInsertRule();
    }


    /**
     * @Des 查询最优的视频id
     * 1）优先选取视频累计曝光量< = video-basenum(初始曝光值 rsm) 有多条该类视频时，随机选取一个视频进行展示
     * 2）若条件1不满足，则选取视频最高分
     * 3）若条件2不满足，则 轮播图 > 平台> 用户精选
     * @return java.lang.Integer
     * @Author changxiaowei
     * @Date  2022/2/19
     */
    public Integer selectOptimalVid(String videoInfo,int channelType){
        Integer defaultVid = null;
        if(StringUtil.isBlank(videoInfo)){
            return defaultVid;
        }
        int videoBasenum = switchConfigCache.getRsmIntValue(VIDEO_BASE_NUM,VIDEO_BASE_NUM_DEFAULT,ZERO,ZERO);
        try {
            List<Video> videoList = JSONArray.parseArray(videoInfo, Video.class);
            //去除无效的视频
            videoList.removeIf(video -> !Video.isValid(video,channelType));
            videoList.sort((o1, o2) ->{
                // 视频曝光量不大于 初始曝光量的排在前面
                int o1exp = o1.getExpo() ==null ? 0:o1.getExpo();
                int o2exp = o2.getExpo() ==null ? 0:o2.getExpo();
                if(o1exp <= videoBasenum){
                    return -1;
                }
                if(o2exp <= videoBasenum){
                    return 1;
                }
                if(o1.getScore().equals(o2.getScore())){
                    //分一样 类型最大的排在前面
                    return o2.getType().compareTo(o1.getType());
                }else {
                    // 分高的排在前面
                    return o2.getScore().compareTo(o1.getScore());
                }
            });
            return CollectionUtils.isEmpty(videoList) ? defaultVid :videoList.get(0).getVid();
        }catch (Exception e){
            log.error("[严重异常]根据规则选择最优的视频时出现异常,视频信息:{},异常信息:{}",videoInfo,e);
        }
        return defaultVid;
    }

    /**
     * @param showType 展示类型
     * @param productIndex 商品当前位置
     * @param videoInterval 视频间隔
     * @param lastProductIndex 上次被替换的商品位置
     * @param feedIndex 当前页面页码
     * @param lastFeedIndex 上次被替换视频的页码
     * @param size 页面尺寸
     * @param advertNum 具体上次替换到本次 间隔多少运营位
     * @param videoInfo 视频信息
     * @return
     */
    public   boolean isSatisfyConverTOVideo(String showType,String pid,
                                            int productIndex,int feedIndex,
                                            int videoInterval, Integer lastProductIndex ,
                                            int lastFeedIndex,int size,
                                            int advertNum,String videoInfo){
        // 没有视频信息的商品 返回false
        if(StringUtil.isBlank(videoInfo)){
            return  false;
        }
        // 商品组 和广告运营位不做替换
        if (SHOW_TYPE_PRO_GROUP.equals(showType) ||
                SHOW_TYPE_ADVERT_GLOBAL.contains(showType)){
            return false;
        }
        // 上一个被替换成视频的位置到当前位置间隔数 第一次可以被替换（lastFeedIndex=0）
        if(lastFeedIndex != 0 && lastProductIndex !=null){
            // 如果上一个被替换成视频的位置到当前位置的非运营位置数量小于间隔要求
            int Interval = (feedIndex-lastFeedIndex) * size+productIndex -lastProductIndex-1;
            if (Interval-advertNum < videoInterval) {
                return false;
            }
        }
        // 非：隐形眼镜、低模眼镜、可定制咖啡、衍生商品、白模可定制商品
        ProductInfo productInfo = productDetailCache.getProductInfo(Long.valueOf(pid));
        if(productInfo == null || ZERO.byteValue() != productInfo.getSupportTexture() ||
                GLASSES_CATEGORY2_IDS.contains(productInfo.getSecondCategoryId())){
            return  false;
        }
        return true;
    }


    /**
     * 获取当前页面 运营配置的广告位
     * @param videoInfo 总广告位
     * @param feedIndex 当前页码
     * @param pageSize 页面大小
     * @return
     */
    public List<AdvertParam>  getAdverInfoByFeedIndex(List<AdvertParam> videoInfo, int feedIndex, int pageSize){
        List<AdvertParam> result = new ArrayList<>();
        if (CollectionUtils.isEmpty(videoInfo)) {
            return result;
        }
        try {
            for (AdvertParam advertParam : videoInfo) {
                if(StringUtil.isInteger(advertParam.getPosition())){
                    Integer position = Integer.valueOf(advertParam.getPosition());
                    if (position > (feedIndex-1)*pageSize && position <=(feedIndex)*pageSize){
                        result.add(advertParam);
                    }
                }
            }
        }catch (Exception e){
            log.info("[严重异常]获取当前页面运营配置的广告位异常。videoInfo:{},feedIndex:{}", JSONObject.toJSONString(videoInfo),feedIndex);
        }
        return result;
    }

}
