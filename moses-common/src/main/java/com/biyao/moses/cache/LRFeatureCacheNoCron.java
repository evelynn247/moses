package com.biyao.moses.cache;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.biyao.moses.common.constant.RedisKeyConstant;
import com.biyao.moses.model.feature.OnlineParseFeaConf;
import com.biyao.moses.util.RedisUtil;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

/**
 * LR算法特征缓存
 */
@Slf4j
public class LRFeatureCacheNoCron {

    @Autowired
    private RedisUtil redisUtil;
    //key:特征名称  value:索引位置
    private Map<String, Integer> feaMap = new HashMap<String, Integer>();
    //在线特征配置集合
    private List<OnlineParseFeaConf> onlineConfList = new ArrayList<>();
    //key:特征名称  value:临界值数组 ([0,1,2,3])
    private Map<String, List<Double>> thresholdMap = new HashMap<>();
    // 常数项 公式为score=1/(1+e-(w•x+b)) 索引值为1的特征对应的x值为1 w系数为系数数组中的值 b为常数项 intercept
    private double intercept = 0.0d;
    // 系数数组 公式为score=1/(1+e-(w•x+b))
    private double[] coefs;


    /**
     * 初始化
     */
    protected void init() {
        refreshFeaCache();
    }


    /**
     * 定时刷新缓存
     */
    protected void refreshFeaCache() {

        try {
            //加载特征索引文件数据
            Map<String, Integer> tempFeaMap = initFeaMap();

            //加载在线特征配置文件数据
            List<OnlineParseFeaConf> tempOnlineConfList = initConfigList();

            //加载临界值文件数据
            Map<String, List<Double>> tempThresholdMap = initThresholdMap();

            //加载特征阶跃公式系数、常数项文件数据
            String coefStr = redisUtil.getString(RedisKeyConstant.MOSES_FEA_COEF);
            if(!StringUtils.isBlank(coefStr)) {
            	// 解析模型
                List<String> items = Splitter.on(",").splitToList(coefStr);
                double tempIntercept = 0.0d;
                double[] tempCoefs = new double[ (items.size()-1) ];
                
                tempIntercept = Double.valueOf( items.get(0) );
                for( int i = 1; i < items.size(); i++){
                    tempCoefs[i-1] = Double.valueOf( items.get(i) );
                }
                //赋值
                intercept = tempIntercept;
                coefs = tempCoefs;
            }

            feaMap = tempFeaMap;
            onlineConfList = tempOnlineConfList;
            thresholdMap = tempThresholdMap;
        } catch (Exception e) {
            log.error("LR特征缓存刷新失败  ",e);
        }

    }

    /**
     * 获取临界值数组
     * @param key
     * @return
     */
    public List<Double> getThreshold(String key){
        return thresholdMap.get(key);
    }

    /**
     * get
     * @param key
     * @return
     */
    public Integer getIndex(String key){
        return feaMap.get(key);
    }

    /**
     * 获取特征索引map
     * @return
     */
    public Map<String, Integer> getFeaMap(){
        return feaMap;
    }

    /**
     * 获取在线特征配置文件
     * @return
     */
    public List<OnlineParseFeaConf> getOnlineConfList(){
        return onlineConfList;
    }

    /**
     * 获取临界值map
     * @return
     */
    public Map<String, List<Double>> getThresholdMap(){
        return thresholdMap;
    }

    /**
     * 获取常数项
     * @return
     */
    public double getIntercept(){
        return intercept;
    }

    /**
     * 获取系数数组
     * @return
     */
    public double[] getCoefs(){
        return coefs;
    }

    /**
     * 解析feaStr数据，初始化特征
     * feamap文本格式：
     * 索引（从0开始）  \t  特征值    \t PV  \t  Click  \t 点击率
     */
    private Map<String, Integer> initFeaMap(){
        Map<String, Integer> tempfeaMap = new HashMap<>();
        //获取特征索引文件数据
        String feaStr = redisUtil.getString(RedisKeyConstant.MOSES_FEA_MAP);
        //容错
        if(Strings.isNullOrEmpty(feaStr)) {
            return tempfeaMap;
        }
        Iterable<String> lines = Splitter.on("\n").trimResults().split(feaStr);
        Splitter splitter = Splitter.on("\t").trimResults();
        for(String line: lines ) {
            if(Strings.isNullOrEmpty(line)) {
                continue;
            }
            List<String> items = splitter.splitToList( line );
            if(items.size() != 5 ){
                log.error("LR缓存中解析feaStr文件行出错：{} ", line);
                continue;
            }

            // 获取特征索引
            Integer featureIndex = Integer.valueOf( items.get(0) );
            String  featureValue = items.get(1);

            // 获取特征名
            String[] parts = featureValue.split("=");
            if(parts.length < 2) {
                continue;
            }
            tempfeaMap.put(featureValue, featureIndex);
        }
        return tempfeaMap;
    }

