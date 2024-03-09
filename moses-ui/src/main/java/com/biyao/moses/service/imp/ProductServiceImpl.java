package com.biyao.moses.service.imp;

import com.alibaba.fastjson.JSONObject;
import com.biyao.moses.cache.ProductDetailCache;
import com.biyao.moses.cache.SwitchConfigCache;
import com.biyao.moses.common.constant.ErrorCode;
import com.biyao.moses.constants.CommonConstants;
import com.biyao.moses.context.ByUser;
import com.biyao.moses.model.SearchProduct;
import com.biyao.moses.model.template.entity.FirstCategory;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;
import com.biyao.moses.model.video.Video;
import com.biyao.moses.params.*;
import com.biyao.moses.rpc.ProductGroupRpcService;
import com.biyao.moses.service.IProductService;
import com.biyao.moses.util.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

import static com.biyao.moses.common.constant.AlgorithmRedisKeyConstants.PRODUICT_VEDIO_REDIS;
import static com.biyao.moses.common.constant.CommonConstants.TIME_30MIN;
import static com.biyao.moses.constants.CommonConstants.SHOW_TYPE_ADVERT_GLOBAL;
import static com.biyao.moses.constants.CommonConstants.VEDIO;

/**
 * ProductServiceImpl
 *
 * @Description
 * @Date 2018年9月27日
 */
@Service
@Slf4j
public class ProductServiceImpl implements IProductService {

    // @Autowired
    // private SearchProductMapper searchProductMapper;

    private static final String MOSES_TOPIC_PREFIX = "moses:";

    private static final String MOSES_TOPIC_SUFFIX = "_TGM_0000";
    // 商品组默认间隔
    private static final String PRODUCT_GROUP_DEFAULT_LIMIT = "10";
    @Autowired
    private RedisUtil reidsUtil;

    @Autowired
    private CacheRedisUtil cacheRedisUtil;
    @Autowired
    ProductGroupRpcService productGroupRpcService;
    @Autowired
    AlgorithmRedisUtil algorithmRedisUtil;

    @Autowired
    private ProductDetailCache productDetailCache;
    @Autowired
    private SwitchConfigCache switchConfigCache;
    @Autowired
    private AdvertInfoService advertInfoService;
    @Autowired
    FilterUtil filterUtil;
    @Override
    public SearchProduct selectByPrimaryKey(Integer productId) {
        // return searchProductMapper.selectByPrimaryKey(productId);
        return null;
    }

    @Override
    public ApiResult<String> isNewuserProduct(RecommendNewuserRequest recommendNewuserRequest) {
        ApiResult<String> apiResult = new ApiResult<>();
        try {
            String topicId = recommendNewuserRequest.getTopicId();
            List<String> spuIds = recommendNewuserRequest.getSpuIds();
            if (StringUtils.isEmpty(topicId) || spuIds == null || spuIds.size() == 0) {
                apiResult.setError("params error");
                apiResult.setSuccess(ErrorCode.PARAM_ERROR_CODE);
                return apiResult;
            } else {
                String productIds = reidsUtil.getString(MOSES_TOPIC_PREFIX + topicId + MOSES_TOPIC_SUFFIX);
                Map<String, Boolean> map = new HashMap<String, Boolean>();
                if (spuIds.size() > 50) {
                    spuIds = spuIds.subList(0, 50);
                }
                // redis数据格式：1301085113:1.0,1301485110:0.5,1300815054:0.333333333333
                if (StringUtils.isNotBlank(productIds)) {
                    List<String> newuserSpuIds = new ArrayList<String>();
                    String[] spuIdskv = productIds.split(",");
                    for (String newuserSpuId : spuIdskv) {
                        newuserSpuIds.add(newuserSpuId.split(":")[0]);
                    }
                    for (String spuId : spuIds) {
                        if (newuserSpuIds.contains(spuId)) {
                            ProductInfo productInfo = productDetailCache.getProductInfo(Long.valueOf(spuId));
                            if (productInfo != null && productInfo.getIsToggroupProduct() == 1) {
                                map.put(spuId, true);
                            } else {
                                map.put(spuId, false);
                            }
                        } else {
                            map.put(spuId, false);
                        }
                    }
                } else {
                    for (String spuId : spuIds) {
                        map.put(spuId, false);
                    }
                }
                apiResult.setData(JSONObject.toJSONString(map));
            }
        } catch (Exception e) {
            apiResult.setError("system error");
            apiResult.setSuccess(ErrorCode.SYSTEM_ERROR_CODE);
            log.error("[严重异常]判断商品是否是新手专享商品出错", e);
        }
        return apiResult;
    }

