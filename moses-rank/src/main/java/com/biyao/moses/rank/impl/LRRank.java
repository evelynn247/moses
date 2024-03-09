package com.biyao.moses.rank.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.biyao.moses.cache.LRFeatureCache;
import com.biyao.moses.cache.ProductDetailCache;
import com.biyao.moses.cache.ProductFeaCache;
import com.biyao.moses.common.constant.RedisKeyConstant;
import com.biyao.moses.model.feature.OnlineParseFeaConf;
import com.biyao.moses.model.feature.Sparse01Vector;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;
import com.biyao.moses.rank.RankRequest;
import com.biyao.moses.rank.RecommendRank;
import com.biyao.moses.util.OnlineParseUtil;
import com.biyao.moses.util.RedisUtil;
import com.biyao.moses.params.ProductInfo;
import com.by.profiler.annotation.BProfiler;
import com.by.profiler.annotation.MonitorType;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.rmi.server.UID;
import java.util.*;

/**
 * LR算法排序
 */
@Slf4j
@Component("LRRank")
public class LRRank implements RecommendRank {

    @Autowired
    RedisUtil redisUtil;
    @Autowired
    LRFeatureCache lrFeatureCache; //LR特征缓存
    @Autowired
    ProductFeaCache productFeaCache; //商品特征缓存
    @Autowired
    ProductDetailCache productDetailCache;//商品详情缓存
    //连接特征名和特征值
    private static final String FEAK_AND_FEAV_JOINER = "=";

    @BProfiler(key = "LRRank.executeRecommend", monitorType = {
            MonitorType.TP, MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR })
    @Override
    public List<TotalTemplateInfo> executeRecommend(RankRequest rankRequest) {
        //参数、数据准备
        String uuid = rankRequest.getUuid();
        String uid = rankRequest.getUid();
        String siteId = rankRequest.getSiteId();
        List<TotalTemplateInfo> productList = rankRequest.getOriData();
        log.error("@@@shangpin : {}" , JSON.toJSONString(productList));
        String productLog = "";
        for (TotalTemplateInfo testItem:productList) {
            productLog  =productLog + testItem.getId()+",";
        }
        log.error("@@@@@"+"访问者"+"uuid:  "+uuid+"uid:  "+uid +"siteid   :"+ siteId);
        log.error("@@@@入参信息为："+"  uuid:"+uuid+"  uid:"+uid+"   siteid:"+siteId);
        log.error("@@@@输入商品集合为："+productLog);

        //在线特征文件
        List<OnlineParseFeaConf> onlineParseFealist = lrFeatureCache.getOnlineConfList();
        //容错
        if (CollectionUtils.isEmpty(productList)) {
            return rankRequest.getOriData();
        }
        try {
            //获取uid、uuid、商品 特征
            JSONObject uidFeatures = getUidFeature(uid);
            JSONObject uuidFeatures = getUuidFeature(uuid);
            JSONObject productFeaJson = productFeaCache.getProductFeaJson();
            //处理用户特征
            Sparse01Vector uidFeaVector = new Sparse01Vector();
            dealWithNormalFeature(uidFeaVector, uidFeatures);
            //处理访客特征
            Sparse01Vector uuidFeaVector = new Sparse01Vector();
            dealWithNormalFeature(uuidFeaVector, uuidFeatures);

            for (TotalTemplateInfo item : productList) {
                //容错
                if (item == null || StringUtils.isBlank(item.getId())) {
                    continue;
                }
                //用于分值计算的默认值
                double score = 0.0;
                Long pid = Long.parseLong(item.getId());
                //先存入商品记录
                Sparse01Vector proFeaVector = new Sparse01Vector();
                ProductInfo pInfo = productDetailCache.getProductInfo(pid);
                // 商品普通特征
                JSONObject proFeatures = productFeaJson.getJSONObject(String.valueOf(pid));
                if (proFeatures == null) {
                    log.info("缓存中缺少该商品的特征，商品id: {}", pid);
                    item.setScore(score);
                    continue;
                }
                //处理商品普通特征
                dealWithNormalFeature(proFeaVector, proFeatures);
                //线上特征计算
                dealPersonAndProductFeature(siteId, onlineParseFealist, uidFeatures, uuidFeatures, pid, proFeaVector, pInfo, proFeatures);
                //加入用户和访客特征
                proFeaVector.addAll(uidFeaVector).addAll(uuidFeaVector);

                if (proFeaVector != null && proFeaVector.getIndices1().size() > 0) {
                    score = predict(proFeaVector);
                }
                item.setScore(score);
            }

            //得分降序排序
            Collections.sort(productList, new Comparator<TotalTemplateInfo>() {
                @Override
                public int compare(TotalTemplateInfo o1, TotalTemplateInfo o2) {
                    return o2.getScore().compareTo(o1.getScore());
                }
            });
            String productLog2 = "";
            for (TotalTemplateInfo testItem:productList) {
                productLog2  =productLog2 + testItem.getId()+":"+testItem.getScore()+";";
            }
            log.error("@@@@经过LR算法排序后的商品以及分数为 ： "+productLog2);
            return productList;
        } catch (Exception e) {
            log.error("LR特征排序出错,", e);
        }
        return rankRequest.getOriData();
    }