    /**
     * 加载在线解析特征配置
     * @param onlineParseFea
     * @return
     */
    public List<OnlineParseFeaConf> initConfigList(){
        List<OnlineParseFeaConf> tempConfigList = new ArrayList<>();
        //加载线上特征配置文件
        String configStr = redisUtil.getString(RedisKeyConstant.MOSES_FEA_PARSE_FEA);
        //容错
        if(Strings.isNullOrEmpty(configStr)) {
            return tempConfigList;
        }
        Iterable<String> lines = Splitter.on("\n").trimResults().split(configStr);

        Splitter splitter = Splitter.on(":").trimResults();
        for(String line: lines ) {
            if(Strings.isNullOrEmpty(line)||"#".equals(line.substring(0,1))) {
                continue;
            }

            List<String> items = splitter.splitToList( line );
            if(items.size() != 8 ){
                log.error("解析在线解析特征配置行出错：{} ", line);
                continue;
            }

            if(!"2".equals(items.get(5))) {//需要在线解析的为2
                continue;
            }
            OnlineParseFeaConf conf = new OnlineParseFeaConf();
            conf.setFeaName(items.get(0));
            conf.setNeedCalcu(Integer.valueOf(items.get(1)));
            conf.setFormula(items.get(4));
            conf.setDefaultValue(items.get(6));

            parseFormulaParams(conf);

            tempConfigList.add(conf);
        }

        return tempConfigList;
    }

    /**
     * 解析特征
     * @param conf
     */
    private void parseFormulaParams(OnlineParseFeaConf conf) {
        String formula = conf.getFormula();
        if(formula.startsWith("split_")) {
            String params = formula.substring(formula.indexOf('(') + 1, formula.length() - 1);
            String paramName = params.substring(0, params.indexOf(',')).trim();

            conf.setParamA(paramName);
        }else if(formula.startsWith("count_isin")) {
            String params = formula.substring(formula.indexOf('(') + 1, formula.length() - 1);
            int indexOfComma = params.indexOf(',');
            String paramAName = params.substring(0, indexOfComma).trim();
            String paramBName = params.substring(indexOfComma + 1).trim();

            conf.setParamA(paramAName);
            conf.setParamB(paramBName);
        }else if(formula.startsWith("clk_order_rate")) {
            String params = formula.substring(formula.indexOf('(') + 1, formula.length() - 1);
            int indexOfComma = params.indexOf(',');
            String paramAName = params.substring(0, indexOfComma).trim();
            String paramBName = params.substring(indexOfComma + 1).trim();

            conf.setParamA(paramAName);
            conf.setParamB(paramBName);
        }else if(formula.contains("+")){
            int indexOfPlussign =  formula.indexOf('+');
            String fieldAName = formula.substring(0, indexOfPlussign).trim();
            String fieldBName = formula.substring(indexOfPlussign + 1).trim();

            conf.setParamA(fieldAName);
            conf.setParamB(fieldBName);
        } else if(formula.startsWith("log1_norm")) {
            String paramAName = formula.substring(formula.indexOf('(') + 1, formula.length() - 1);
            conf.setParamA(paramAName);
        }else {
        }
    }

    /**
     * 解析临界值数据
     * 文本格式：
     * {"key":[0.1,0.2,0.3]}
     */
    public Map<String, List<Double>> initThresholdMap(){
        Map<String, List<Double>> tempThresholdMap = new HashMap<>();
        String thresholdStr = redisUtil.getString(RedisKeyConstant.MOSES_FEA_SPLIT_THRESHOLD);
        if(Strings.isNullOrEmpty(thresholdStr)) {
            return tempThresholdMap;
        }
        JSONObject thresholds = JSONObject.parseObject(thresholdStr);
        Set<String> keys = thresholds.keySet();
        for (String key : keys) {
            JSONArray jsonArray = thresholds.getJSONArray(key);
            List<Double> doubleList = jsonArray.toJavaList(Double.class);

            tempThresholdMap.put(key, doubleList);
        }
        return tempThresholdMap;
    }

}