    @Override
    public ApiResult<List<FirstCategory>> getFirstCategoryDuplicate(ApiResult<List<FirstCategory>> apiResult, NewUserCategoryRequest newUserCategoryRequest) {
        try {

            //数据格式：pid:score,pid:score....
            String productIdDatas = reidsUtil.getString(MOSES_TOPIC_PREFIX + newUserCategoryRequest.getTopicId() + MOSES_TOPIC_SUFFIX);
            if(StringUtils.isEmpty(productIdDatas)){
                return apiResult;
            }

            //前台一级类目集合
            Map<String, FirstCategory> firstCategoryMap = new HashMap<>();

            //解析productIdDatas
            List<String> productIdDataLst = new ArrayList<>();
            Collections.addAll(productIdDataLst, productIdDatas.split(","));
            for (int i = 0; i < productIdDataLst.size(); i++){

                String[] productIdArr = productIdDataLst.get(i).split(":");
                //获取商品信息
                ProductInfo productInfo = productDetailCache.getProductInfo(Long.valueOf(productIdArr[0]));

                //如果商品对接为空、非一起拼商品、未上架、前台一级类目ID为空、前台一级类目为空则不处理
                if(productInfo == null ||
                        productInfo.getIsToggroupProduct() == null ||
                        productInfo.getIsToggroupProduct() != 1 ||
                        CollectionUtils.isEmpty(productInfo.getFCategory1Ids()) ||
                        CollectionUtils.isEmpty(productInfo.getFCategory1Names()) ||
                        !StringUtils.equals("1",String.valueOf(productInfo.getShelfStatus()))
                        ){
                    continue;
                }
                if(filterUtil.isFilteredBySiteId(productInfo.getProductId(),newUserCategoryRequest.getSiteId())){
                    continue;
                }
                List<String> fCategory1IdLst = productInfo.getFCategory1Ids();
                List<String> fCategory1NameLst = productInfo.getFCategory1Names();
                //筛选新手专享前台一级类目
                for (int j = 0; j < fCategory1IdLst.size(); j++){
                    String fCategory1Id = fCategory1IdLst.get(j);
                    if(firstCategoryMap.containsKey(fCategory1Id)){
                        continue;
                    }
                    if(StringUtils.isEmpty(fCategory1NameLst.get(j))){
                        continue;
                    }

                    FirstCategory firstCategory = FirstCategory.builder().firstCategoryId(fCategory1Id).firstCategoryName(fCategory1NameLst.get(j)).build();
                    firstCategoryMap.put(firstCategory.getFirstCategoryId(),firstCategory);
                }
            }
            //构建返回结果
            apiResult.setData(new ArrayList<>(firstCategoryMap.values()));
        } catch (Exception e) {
            apiResult.setError("system error");
            apiResult.setSuccess(ErrorCode.SYSTEM_ERROR_CODE);
            log.error("[严重异常]新手专享获取一起拼商品一级类目集合出错", e);
        }
        return apiResult;
    }

