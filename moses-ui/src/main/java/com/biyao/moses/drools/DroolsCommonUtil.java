package com.biyao.moses.drools;

import com.biyao.moses.cache.RecommendManualSourceConfigCache;
import com.biyao.moses.cache.SwitchConfigCache;
import com.biyao.moses.common.enums.SeasonEnumOnline;
import com.biyao.moses.constants.CommonConstants;
import com.biyao.moses.constants.MosesBizConfigEnum;
import com.biyao.moses.context.ByUser;
import com.biyao.moses.model.drools.BuildBaseFactParam;
import com.biyao.moses.model.drools.RuleBaseFact;
import com.biyao.moses.model.drools.RuleFact;
import com.biyao.moses.model.match2.MatchItem2;
import com.biyao.moses.params.BaseRequest2;
import com.biyao.moses.params.RecommendAllRequest;
import com.biyao.moses.params.UIBaseRequest;
import com.biyao.moses.params.match2.MatchRequest2;
import com.biyao.moses.params.matchOnline.MatchOnlineRequest;
import com.biyao.moses.params.rank2.RankRequest2;
import com.biyao.moses.rpc.UcRpcService;
import com.biyao.moses.util.MyBeanUtil;
import com.biyao.moses.util.PartitionUtil;
import com.biyao.moses.util.StringUtil;
import com.uc.domain.bean.User;
import com.uc.domain.constant.UserFieldConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

import static com.biyao.moses.common.constant.CommonConstants.ZERO;
import static com.biyao.moses.common.constant.RsmConfigConstants.*;

/**
 * @program: moses-parent
 * @description: 规则引擎通用工具类
 * @author: changxiaowei
 * @create: 2021-04-12 14:15
 **/
@Slf4j
@Component
public class DroolsCommonUtil {

    @Autowired
    SwitchConfigCache switchConfigCache;
    @Autowired
    RecommendManualSourceConfigCache recommendManualSourceConfigCache;
    @Autowired
    UcRpcService ucRpcService;
    @Autowired
    PartitionUtil partitionUtil;
    /**
     * 构建规则引擎条件对象
     * @return
     */
    public RuleBaseFact buildRuleBaseFact(BuildBaseFactParam buildBaseFactParam){
        RuleBaseFact ruleBaseFact=new RuleBaseFact();

        // 场景id  (首页  个人中心 购物车 订单页)== uibaseRequest.getPagePositionId();感兴趣商品集 和 买二返一 通过topicId定位
        String scene = buildBaseFactParam.getSceneId();
        if(StringUtil.isBlank(scene)){
            if(CommonConstants.GXQSP_PAGE_TOPICID.equals(buildBaseFactParam.getTopicId())){
                scene="8";
            }
            if(CommonConstants.M2F1_PAGE_TOPICID.equals(buildBaseFactParam.getTopicId())) {
                scene="9";
            }
            if(MosesBizConfigEnum.JJG.getBizName().equals(buildBaseFactParam.getBiz())){
                scene="6";
            }
            if(MosesBizConfigEnum.ALLOWANCE_PAGE.getBizName().equals(buildBaseFactParam.getBiz())){
                scene="7";
            }
        }
        ruleBaseFact.setScene(scene);
        String uid="0";
        if(!StringUtil.isBlank(buildBaseFactParam.getUid())){
            uid=buildBaseFactParam.getUid();
        }
        // 个性化 只有当用户个性化开关为开且推荐个性化开关为开时，才为支持个性化
        if(partitionUtil.getPersonalizedRecommendSwitch(uid) && switchConfigCache.getRecommendSwitchByConfigId(CommonConstants.PERSONALIZE_RECOMMEND_OFF_CONFIG_ID)){
            ruleBaseFact.setIsPersonal(true);
        }
        ruleBaseFact.setUuid(buildBaseFactParam.getUuid());
        ruleBaseFact.setSiteId(buildBaseFactParam.getSiteId());
        String upcUserType = buildBaseFactParam.getUtype();
        if(StringUtil.isBlank(upcUserType)){
             upcUserType =partitionUtil.getUpcUserType(buildBaseFactParam.getUuid(), buildBaseFactParam.getUid()).toString();
        }
        ruleBaseFact.setUtype(upcUserType);

        return ruleBaseFact;
    }

