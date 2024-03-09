package com.biyao.moses.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.biyao.moses.Enum.IndexTypeEnum;
import com.biyao.moses.cache.NewVProductCache;
import com.biyao.moses.cache.ProductSeasonCache;
import com.biyao.moses.cache.ProductSexlabelCache;
import com.biyao.moses.common.constant.CommonConstant;
import com.biyao.moses.config.AlgorithmRedisUtil;
import com.biyao.moses.pdc.domain.ProductDomain;
import com.biyao.moses.po.Video;
import com.biyao.moses.util.CommonUtil;
import com.biyao.moses.util.ESUtils;
import com.by.profiler.util.StringUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsRequest;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.support.ActiveShardCount;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.*;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.cluster.metadata.AliasMetadata;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Stream;

import static com.biyao.moses.common.RediskeyCommonConstant.RECOMMOND_ES_REDISKEY_PREFIX;
import static com.biyao.moses.common.constant.CommonConstant.*;
import static com.biyao.moses.common.constant.EsIndexConstant.*;
import static com.biyao.moses.constant.ElasticSearchConstant.*;
import static com.biyao.moses.constant.RedisKeyConstant.PRODUICT_VEDIO_REDIS;
import static java.util.stream.Collectors.toList;

/**
 * @program: moses-parent-online
 * @description:
 * @author: changxiaowei
 * @create: 2021-09-16 11:16
 **/
@Slf4j
@Service
public class EsConmonService {

    @Autowired
    RestHighLevelClient restHighLevelClient;
    @Value("${env.name}")
    private String envName;
    @Autowired
    ProductSeasonCache  productSeasonCache;
    @Autowired
    ProductSexlabelCache productSexlabelCache;
    @Autowired
    AlgorithmRedisUtil algorithmRedisUtil;
    @Autowired
    NewVProductCache newVProductCache;
/**
 *
 * @Des
 * @Param [indexType] 索引类型
 * @return java.lang.String 创建的indexName
 * @Author changxiaowei
 * @Date  2021/12/17
 */
    public String  creatIndex(String indexType){

        String newIndex ="";
        XContentBuilder mapping =null;
        try {
            // 根据索引类型 创建索引名字和对应的mapping
            if(IndexTypeEnum.PRODUCT.getType().equals(indexType)){
                 newIndex = PRODUCT_INDEX_PREFIX  +getEnvName()+"_"+System.currentTimeMillis();
                 mapping = ESUtils.getProductMapping();
            }
            if(mapping == null){
                log.error("[严重异常]创建mapping异常");
                return null;
            }
            //将settings和mappings封装到Resquest对象中1
            CreateIndexRequest request = new CreateIndexRequest(newIndex)
                    .settings(ESUtils.getSettings())
                    .mapping(mapping);
            // 默认30s
            request.setTimeout(TimeValue.timeValueMinutes(2));//超时,等待所有节点被确认(使用TimeValue方式)
            request.setMasterTimeout(TimeValue.timeValueMinutes(1));//连接master节点的超时时间 默认 30s
            request.waitForActiveShards(ActiveShardCount.from(1));//在创建索引API返回响应之前等待的活动分片副本的数量
            CreateIndexResponse response = restHighLevelClient.indices().create(request, RequestOptions.DEFAULT);
            if(response == null || !response.isAcknowledged() ||!response.isShardsAcknowledged()){
                log.error("[严重异常]创建索引失败，响应：{}",JSONObject.toJSONString(response));
                return null;
            }
        }catch (Exception e){
            log.error("[严重异常]创建索引异常，错误原因：",e);
            return null;
        }
        return newIndex;
    }