    /**
     * 获取推荐商品ID列表
     *
     * @param productScoreStr 数据格式 pid1:score1,pid2:score2...或者pid1,pid2...
     * @return
     */
    private List<String> splitIdAndScore(String productScoreStr) {
        if (org.apache.commons.lang3.StringUtils.isBlank(productScoreStr)) {
            return new ArrayList<>();
        }
        // todo lambda表达式会影响效率
        List<String> rcdProductList = Arrays.stream(productScoreStr.split(",")).map(productScore -> {
            return productScore.split(":")[0];
        }).collect(Collectors.toList());
        return rcdProductList;
    }

    // 将商品替换成商品组
    @Override
    public void converProductToProductGroup(List<TotalTemplateInfo> totalTemplateInfoList, UIBaseRequest uiBaseRequest, ByUser user,Integer feedIndex){
        if(CollectionUtils.isEmpty(totalTemplateInfoList)){
            log.error("[严重异常]待组装商品列表为空.uuid:{}",user.getUuid());
            return;
        }
        String key = CommonConstants.DEFAULT_PREFIX +user.getPvid()+"_"+uiBaseRequest.getPageId()+"_"+uiBaseRequest.getTopicId()+"_"+uiBaseRequest.getFrontendCategoryId();
        String positionAndPageIndex = "";
        Integer tempPosition;
        try {
            // 如果请求为第一页 则清除商品组位置缓存
            if(feedIndex==1){
                cacheRedisUtil.del(key);
            }else {
                //如果不是第一页则需要从redis中获取上一页面被替换商品组的位置
                positionAndPageIndex = cacheRedisUtil.getString(key);
            }
            // 查询cms服务 获取商品组信息
            List<Long> pidList = totalTemplateInfoList.stream().map(
                    totalTemplateInfo -> Long.valueOf(totalTemplateInfo.getId())
            ).collect(Collectors.toList());
            if(CollectionUtils.isEmpty(pidList)){
                log.error("[严重异常]待组装商品pid列表为空.uuid:{}",user.getUuid());
                return;
            }
            Map<Long, Long> productGroupInfoMap = productGroupRpcService.getProductGroupInfo(pidList,user.getUuid());
            log.info("[检查日志]查询商品组信息结果返回：{},uuid:{}",JSONObject.toJSONString(productGroupInfoMap),user.getUuid());
            // 本页没有商品组信息 则返回
            if(CollectionUtils.isEmpty(productGroupInfoMap)){
                return;
            }
            // 从rsm中获取间隔配置
            String productGroupLimit = switchConfigCache.getRecommendContentByConfigId(CommonConstants.CATEGORY_PRO_GROUP_LIMIT);
            if(StringUtils.isBlank(productGroupLimit)|| !StringUtil.isInteger(productGroupLimit)){
                productGroupLimit = PRODUCT_GROUP_DEFAULT_LIMIT;
            }
            // 计算本页面第一个被替换成商品组的位置
            tempPosition = calculateFirstPosition(positionAndPageIndex,Integer.valueOf(productGroupLimit),feedIndex);
            // 标示该页是否有商品组被替换
            // 遍历待组装商品 替换商品组
            while (tempPosition < totalTemplateInfoList.size()) {
                TotalTemplateInfo totalTemplateInfo = totalTemplateInfoList.get(tempPosition);
                Long id = Long.valueOf(totalTemplateInfo.getId());
                ProductInfo productInfo = productDetailCache.getProductInfo(id);
                Long productGroupId = productGroupInfoMap.get(id);
                // 无效商品或者不满足替换条件的商品不替换
                if (productInfo == null || productGroupId == null) {
                    tempPosition++;
                    continue;
                }
                // 商品替换成商品组
                Map<String, String> routerParams = new HashMap<>();
                routerParams.put("productGroupId", productGroupId.toString());
                routerParams.put("suId",productInfo.getSuId().toString());
                log.info("[检查日志]被替换商品组的商品id:{},商品组id:{},uuid:{}",id,productGroupId,user.getUuid());
                totalTemplateInfo.setShowType(CommonConstants.SHOW_TYPE_PRO_GROUP);
                totalTemplateInfo.setRouterParams(routerParams);
                cacheRedisUtil.setString(key, tempPosition +":"+feedIndex, TIME_30MIN);
                tempPosition = tempPosition + Integer.valueOf(productGroupLimit)+1;
            }
        }catch (Exception e){
            log.error("[严重异常]商品替换成商品组异常.uuid:{},异常信息:{},",user.getUuid(),e);
        }
   }