      /**
       * @Des 构建matchonline request参数
       * @Param [request, ruleFact]架matchonline request参数
       * @return com.biyao.moses.params.matchOnline.MatchOnlineRequest
       * @Author changxiaowei
       * @Date  2022/1/26
       */
     public  MatchOnlineRequest  buildMatchOnlineRequest(RecommendAllRequest request,RuleFact ruleFact){
        MatchOnlineRequest matchOnlineRequest = new MatchOnlineRequest();
        // uuid uid sid device debug scendid channelType
         BeanUtils.copyProperties(request, matchOnlineRequest);
         if(!StringUtil.isBlank(request.getThirdCateGoryIdList())){
             matchOnlineRequest.setThirdCateGoryIdList(StringUtil.strConverToList(request.getThirdCateGoryIdList()));
         }
         if(!StringUtil.isBlank(request.getTagIdList())){
             matchOnlineRequest.setTagIdList(StringUtil.strConverToList(request.getTagIdList()));
         }
         if(request.getEntryProductId()!= null){
             matchOnlineRequest.setMainPid(request.getEntryProductId());
         }
        // siteId 类型不一致 无法通过copyProperties 赋值
         matchOnlineRequest.setSiteId(Integer.valueOf(request.getSiteId()));
         //规则引擎文件
         fillRuleFactInfo(matchOnlineRequest,ruleFact);
         // 填充UC级别的参数  类目下商品召回无需填充
         if(StringUtils.isEmpty(request.getFrontendCategoryId())){
             fillUcUserInfo(matchOnlineRequest,request.getUuid(),request.getUid());
         }
         return matchOnlineRequest;
     }


    /**
     * @Des 为在线召回参数填充ruleFact级别的参数 属性
     * @Param [matchOnlineRequest, ruleFact]
     * @return void
     * @Author changxiaowei
     * @Date  2022/3/23
     */
    private void fillRuleFactInfo(MatchOnlineRequest matchOnlineRequest,RuleFact ruleFact){
        //规则引擎文件
        matchOnlineRequest.setRuleId(ruleFact.getRuleId());
        matchOnlineRequest.setIsPersonal(ruleFact.getIsPersonal());
        matchOnlineRequest.setSourceAndWeight(ruleFact.getSourceAndWeight());
        matchOnlineRequest.setExpNum(ruleFact.getExpectNumMax());
    }

    /**
     * @Des 为在线召回参数填充ucUser 属性
     * @Param [matchOnlineRequest, uuid, uid]
     * @return com.biyao.moses.params.matchOnline.MatchOnlineRequest
     * @Author changxiaowei
     * @Date  2022/1/26
     */
     private MatchOnlineRequest fillUcUserInfo(MatchOnlineRequest matchOnlineRequest,String uuid,String uid){
         // 查询uc属性 性别 季节 深度浏览  地理位置 + 加购、购买（算法优化）
         User ucUser = ucRpcService.getData(uuid, uid,
                 Arrays.asList(UserFieldConstants.SEASON,
                         UserFieldConstants.SEX,UserFieldConstants.VIEWPIDS,
                         UserFieldConstants.CARTPIDS,UserFieldConstants.ORDERPIDS,
                         UserFieldConstants.LOCATION
                 ), "moses");
         if(Objects.nonNull(ucUser)){
             matchOnlineRequest.setUserSeason(SeasonEnumOnline.getSeasonIdByName(ucUser.getSeason()));
             matchOnlineRequest.setUserSex(ucUser.getSex().byteValue());
             // 深度浏览
             List<String> viewPids = ucUser.getViewPids();
             List<String> clickPids = new ArrayList<>();
             // 注意需要最近浏览的商品 按照时间倒叙取n个
             if(CollectionUtils.isNotEmpty(viewPids)){
                 int min = Math.min(viewPids.size(), switchConfigCache.getRsmIntValue(TF_DEEP_VIEW_LIMIT,TF_DEEP_VIEW_LIMIT_DEFAULT,ZERO,ZERO));
                 for (int i = 0; i < min; i++) {
                     clickPids.add(viewPids.get(viewPids.size()-1-i).split(":")[0]);
                 }
             }
             // 加购
             List<String> cartPids = ucUser.getCartPids();
             if(CollectionUtils.isNotEmpty(cartPids)){
                 int size =  cartPids.size();
                 int min = Math.min(size, switchConfigCache.getRsmIntValue(TF_CART_LIMIT,TF_CART_LIMIT_DEFAULT,ZERO,ZERO));
                 for (int i = size-1; i >=0 ; i--) {
                     if(min <= 0) break;
                     String spuId = cartPids.get(i).split(":")[0].substring(0,10);
                     if(clickPids.contains(spuId)) continue;
                     clickPids.add(spuId);
                     min--;
                 }
             }
             // 购买
             List<String> orderPids = ucUser.getOrderPids();
             if(CollectionUtils.isNotEmpty(orderPids)){
                 int size =  orderPids.size();
                 int min = Math.min(size, switchConfigCache.getRsmIntValue(TF_ORFER_LIMIT,TF_ORFER_LIMIT_DEFAULT,ZERO,ZERO));
                 for (int i = size-1; i >= 0 ; i--) {
                     if(min <= 0) break;
                     String spuId = orderPids.get(i).split(":")[0];
                     if(clickPids.contains(spuId)) continue;
                     clickPids.add(spuId);
                     min--;
                 }
             }
             matchOnlineRequest.setViewPids(clickPids);
             matchOnlineRequest.setLocation(ucUser.getLocation());
         }
         return matchOnlineRequest;
     }