    /**
     * 根据别名获取指向的所有索引
     * @param aliasName
     * @return
     */
   public  Set<String> getIndexByAlias(String aliasName){
       Set<String> resultSet = new HashSet<>();
       GetIndexRequest getIndexRequest = new GetIndexRequest(aliasName);
       try {
           boolean exists = restHighLevelClient.indices().exists(getIndexRequest, RequestOptions.DEFAULT);
           if(!exists){
               return resultSet;
           }
           Request request = new Request("GET","/_alias/"+aliasName);
           Response response = restHighLevelClient.getLowLevelClient().performRequest(request);
           //response.hasWarnings()
           if(response == null || response.hasWarnings()){
               log.error("[严重异常]根据别名获取指向的所有索引时有异常告警，告警信息：{}",JSONObject.toJSONString(response));
               return resultSet;
           }
           String resultStr = EntityUtils.toString(response.getEntity());
           if (StringUtils.isEmpty(resultStr)){
               log.error("[严重异常]根据别名获取指向的所有索引时结果为空,resultStr:{}",resultStr);
               return resultSet;
           }
           ObjectMapper objectMapper = new ObjectMapper();
           Map<String,Object> map = objectMapper.readValue(resultStr, Map.class);
           resultSet  = map.keySet();
       }catch (Exception e){
           log.error("[严重异常]根据别名获取全部索引时异常，索引别名：{}，原因：",aliasName,e);
       }
        return resultSet;
    }

    /**
     * @Des 更新索引别名指向
     * @Param  ndexName 索引名字 alias 别名 method 操作方式  移除 remove 添加为add
     * @return
     * @Author changxiaowei
     * @Date  2021/12/17
     */
  public void  updateALias(String indexName,String alias,String method) {
      Map<String,Object> deleteAlias=new HashMap<>();
      Map<String,String> methodMap=new HashMap<>();
      methodMap.put("index",indexName);
      methodMap.put("alias",alias);
      deleteAlias.put(method,methodMap);
      Object[] actions=new Object[1];
      actions[0]=deleteAlias;
      Map<String,Object> params=new HashMap<>();
      params.put("actions",actions);
      log.info("params={}",JSONObject.toJSONString(params));
      try {
          //如果是解除别名和索引的关系
          if ("remove".equals(method)) {
              GetIndexRequest getIndexRequest = new GetIndexRequest(alias);
              // 先检查下是否有此别名
              boolean exists = restHighLevelClient.indices().exists(getIndexRequest, RequestOptions.DEFAULT);
              log.info("[检查日志]删除索引别名前查询索引别名{}是否存在{}", alias, exists);
              if (!exists) return ;
          }
          Request request = new Request("POST", "/_aliases");
          HttpEntity entity = new NStringEntity(JSONObject.toJSONString(params), ContentType.APPLICATION_JSON);
          request.setEntity(entity);
          Response response = restHighLevelClient.getLowLevelClient().performRequest(request);
          if(response == null || response.hasWarnings()){
              log.error("[严重异常]更新别名指向时有异常告警，告警信息：{}",JSONObject.toJSONString(response));
          }
      }catch (Exception e){
        log.error("[严重异常]更新别名指向异常，indexName:{},alias:{},method:{},异常信息：{}",indexName, alias, method,e);
      }
   }


