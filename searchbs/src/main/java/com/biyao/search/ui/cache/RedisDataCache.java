package com.biyao.search.ui.cache;


import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.biyao.search.ui.constant.RedisKeyConsts;
import com.biyao.search.ui.model.PanelConfig;
import com.biyao.search.ui.rest.response.SearchOrderBy;
import com.biyao.search.ui.rest.response.SearchOrderByConsts;
import com.biyao.search.ui.util.RedisUtil;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;

/**
 * @author zhangjian
 * @date 2019/9/18
 **/
@Component
@Slf4j
public class RedisDataCache {

    @Autowired
    private RedisUtil redisUtil;
    /**
     * 标签配置信息
     */
    @Getter
    private JSONObject labelConfig;
    /**
     * 面板配置信息
     */
    @Getter
    private PanelConfig panelConfig;

    /**
     * 普通商品间隔数量
     */
    @Getter
    private int commonProductInterval;

    /**
     * 衍生商品召回数量
     */
    @Getter
    private int deriveProductSize;

    /**
     * 展示衍生商品用户uid白名单
     */
    @Getter
    private List<String> deriveWhiteList = new ArrayList<>();

    /**
     * 特殊口罩商品
     */
    @Getter
    private List<String> maskList = new ArrayList<>();

    /**
     * 特殊口罩商品
     */
    @Getter
    private String maskContent = "";

    /**
     * 梦工厂流量分流左区间
     */
    @Getter
    private Integer flowLimitL = 0;

    /**
     * 梦工厂流量分流右区间
     */
    @Getter
    private Integer flowLimitR = 0;

    /**
     * 搜索召回日志白名单
     */
    private Set<String> searchDetailLogList = new HashSet<>();

    /**
     * 找相似开关
     */
    @Getter
    private Boolean similarProductFlag = false;

    /**
     * 禁止搜索的query词集合
     */
    private Set<String> blockQuerySet = new HashSet<>();


    @PostConstruct
    public void init(){
    	log.info("[任务报告]系统初始化同步Redis数据到本地缓存--》start");
        refresh();
        log.info("[任务报告]系统初始化同步Redis数据到本地缓存--》end");
    }

    public void refresh(){
        refreshBlockQuery();
        refreshLabelConfigData();
        refreshSearchPanelConfig();
        refreshCommonProductInterval();
        refreshDeriveProductSize();
        refreshDeriveWhiteList();
        refreshMaskList();
        refreshFlowLimit();
        refreshSearchDetailLogList();
        refreshSimilarProductFlag();
     }
    /**
     * 禁止搜索词更新
     * @return
     */
    private void refreshBlockQuery() {
        try {
            Set<String> tempBlockQuerySet = new HashSet<>();
            String temp = redisUtil.getString(RedisKeyConsts.BLOCK_QUERY);
            if (StringUtils.isNotEmpty(temp)) {
                String[] arr = temp.split(",");
                for (int i = 0; i < arr.length; i++) {
                    if (StringUtils.isEmpty(arr[i])) {
                        continue;
                    }
                    tempBlockQuerySet.add(arr[i].toUpperCase());
                }
            }
            blockQuerySet = tempBlockQuerySet;
        } catch (Exception e) {
            log.error("[严重异常]从redis中获取禁止搜索的query集合时，redis.key={}，发生异常:", RedisKeyConsts.BLOCK_QUERY, e);

        }
    }

    /**
     * 判断用户输入query是否是禁止搜索的query
     *
     * @param query
     * @return true：不允许搜索，false：可以搜索
     */
    public boolean isBlockQuery(String query) {
        if (StringUtils.isEmpty(query)) {
            return false;
        }
        if (blockQuerySet.contains(query.toUpperCase())) {
            log.info("[操作日志]搜索词：{}在query黑名单中，不进行搜索召回", query);
            return true;
        }
        return false;
    }
    /**
     * 获取找相似商品开关
     * @return
     */
    private void refreshSimilarProductFlag() {
        try {
            String temp = redisUtil.getString(RedisKeyConsts.SIMILAR_PRODUCT_FLAG);
            if(StringUtils.isNotBlank(temp)){
                similarProductFlag = "1".equals(temp);
            }
        } catch (Exception e) {
            log.error("找相似商品开关获取,key:{},异常:", RedisKeyConsts.SIMILAR_PRODUCT_FLAG, e);
        }
    }