    /**
     * @Des 构架matchonline request参数
     * @Param [uibaseRequest, user, ruleFact]
     * @return com.biyao.moses.params.matchOnline.MatchOnlineRequest
     * @Author changxiaowei
     * @Date  2021/12/24
     */
    public  MatchOnlineRequest  buildMatchOnlineRequest(UIBaseRequest uibaseRequest, ByUser user, RuleFact ruleFact){
        MatchOnlineRequest matchOnlineRequest = new MatchOnlineRequest();
        // 基本信息 byUser
        matchOnlineRequest.setUuid(user.getUuid());
        matchOnlineRequest.setUid(user.getUid());
        matchOnlineRequest.setSiteId(Integer.valueOf(user.getSiteId()));
        matchOnlineRequest.setSid(uibaseRequest.getSid());
        matchOnlineRequest.setDevice(user.getDevice());
        //填充 规则引擎文件中的属性到在线召回
        fillRuleFactInfo(matchOnlineRequest,ruleFact);
        // 网关传参
        matchOnlineRequest.setSceneId(uibaseRequest.getPagePositionId());
        // UC
        fillUcUserInfo(matchOnlineRequest,user.getUuid(),user.getUid());
        matchOnlineRequest.setDebug(user.isDebug());
        return matchOnlineRequest;
    }
    /**
     * 为drools 构建match请求参数  feeds流请求
     * @param uibaseRequest
     * @param user
     * @param ruleFact
     *  召回源及其权重、召回源及其策略、人工召回源list、最大召回商品数量、用户个性化开关
     * @return
     */
    public MatchRequest2 bulidFeedMatchRequestForDrools(UIBaseRequest uibaseRequest, ByUser user, RuleFact ruleFact){
        MatchRequest2 matchRequest2=new MatchRequest2();
        String sourceAndWeight = ruleFact.getSourceAndWeight();
        if(StringUtil.isBlank(sourceAndWeight)){
            return matchRequest2;
        }
        matchRequest2.setSourceAndWeight(ruleFact.getSourceAndWeight());

        BaseRequest2 baseRequest2 = constructBaseRequest2(user, uibaseRequest);
        MyBeanUtil.copyNotNullProperties(baseRequest2,matchRequest2);
        // 默认取出1000
        matchRequest2.setExpNum(1000);
        matchRequest2.setPersonalizedRecommendSwitch(user.isPersonalizedRecommendSwitch());
        matchRequest2.setSourceDataStrategy(ruleFact.getSourceDataStrategy());

        List<String> maunalSourceList=new ArrayList<>();
        Set<String> recommendManualSource = recommendManualSourceConfigCache.getRecommendManualSourceMapKey();
        if(CollectionUtils.isNotEmpty(recommendManualSource)){
            //拆分出召回源 本期先这么做  后期产品在规则文件中配置哪些召回源是人工召回源
            try {
                String[] sourceAndWeightLayerArray = sourceAndWeight.split(";");
                for(String sourceAndWeightLayer :sourceAndWeightLayerArray){
                    if(StringUtils.isBlank(sourceAndWeightLayer)){
                        continue;
                    }
                    String[] sourceAndWeightArray = sourceAndWeightLayer.split("\\|");
                    for (String str2:sourceAndWeightArray){
                        String[] sourceAndWeightStr = str2.split(",");
                        if(sourceAndWeightStr.length!=2){
                            continue;
                        }
                        if(recommendManualSource.contains(sourceAndWeightStr[0])){
                            maunalSourceList.add(sourceAndWeightStr[0]);
                        }
                    }
                }
            }catch (Exception e){
                log.error("[严重异常]召回源{}参数解析出错",sourceAndWeight);
            }
        }
        matchRequest2.setManualSourceList(maunalSourceList);
        return matchRequest2;
    }