   private Map<String,Object> getRedisData(HashMap map,Long productId){
       Map<String,Object> result = new HashMap<>();
       float hotScore = CommonConstant.ZERO;
       result.put(HOT_SCORE,hotScore);
       if (!CollectionUtils.isEmpty(map)) {
           // 热门分
           Object hotScoreO = map.get(HOT_SCORE);
           if (!Objects.isNull(hotScoreO)) {
               hotScore = Float.valueOf(hotScoreO.toString());
           } else {
               log.error("[严重异常]热门分格式错误,hotscore:{}", JSONObject.toJSONString(hotScoreO));
           }
           result.put(HOT_SCORE,hotScore);
           // fmVector
           Object fmVectorO = map.get(FM_VECTOR);
           if (!Objects.isNull(fmVectorO)) {
               String[] fmVectorArr = fmVectorO.toString().split(",");
               if (fmVectorArr.length == 8) {
                 float[]  fmVector = com.biyao.moses.common.utils.CommonUtil .StringArrToFloat(fmVectorArr);
                 result.put(FM_VECTOR,fmVector);
               } else {
                   log.error("[严重异常]fmVector向量维度异常,fmVector:{}", JSONObject.toJSONString(fmVectorArr));
               }
           }
           // icfVector
           Object icfVectorO = map.get(ICF_VECTOR);
           if (!Objects.isNull(icfVectorO)) {
               String[] icfVectorArr = icfVectorO.toString().split(",");
               if (icfVectorArr.length == 16) {
                   float[]  icfVector = com.biyao.moses.common.utils.CommonUtil.StringArrToFloat(icfVectorArr);
                   result.put(ICF_VECTOR,icfVector);
               } else {
                   log.error("[严重异常]icfVector向量维度异常,icfVector:{}", JSONObject.toJSONString(icfVectorArr));
               }
           }
       } else {
           log.error("[一般异常]从算法redis集群中获取向量和热门分结果为空，productId:{}",productId);
       }
       return result;
   }
    /**
     * 构建批量更新索引request
     * @param productList 待组装的商品idlist
     * @param indexName 索引名字
     * @return
     */
    public BulkRequest buildProductBulkRequest(List<ProductDomain> productList, String indexName){
       BulkRequest request = new BulkRequest();
        if(CollectionUtils.isEmpty(productList)){
            return request;
        }
        // 构建rediskey  批量查询redis中的向量和热门分
        List<String> rediskeyList = new ArrayList<>();
        String[] pidArr = new String[productList.size()];
        for (int i = 0; i < productList.size(); i++) {
            Long productId = productList.get(i).getProductId();
            rediskeyList.add(RECOMMOND_ES_REDISKEY_PREFIX + productId);
            pidArr[i]=productId.toString();
        }
//        查询redis中数据（商品向量、热门分）
//        createTestData(pidArr);
        Map<String, Object> pipelineResult = algorithmRedisUtil.pipelineHgetAll(rediskeyList);
        List<String> pidVideo = algorithmRedisUtil.hmget(PRODUICT_VEDIO_REDIS, pidArr);
        if(newVProductCache.isNewVProductCacheNull()){
            newVProductCache.refreshNewProductCache();
        }
        // 将结果解析成map的形式
        for (int i = 0; i < productList.size(); i++) {
            ProductDomain product = productList.get(i);
            try {
                request.add(new IndexRequest(indexName).id(product.getProductId().toString())
                        .source(XContentType.JSON,buildEsProductInfo(product,pipelineResult,pidVideo.get(i))));
                log.info("[检查日志]更新es中的商品信息，productInfo:{}", JSONObject.toJSONString(product));
            } catch (Exception e) {
                log.error("[严重异常]构建批量更新索引request异常，商品信息:{},异常信息：",
                        JSONObject.toJSONString(product), e);
            }
        }
        return request;
    }