    /**
     * @param feature 稀疏化的特征向量
     * @return 预测值
     */
    public double predict(Sparse01Vector feature) {
        double sumv = lrFeatureCache.getIntercept();
        double[] coefs = lrFeatureCache.getCoefs();
        // 获取特征为1的索引位置
        for (Integer index : feature.getIndices1()) {
            if (index > coefs.length) {
                log.error("特征索引超出: {}", feature.getIndices1());
                continue;
            }
            sumv += coefs[index];
        }

        double score = 1.0 / (1 + Math.pow(Math.E, 0 - sumv));
        return score;
    }

    /**
     * 特征计算
     *
     * @param siteId 站点
     * @param onlineParseFealist 特征函数配置文件
     * @param uidFeatures 用户特征文件
     * @param uuidFeatures 访客特征文件
     * @param pid 商品ID
     * @param proFeaVector
     * @param pInfo 商品详情集合
     * @param proFeatures
     * @throws Exception
     */
    private void dealPersonAndProductFeature(String siteId, List<OnlineParseFeaConf> onlineParseFealist,
                                             JSONObject uidFeatures, JSONObject uuidFeatures,
                                             Long pid, Sparse01Vector proFeaVector,
                                             ProductInfo pInfo, JSONObject proFeatures) throws Exception {
        /**
         * 在线特征
         */
        Map<String, Object> tempValues = Maps.newHashMap();//保存在线特征解析计算出的中间值
//            tempValues.put("p", province);
        tempValues.put("sid", pInfo.getSupplierId());
        tempValues.put("pid", pid);
        tempValues.put("pf", siteId);
        tempValues.put("pc1", pInfo.getFirstCategoryId());
        tempValues.put("pc2", pInfo.getSecondCategoryId());
        tempValues.put("pc3", pInfo.getThirdCategoryId());

        for (OnlineParseFeaConf conf : onlineParseFealist) {
            String feaName = conf.getFeaName();
            String formula = conf.getFormula();
            if (formula.startsWith("split_")) {
                String paramName = conf.getParamA();
                Object valueObject = getFeaParamValueByName(paramName, tempValues, uidFeatures, uuidFeatures, proFeatures);
                Double originalValue = valueObject == null ? null : Double.valueOf(valueObject.toString());
                Integer feaValue = OnlineParseUtil.split(paramName, originalValue, lrFeatureCache.getThreshold(feaName));

                handleFeaValue(feaName, feaValue, Integer.valueOf(conf.getDefaultValue()), tempValues, proFeaVector, conf.getNeedCalcu());
            } else if (formula.startsWith("count_isin")) {
                String paramAName = conf.getParamA();
                String paramBName = conf.getParamB();

                Object paramAValue = getFeaParamValueByName(paramAName, tempValues, uidFeatures, uuidFeatures, proFeatures);
                Object paramBValue = getFeaParamValueByName(paramBName, tempValues, uidFeatures, uuidFeatures, proFeatures);

                Double feaValue = OnlineParseUtil.countIsin(paramAValue.toString(), (JSONObject) paramBValue);

                handleFeaValue(feaName, feaValue, Double.valueOf(conf.getDefaultValue()), tempValues, proFeaVector, conf.getNeedCalcu());
            } else if (formula.startsWith("clk_order_rate")) {
                String paramAName = conf.getParamA();
                String paramBName = conf.getParamB();

                Double paramAValue = Double.valueOf(getFeaParamValueByName(paramAName, tempValues, uidFeatures, uuidFeatures, proFeatures).toString());
                Double paramBValue = Double.valueOf(getFeaParamValueByName(paramBName, tempValues, uidFeatures, uuidFeatures, proFeatures).toString());

                Double feaValue = OnlineParseUtil.clkOrderRate(paramAValue, paramBValue);

                handleFeaValue(feaName, feaValue, Double.valueOf(conf.getDefaultValue()), tempValues, proFeaVector, conf.getNeedCalcu());
            } else if (formula.contains("+")) {
                String fieldAName = conf.getParamA();
                String fieldBName = conf.getParamB();

                Object fieldAValue = getFeaParamValueByName(fieldAName, tempValues, uidFeatures, uuidFeatures, proFeatures);
                Object fieldBValue = getFeaParamValueByName(fieldBName, tempValues, uidFeatures, uuidFeatures, proFeatures);

                String feaValue = fieldAValue + "_" + fieldBValue;
                handleFeaValue(feaName, feaValue, conf.getDefaultValue(), tempValues, proFeaVector, conf.getNeedCalcu());
            } else if (formula.startsWith("log1_norm")) {
                String paramName = conf.getParamA();
                Double originalValue = (Double) getFeaParamValueByName(paramName, tempValues, uidFeatures, uuidFeatures, proFeatures);

                Double feaValue = OnlineParseUtil.log1Norm(originalValue);
                handleFeaValue(feaName, feaValue, Integer.valueOf(conf.getDefaultValue()), tempValues, proFeaVector, conf.getNeedCalcu());
            } else {
            }
        }
    }