    public void createTestData(String[] pids){
        Map<String, String> map = new HashMap<>();
        Random r = new Random(1);
        int max=3,min=1;
        for (int i = 0; i < pids.length;) {
            int ran1 = (int) (Math.random()*(max-min)+min);
            Video v1 = new Video(3,r.nextFloat(),r.nextInt(100),"2022-02-15 14:30:55.006",1);
            Video v2 = new Video(5,r.nextFloat(),r.nextInt(100),"2022-02-14 14:30:55.006",2);
            Video v3 = new Video(17,r.nextFloat(),r.nextInt(100),"2022-02-16 14:30:55.006",3);
            List<Video> list = new ArrayList<>();
            list.add(v1);list.add(v2);list.add(v3);
            map.put(pids[i],JSONObject.toJSONString(list));
            i= i+ran1;
        }
//        algorithmRedisUtil.del(PRODUICT_VEDIO_REDIS);
        algorithmRedisUtil.hmset(PRODUICT_VEDIO_REDIS,map);
        algorithmRedisUtil.expire(PRODUICT_VEDIO_REDIS,60*60*24*7);
    }
    @Override
    public void converProductToVideo(List<TotalTemplateInfo>  totalTemplateInfoList, UIBaseRequest uiBaseRequest, ByUser user, Integer feedIndex) {
        if(CollectionUtils.isEmpty(totalTemplateInfoList)){
            log.error("[严重异常]待组装商品列表为空.uuid:{}",user.getUuid());
            return;
        }
        Integer videoInterval = uiBaseRequest.getVideoInterval();
        if(videoInterval == null){
            log.error("[严重异常]商品替换为视频流入口时，视频间隔参数为空.uuid:{}",user.getUuid());
            return;
        }
        String[] pids = new String[totalTemplateInfoList.size()];
        for (int i = 0; i < totalTemplateInfoList.size(); i++) {
            pids[i]=totalTemplateInfoList.get(i).getId();
        }
        // 造测试环境数据
//         createTestData(pids);
        // 获取redis中商品和视频的关系
        List<String> videoInfoList = algorithmRedisUtil.hmget(PRODUICT_VEDIO_REDIS, pids);
        if (CollectionUtils.isEmpty(videoInfoList)) {
            videoInfoList = new ArrayList<>(totalTemplateInfoList.size());
        }
        // 上一页商品被替换成视频的页码、位置 以及期间配置多少运营位
        String pindexPidIndexAdverNumStr = "";
        StringBuilder redisKeySb = new StringBuilder();
        redisKeySb.append(CommonConstants.DEFAULT_PREFIX).append(VEDIO).append(user.getPvid()).append(":").append(uiBaseRequest.getPageId());
        if(!StringUtil.isBlank(uiBaseRequest.getTopicId())){
            redisKeySb.append(":").append(uiBaseRequest.getTopicId());
        }
        if(!StringUtil.isBlank(uiBaseRequest.getFrontendCategoryId())){
            redisKeySb.append(":").append(uiBaseRequest.getFrontendCategoryId());
        }
        String rediskey =redisKeySb.toString();
        if(feedIndex==1){
            cacheRedisUtil.del(rediskey);
        }else {
         //如果不是第一页则需要从redis中获取上一页面被替换商品组的位置 格式： 2:12:5
            pindexPidIndexAdverNumStr = cacheRedisUtil.getString(rediskey);
        }
        int lastProductIndex =0 ; int lastFeedIndex=0; int  advertNum =0;
        pindexPidIndexAdverNumStr = StringUtil.isBlank(pindexPidIndexAdverNumStr) ? "" :pindexPidIndexAdverNumStr;
        String[] pindexPidIndexAdverNum = pindexPidIndexAdverNumStr.split(":");
        if(pindexPidIndexAdverNum.length==3){
            try {
                lastFeedIndex =Integer.valueOf(pindexPidIndexAdverNum[0]);
                lastProductIndex =Integer.valueOf(pindexPidIndexAdverNum[1]);
                advertNum =Integer.valueOf(pindexPidIndexAdverNum[2]);
            }catch (Exception e){
                log.error("[严重异常]存储上一次被替换的商品替换视频位置信息格式错误，positionAndPageIndex:{}",pindexPidIndexAdverNumStr);
            }
        }
        // 遍历商品 替换为视频
        for (int i = 0; i < totalTemplateInfoList.size(); i++) {
            TotalTemplateInfo totalTemplateInfo = totalTemplateInfoList.get(i);
            // 判断当前数据是否满足被替换成视频的基本条件
          if (!advertInfoService.isSatisfyConverTOVideo(totalTemplateInfo.getShowType(),totalTemplateInfo.getId(),i,feedIndex,videoInterval,
                  lastProductIndex,lastFeedIndex,totalTemplateInfoList.size(),advertNum,videoInfoList.get(i))){
              //如果不满足的话 需要看下是不是因为运营位不满足
              if (SHOW_TYPE_ADVERT_GLOBAL.contains(totalTemplateInfo.getShowType())){
                 advertNum++;
              }
              continue;
          }
           // 满足上述条件 替换视频
            Integer vid = advertInfoService.selectOptimalVid(videoInfoList.get(i),uiBaseRequest.getChannelType());
            // 替换失败 跳过
            if(Objects.isNull(vid)){
                log.error("[严重异常]商品替换为视频时出现异常，商品:{}，视频信息:{},uuid:{}，场景id:{}",
                        totalTemplateInfo.getId(),videoInfoList.get(i),user.getUuid(),uiBaseRequest.getPagePositionId());
                continue;
            }
            lastFeedIndex =feedIndex; lastProductIndex =i ;advertNum=0;
            Map<String, String> routerParams = new HashMap<>();
            routerParams.put("videoId", vid.toString());
            routerParams.put("position",String.valueOf(i));
            log.info("[检查日志]被替换视频的商品id:{},视频id:{},uuid:{}",totalTemplateInfo.getId(),vid,user.getUuid());
//            System.out.println("商品替换视频：当前页面:"+feedIndex+",位置:"+i+",商品:"+totalTemplateInfo.getId());
            totalTemplateInfo.setShowType(CommonConstants.SHOW_TYPE_VIDEO);
            totalTemplateInfo.setRouterParams(routerParams);
        }
        // 将本次替换为视频的商品位置及页码记录到redis中
        cacheRedisUtil.setString(rediskey,lastFeedIndex+":"+lastProductIndex+":"+advertNum,TIME_30MIN);
    }

    /**
     * 计算该页面第一个被替换成商品组的位置
     * @param positionAndPageIndex 上一页面的最后一个商品组的位置和页数
     * @param productGroupLimit 商品组之间最小间隔
     * @param feedIndex 当前页面数
     * @return
     */
    private  Integer calculateFirstPosition(String positionAndPageIndex,int productGroupLimit,Integer feedIndex){
        int firstPosition = 0;
        try {
            if (!StringUtil.isBlank(positionAndPageIndex)) {
                String[] positionAndPageIndexArr = positionAndPageIndex.split(":");
                int position = Integer.valueOf(positionAndPageIndexArr[0]);
                //当前页面和缓存中的页面之间的商品数
                int productNum = (feedIndex - Integer.valueOf(positionAndPageIndexArr[1])) * 20;
                firstPosition = productGroupLimit - (productNum - position) + 1;
            }
        } catch (Exception e) {
            log.error("[严重异常]计算当前页面首个被替换商品组的位置异常，异常信息", e);
        }
        return firstPosition > 0 ? firstPosition : 0;
    }
}