    /**
     * 构建带更新的es信息
     * @return
     */
    private Object[] buildEsProductInfo(ProductDomain product, Map<String, Object> pipelineResult,String pidVideo){
        HashMap map = (HashMap) pipelineResult.get(RECOMMOND_ES_REDISKEY_PREFIX + product.getProductId());
        Map<String, Object> redisData = getRedisData(map, product.getProductId());
        List<Byte> season = productSeasonCache.getProductSeasonValue(product.getProductId());
        String sex = productSexlabelCache.getProductSexLabel(product.getProductId().toString());
        Set<Byte> channelSet = getvidsChannel(pidVideo);

        List<Object> objects = Stream.of(
                PRODUCT_ID, product.getProductId(),
                FIRST_SHELLF_TIME, product.getFirstOnshelfTime(),
                SHORT_TITLE, product.getShortTitle(),
                SHELF_STATUS, product.getShelfStatus(),
                SHOW_STATUS, product.getShowStatus(),
                SUPPORT_PLATFORM, CommonUtil.StringArrToByte(product.getSupportPlatform()),
                SUPPORT_ACT, CommonUtil.StringArrToByte(product.getSupportAct()),
                CATEGORY3ID, product.getThirdCategoryId(),
                F_CATEGORY1ID, CommonUtil.StringArrToLong(product.getFcategory1Ids().split(",")),
                F_CATEGORY3ID, CommonUtil.StringArrToLong(product.getFcategory3Ids().split(",")),
                IS_CREATOR, product.getIsCreator(),
                NEW_PRIVILEGE, product.getNewUserPrivilege(),
                NEW_PRIVILATE_DEDUCT, product.getNewPrivilateDeduct(),
                SUPPORT_TEXTURE, product.getSupportTexture(),
                TAGSID, StringUtils.isEmpty(product.getTagsId()) ? DEFAULARR : CommonUtil.StringArrToLong(product.getTagsId().split(",")),
                CATEGORY2ID, product.getSecondCategoryId(),IS_TOGGROUP, product.getIsToggroupProduct(),
                HOT_SCORE, redisData.get(HOT_SCORE),
                NEWV_PRODUCT,newVProductCache.isNewVProduct(product.getProductId()) ?(byte)ONE :(byte)ZERO,
                SEASON, season, SEX, sex,
                IS_VIDEO,CollectionUtils.isEmpty(channelSet) ?(byte)ZERO :(byte)ONE,
                VID_SUPPORT_PALTFORM,channelSet.toArray(new Byte[0]),
                SUPPORT_CHANNEL,CommonUtil.StringArrToByte(product.getSupportChannel())
                ).collect(toList());
        if(redisData.containsKey(FM_VECTOR)){
                    objects.add(FM_VECTOR);
                    objects.add(redisData.get(FM_VECTOR));
        }
        if(redisData.containsKey(ICF_VECTOR)){
            objects.add(ICF_VECTOR);
            objects.add(redisData.get(ICF_VECTOR));
        }
        return objects.toArray(new Object[0]);
    }


    public Set<Byte> getvidsChannel(String videoInfo){
        Set<Byte> result = new HashSet<>();
        if(StringUtil.isBlank(videoInfo)){
            return result;
        }
        try {
            List<Video> videoList = JSONArray.parseArray(videoInfo, Video.class);
            //去除无效的视频
            for (Video video : videoList) {
                if(!Video.isValid(video)){
                    videoList.remove(video);
                }
            }
            if(CollectionUtils.isEmpty(videoList)){
                return result;
            }
            for (Video video : videoList) {
                result.addAll(video.getChannel());
            }
        }catch (Exception e){
            log.error("[严重异常]获取商品支持的视频支持的渠道时出现异常,视频信息:{},异常信息:{}",videoInfo,e);
        }
        return result;
    }



    /**
     * @Des 批量更新索引
     * @Param [request]
     * @return void
     * @Author changxiaowei
     * @Date  2021/12/16
     */
   public void blukUpdateDoc(BulkRequest request){
        try {
            BulkResponse bulkResponse = restHighLevelClient.bulk(request, RequestOptions.DEFAULT);
            // 如果失败 则需要找出具体哪个数据失败  打印日志
            if (bulkResponse.hasFailures()) {
                StringBuilder sb = new StringBuilder();
                for (BulkItemResponse bulkItemResponse : bulkResponse) {
                    //指示给定操作是否失败
                    if (bulkItemResponse.isFailed()) {
                        //检索失败操作的失败
                        BulkItemResponse.Failure failure = bulkItemResponse.getFailure();
                        sb.append(failure.toString()).append("\n");
                    }
                }
                log.error("[严重异常]批量更新es中的数据失败，信息：{}",sb.toString());
            }
        }catch (Exception e){
            log.error("[严重异常]批量更新es中的数据异常，参数：{}，异常信息：",JSONObject.toJSONString(request),e);
        }
    }

    /**
     * 获取所有的索引
     * @return
     */
    public Set<String> getAllIndex(){
       Set<String> indexSet = new HashSet<>();
       try {
           GetAliasesRequest request = new GetAliasesRequest();
           GetAliasesResponse getAliasesResponse = restHighLevelClient.indices().getAlias(request, RequestOptions.DEFAULT);
           Map<String, Set<AliasMetadata>> map = getAliasesResponse.getAliases();
           indexSet = map.keySet();
       }catch (Exception e){
           log.error("[严重异常]获取所有索引异常，原因：",e);
       }
        return indexSet;
    }