    private BaseRequest2 constructBaseRequest2(ByUser user, UIBaseRequest uiBaseRequest){
        //根据uuid获取用户最近浏览的时间，后续如果有多个地方使用，可以放到headFilter中获取
        List<String> fields = new ArrayList<>();
        Long latestViewTime = null;
        fields.add(UserFieldConstants.LASTVIEWTIME);
        User ucUser = ucRpcService.getData(user.getUuid(), null, fields, "moses");
        if(ucUser != null){
            latestViewTime = ucUser.getLastViewTime();
        }
        BaseRequest2 baseRequest2 = new BaseRequest2();
        baseRequest2.setLatestViewTime(latestViewTime);
        if(StringUtils.isNotBlank(user.getUid())) {
            try{
                baseRequest2.setUid(Integer.valueOf(user.getUid()));
            }catch(Exception e){
                log.error("[严重异常][入参]用户uid数据非法， uid {}", user.getUid());
                baseRequest2.setUid(0);
            }
        }

        if(StringUtils.isNotBlank(user.getSiteId())) {
            try {
                Integer siteId = Integer.valueOf(user.getSiteId().trim());
                baseRequest2.setSiteId(siteId);
            } catch (Exception e) {
                log.error("[严重异常][入参]用户siteId数据非法， siteId {}", user.getSiteId());
            }
        }
        baseRequest2.setPriorityProductId(user.getPriorityProductId());
        baseRequest2.setUuid(user.getUuid());
        baseRequest2.setUpcUserType(user.getUpcUserType());
        baseRequest2.setDebug(user.isDebug());
        baseRequest2.setShowAdvert(user.getShowAdvert());
        if(StringUtils.isNotBlank(user.getSex())) {
            try {
                baseRequest2.setUserSex(Integer.valueOf(user.getSex()));
            }catch (Exception e){
                log.error("[严重异常]用户性别数据非法，uuid {}, sex {}",user.getUuid(), user.getSex());
            }
        }
        baseRequest2.setSid(uiBaseRequest.getSid());
        baseRequest2.setAdvertInfoList(user.getAdvertInfoList());
        baseRequest2.setFrontPageId(uiBaseRequest.getFrontPageId());
        baseRequest2.setPagePositionId(uiBaseRequest.getPagePositionId());
        return baseRequest2;
    }

    /**
     *
     * @param filterResult
     * @param ruleFact
     * @return
     */
    public RankRequest2 buildRankRequestForDrools(List<MatchItem2> filterResult, RuleFact ruleFact){
        RankRequest2 rankRequest2 = new RankRequest2();
        rankRequest2.setRankName("droolsRank");
        rankRequest2.setMatchItemList(filterResult);
        rankRequest2.setPriceFactor("1".equals(ruleFact.getPriceFactor()));
        rankRequest2.setRecallPoints("1".equals(ruleFact.getRecallPoints()));
        rankRequest2.setPunishFactor(ruleFact.getPunishFactor());
        return  rankRequest2;
    }

}