    /**
     * 获取uuid特征
     *
     * @param uuid
     * @return
     */
    private JSONObject getUuidFeature(String uuid) {
        String uuidFeaStr = redisUtil.hget(RedisKeyConstant.MOSES_FEA_VISITOR_FEATURE, uuid);
        JSONObject uuidFeatures = null;
        if (StringUtils.isNotBlank(uuidFeaStr)) {
            uuidFeatures = JSONObject.parseObject(uuidFeaStr);
        }
        return uuidFeatures;
    }

    /**
     * 获取uid特征
     *
     * @param uid
     * @return
     */
    private JSONObject getUidFeature(String uid) {
        String uidFeaStr = redisUtil.hget(RedisKeyConstant.MOSES_FEA_USER_FEATURE, uid);
        JSONObject uidFeatures = null;
        if (StringUtils.isNotBlank(uidFeaStr)) {
            uidFeatures = JSONObject.parseObject(uidFeaStr);
        }
        if (uidFeatures == null) {
            String defaultUidFeaStr = redisUtil.hget(RedisKeyConstant.MOSES_FEA_DEFAULT_UID_FEA, uid);
            uidFeatures = JSONObject.parseObject(defaultUidFeaStr);
        }
        return uidFeatures;
    }

    /**
     * 处理特征
     *
     * @param result
     * @param allFeatures
     */
    private void dealWithNormalFeature(Sparse01Vector result, JSONObject allFeatures) {
        if (allFeatures == null) {
            return;
        }
        Set<String> keys = allFeatures.keySet();
        for (String feaKey : keys) {
            Object object = allFeatures.get(feaKey);
            if (object instanceof String) {
                String value = (String) object;
                getIndexAndAdd2Vector(result, feaKey + FEAK_AND_FEAV_JOINER + value);
            } else if (object instanceof JSONArray) {
                JSONArray valueArray = (JSONArray) object;
                for (int i = 0; i < valueArray.size(); i++) {
                    String value = valueArray.getString(i);
                    getIndexAndAdd2Vector(result, feaKey + FEAK_AND_FEAV_JOINER + value);
                }
            } else if (object instanceof JSONObject) {
                //pass
            }
        }
    }

    /**
     * 返回特征索引
     *
     * @param result
     * @param feature
     */
    private void getIndexAndAdd2Vector(Sparse01Vector result, String feature) {
        Integer idx = lrFeatureCache.getIndex(feature);
        if (idx == null) {
//            log.info("在feamap中未能获取到特征值，feature：{}", feature);
            return;
        }

        result.add(idx);
    }

    /**
     * @param feaName
     * @param feaValue
     * @param defaultValue
     * @param tempValues
     * @param feaVector
     * @param needCalcu
     */
    private void handleFeaValue(String feaName, Object feaValue, Object defaultValue, Map<String, Object> tempValues,
                                Sparse01Vector feaVector, Integer needCalcu) {
        if (feaValue == null) {
            feaValue = defaultValue;
        }

        tempValues.put(feaName, feaValue);

        if (needCalcu == 1) {
            getIndexAndAdd2Vector(feaVector, feaName + FEAK_AND_FEAV_JOINER + feaValue);
        }
    }

    /**
     * 获取特征参数
     *
     * @param paramName
     * @param tempValues
     * @param uidFeatures
     * @param uuidFeatures
     * @param proFeatures
     * @return
     */
    private Object getFeaParamValueByName(String paramName,
                                          Map<String, Object> tempValues, JSONObject uidFeatures,
                                          JSONObject uuidFeatures, JSONObject proFeatures) {
        if (tempValues.containsKey(paramName)) {
            return tempValues.get(paramName);
        } else if (paramName.startsWith("uo")) {//用户特征
            return uidFeatures.get(paramName);
        } else if (paramName.startsWith("uc")) {//访客特征
            return uuidFeatures.get(paramName);
        } else {//商品特征
            return proFeatures.get(paramName);
        }
    }
}