    /**
     * 获取搜索召回日志白名单（uuid）
     * @return
     */
    private void refreshSearchDetailLogList(){
        try {
            log.info("[操作日志]redis白名单缓存刷新开始");
            Set<String> uuidWhiteListSetTemp = new HashSet<>();
            String uuidStr = redisUtil.getString(RedisKeyConsts.SEARCH_DETAIL_LOG_LIST);
            if (org.apache.commons.lang.StringUtils.isNotBlank(uuidStr)) {
                String[] uuidArray = uuidStr.split(",");
                for (String uuid : uuidArray) {
                    if (org.apache.commons.lang.StringUtils.isNotBlank(uuid)) {
                        uuidWhiteListSetTemp.add(uuid);
                    }
                }
            }
            if(uuidWhiteListSetTemp.size() > 0){
                searchDetailLogList = uuidWhiteListSetTemp;
                log.info("[操作日志]redis白名单缓存刷新结束, 白名单：{}", searchDetailLogList);
            }
        } catch (Exception e) {
            log.error("[严重异常][redis异常]redis白名单缓存刷新异常，key:{}", RedisKeyConsts.SEARCH_DETAIL_LOG_LIST, e);
        }
    }

    /**
     * 梦工厂流量分流
     * @return
     */
    private void refreshFlowLimit(){
        try {
            String temp = redisUtil.getString(RedisKeyConsts.FLOW_LIMIT);
            if(temp!=null){
                String[] arr = temp.split(",");
               if(arr.length == 2){
                   flowLimitL = Integer.valueOf(arr[0]);
                   flowLimitR = Integer.valueOf(arr[1]);
               }
            }
        }catch (Exception e){
            log.error("[严重异常]从redis中获取梦工厂流量分流时，redis.key={}，发生异常:", RedisKeyConsts.FLOW_LIMIT, e);

        }
    }

    /**
     * 获取口罩商品列表（支持特殊搜索口罩需求）
     * @return
     */
    private void refreshMaskList(){
        try {
            List<String> tempList = redisUtil.lrange(RedisKeyConsts.MASK_LIST,0,-1);
            if(tempList==null){
                this.maskList = new ArrayList<>();
            }else{
                this.maskList = tempList;
            }
        }catch (Exception e){
            log.error("[严重异常]从redis中获取口罩商品id集合时，redis.key={}，发生异常:", RedisKeyConsts.MASK_LIST, e);

            this.maskList = new ArrayList<>();
        }
    }

    /**
     * 衍生商品展示白名单
     */
    private void refreshDeriveWhiteList(){
        try {
            List<String> tempList = redisUtil.lrange(RedisKeyConsts.DERIVEPRODUCT_WHITELIST,0,-1);
            if(tempList==null){
                this.deriveWhiteList = new ArrayList<>();
            }else{
                this.deriveWhiteList = tempList;
            }
        }catch (Exception e){
            log.error("[严重异常]从redis中获取衍生商品展示白名单时，redis.key={}，发生异常:", RedisKeyConsts.DERIVEPRODUCT_WHITELIST, e);

            this.deriveWhiteList = new ArrayList<>();
        }

    }