    /**
     * 删除索引  支持多个
     * @param index
     */
    public void deleteIndex(String[] index){
        //准备request对象
        DeleteIndexRequest request = new DeleteIndexRequest(index);
        try {
            //通过client连接es
            AcknowledgedResponse delete = restHighLevelClient.indices().delete(request, RequestOptions.DEFAULT);
            if (!delete.isAcknowledged()) {
                log.error("[严重异常]删除索引失败，索引名字：{}",JSONObject.toJSONString(index));
            }
        }catch (Exception e){
            log.error("[严重异常]删除索引异常，索引名字：{},异常信息",JSONObject.toJSONString(index),e);
        }
    }

    /**
     * 根据索引别名 获取最新的索引
     * 预防别名指向多个索引出现问题
     */
    public String getLatestIndexNameByAlais(String aliasName){
        String indexName ="";
        if(StringUtils.isEmpty(aliasName)) return indexName;
        // 获取当前别名指向的所有索引
        Set<String> indexSet = getIndexByAlias(aliasName);
        long indexCreateTime = 0;
        if(CollectionUtils.isEmpty(indexSet)){
            log.error("[严重异常]索引别名指向的索引个数为空,aliasName:{}",aliasName);
            return indexName;
        }
        for (String index : indexSet) {
            if(!index.startsWith(PRODUCT_INDEX_PREFIX)){
                continue;
            }
            String[] split = index.split("_");
            try {
                if(Long.valueOf(split[split.length - 1]) > indexCreateTime) {
                    indexCreateTime = Long.valueOf(split[split.length - 1]);
                    indexName = index;
                }
            }catch (Exception e) {
                log.error("[严重异常] es中商品索引格式错误，索引：{}",index);
            }
        }
      if(indexSet.size()>1){
          log.error("[严重异常]索引别名指向的索引个数不为1，索引：{},默认取最新的索引:{}", JSONObject.toJSONString(indexSet),indexName);
      }
     return indexName;
    }


    public String getEnvName(){
        return envName.toLowerCase();
    }


    public void createTestData(String[] pids){
//        Map<String, String> map = new HashMap<>();
//        Random r = new Random(1);
//        int max=2,min=1;
//
//        for (int i = 0; i < pids.length;) {
//            int ran1 = (int) (Math.random()*(max-min)+min);
//            Video v1 = new Video(3,r.nextFloat(),r.nextInt(100),"2022-02-15 14:30:55.006",1);
//            Video v2 = new Video(5,r.nextFloat(),r.nextInt(100),"2022-02-14 14:30:55.006",2);
//            Video v3 = new Video(3,r.nextFloat(),r.nextInt(100),"2022-02-16 14:30:55.006",3);
//            List<Video> list = new ArrayList<>();
//            list.add(v1);list.add(v2);list.add(v3);
//            map.put(pids[i],JSONObject.toJSONString(list));
//            i= i+ran1;
//        }
//        algorithmRedisUtil.del(PRODUICT_VEDIO_REDIS);
//        algorithmRedisUtil.hmset(PRODUICT_VEDIO_REDIS,map);
//        algorithmRedisUtil.expire(PRODUICT_VEDIO_REDIS,60*60*24*7);
    }

    /**
     * 更新es setting
     * @param key
     * @param value
     */
   public void updateSetting(String key,String value){
        String aliasName = PRODUCT_INDEX_ALIAS + getEnvName();
        Settings settings= Settings.builder().put(key,value).build();
        UpdateSettingsRequest updateSettingsRequest = new UpdateSettingsRequest(settings,aliasName);
            try {
                restHighLevelClient.indices().putSettings(updateSettingsRequest, RequestOptions.DEFAULT);
            }catch (Exception e){
                System.out.println(e);
            }
    }
}