    /**
     * 普通商品间隔数量(默认为5)
     */
    private void refreshCommonProductInterval() {
        try {
            String redisStr = redisUtil.getString(RedisKeyConsts.COMMONPRODUCT_INTERVAL);
            Integer temp = 5;
            if(!Strings.isNullOrEmpty(redisStr)){
                if(Integer.valueOf(redisStr) > 0 && Integer.valueOf(redisStr)<= 100){
                    temp = Integer.valueOf(redisStr);
                }
            }
            this.commonProductInterval = temp;
        }catch (Exception e){
            log.error("[严重异常]从redis中获取普通商品间隔数量时，redis.key={}，发生异常:", RedisKeyConsts.COMMONPRODUCT_INTERVAL, e);

            this.commonProductInterval = 5;
        }
    }

    /**
     * 衍生商品最大召回数量(默认为50)
     */
    private void refreshDeriveProductSize() {
        try {
            String redisStr = redisUtil.getString(RedisKeyConsts.DERIVEPRODUCT_SIZE);
            Integer temp = 50;
            if(!Strings.isNullOrEmpty(redisStr)){
                if(Integer.valueOf(redisStr) >= 0 && Integer.valueOf(redisStr)<=1000){
                    temp = Integer.valueOf(redisStr);
                }
            }
            this.deriveProductSize = temp;
        }catch (Exception e){
            log.error("[严重异常]从redis中获取衍生商品最大召回数量时，redis.key={}，发生异常:", RedisKeyConsts.DERIVEPRODUCT_SIZE, e);
            this.deriveProductSize = 50;
        }
    }

    /**
     * 商品标签配置信息缓存
     */
    private void refreshLabelConfigData() {
        try {
            String labelConfigStr = redisUtil.getString(RedisKeyConsts.LABEL_CONFIG_CACHE);
            this.labelConfig = Strings.isNullOrEmpty(labelConfigStr) ? new JSONObject()
                    : JSONObject.parseObject(labelConfigStr);
        }catch (Exception e){
            log.error("[严重异常]从redis中获取商品标签配置信息时，redis.key={}，发生异常:", RedisKeyConsts.LABEL_CONFIG_CACHE, e);
            this.labelConfig = new JSONObject();
        }
    }

    /**
     * 搜索面板配置缓存
     */
    private void refreshSearchPanelConfig(){
        try{
            String panelConfigStr = redisUtil.getString(RedisKeyConsts.SEARCH_PANEL_CONFIG);
            if (StringUtils.isNotBlank(panelConfigStr)){
                JSONObject panelConfigJson = JSONObject.parseObject(panelConfigStr);
                Integer onOff = "off".equals(panelConfigJson.getString("onOff")) ? 0 : 1;
                Integer bottomHanging = panelConfigJson.getIntValue("bottomHanging");
                String showStyle = panelConfigJson.getString("showStyle");
                JSONArray orderByConfig = panelConfigJson.getJSONArray("orderByList");
                final List<SearchOrderBy> orderByList = new ArrayList<>();
                if (orderByConfig != null && orderByConfig.size() > 0) {
                    orderByConfig.forEach(orderByName -> {orderByList.add(SearchOrderByConsts.getSearchOrderBy(orderByName.toString()));});
                }else{
                    orderByList.addAll(Lists.newArrayList(SearchOrderByConsts.NORMAL, SearchOrderByConsts.SALE,SearchOrderByConsts.NEW,SearchOrderByConsts.PRICE));
                }

                this.panelConfig = PanelConfig.builder()
                        .onOff(onOff)
                        .bottomHanging(bottomHanging)
                        .showStyle(showStyle)
                        .searchOrderByList(orderByList)
                        .build();
            }
        }catch (Exception e){
            log.error("[严重异常]从redis中获取搜索面板配置信息时，redis.key={}，发生异常:", RedisKeyConsts.SEARCH_PANEL_CONFIG, e);
            this.panelConfig = new PanelConfig();
        }
    }

    /**
     * 验证uuid是否为搜索流程日志白名单用户
     * @param uuid
     * @return
     */
    public boolean isSearchDetailLogUuid(String uuid){
        if(StringUtils.isBlank(uuid)){
            return false;
        }
        return searchDetailLogList.contains(uuid);
    }

}
