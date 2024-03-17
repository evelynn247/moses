package com.biyao.search.ui.home.cache;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;
import com.biyao.search.ui.cache.guava.BaseGuavaCache;
import com.biyao.search.ui.home.constant.HomeConsts;

@Component(value = "homeFloorConfigCache")
public class HomeFloorConfigCache extends BaseGuavaCache<String, List<List<JSONObject>>> {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private final static String KEY = "search:home_floor_config";
    private final static String EXP_KEY = "search:home_floor_config2";
    private final static String REC_KEY = "recommend:home_floor_config";
    
    private final static String PERSONAL_KEY = "personal:home_floor_config";
    

    @Override
    public void loadValueWhenStarted() {
        this.setRefreshDuration(1);
        this.setRefreshTimeUnit(TimeUnit.HOURS);
    }

    @Override
    protected List<List<JSONObject>> getValueWhenExpired(String key) throws Exception {
        List<List<JSONObject>> templateJsonDbList = new ArrayList<>();
        String filePath = HomeConsts.HOME_FLOOR_CONF_PATH;
        // zhaiweixi 2019-09-06 此处为很早之前wuzhenwei的实验，一直没有下线。现注释此处代码
//        if (EXP_KEY.equals(key)){
//            filePath = HomeConsts.HOME_FLOOR_CONF_PATH2;
//        }else if(REC_KEY.equals(key)){
//        	filePath = HomeConsts.HOME_FLOOR_CONF_REC_PATH3;
//        }else if(PERSONAL_KEY.equals(key)){
//        	filePath = HomeConsts.HOME_FLOOR_CONF_PERSONAL_PATH4;
//        }
        try{
            File file = new File(filePath);
            if (!file.exists()){
                logger.error("[严重异常]首页楼层配置文件不存在:{}", filePath);
                throw new Exception("首页楼层配置文件不存在");
            }
            try(
                    FileReader fileReader = new FileReader(file);
                    BufferedReader bufferedReader = new BufferedReader(fileReader);
            ){
                String line;
                List<JSONObject> templateJsonList = new ArrayList<>();
                while ((line = bufferedReader.readLine()) != null) {
                    String lineStr = line.trim();
                    try {
                        if (StringUtils.isBlank(lineStr) || lineStr.startsWith("#")) {
                            continue;
                        }
                        if (lineStr.startsWith("floorID")) {
                            if (templateJsonList.size() > 0) {
                                templateJsonDbList.add(templateJsonList);
                                templateJsonList = new ArrayList<>();
                            }
                            continue;
                        }
                        JSONObject modelJson = JSONObject.parseObject(lineStr);
                        String modelType = modelJson.getString("modelType");

                        if (HomeConsts.ModelTypeConst.BLOCK_LINE.equals(modelType)){
                            // 空白行
//                            HomeTemplate homeTemplate = new HomeTemplate();
//                            homeTemplate.setModelType(modelType);
//                            if (modelJson.containsKey("height")){
//                                homeTemplate.setHeight(modelJson.getInteger("height"));
//                            }
//                            if (modelJson.containsKey("color")){
//                                homeTemplate.setColor(modelJson.getString("color"));
//                            }
                            templateJsonList.add(modelJson);
                        }else if (HomeConsts.ModelTypeConst.TITLE_LINE.equals(modelType)){
                            // 标题行
//                            HomeTemplate homeTemplate = parseTitleLine(modelJson);
                            if (!modelJson.containsKey("title")){
                            	logger.error("[严重异常]首页楼层配置文件--》标题行未配置title, modelJson:{},filePath:{}",modelJson, filePath);
                                throw new Exception("标题行未配置title");
                            }
                            templateJsonList.add(modelJson);
                        }else {
                            // 数据模板
                            if (!modelJson.containsKey("modelType")){
                            	logger.error("[严重异常]首页楼层配置文件--》没有配modelType, modelJson:{},filePath:{}",modelJson, filePath);
                                throw new Exception("没有配modelType");
                            }
                            if (!HomeConsts.ModelTypeConst.validModelType(modelType)){
                            	logger.error("[严重异常]首页楼层配置文件--》modelType不合法, modelJson:{},filePath:{}",modelJson, filePath);
                                throw new Exception("modelType不合法:" + modelType);
                            }
                            if (!modelJson.containsKey("topics")){
                            	logger.error("[严重异常]首页楼层配置文件--》没有配topics, modelJson:{},filePath:{}",modelJson, filePath);
                                throw new Exception("没有配topics");
                            }


                            templateJsonList.add(modelJson);
                        }
                    }catch (Exception e){
                    	logger.error("[严重异常]首页楼层配置文件--》模板行配置不正确:{}, lineStr:{},filePath:{}",lineStr, filePath, e);
                    }
                }
                if (templateJsonList.size() > 0){
                    templateJsonDbList.add(templateJsonList);
                }
            }
        }catch (Exception e){
            logger.error("读取首页楼层配置文件失败:{}", filePath, e);
            throw e;
        }

        return templateJsonDbList;
    }

    public void refreshValue(){
        this.refreshValue(KEY);
        this.refreshValue(EXP_KEY);
        this.refreshValue(REC_KEY);
        this.refreshValue(PERSONAL_KEY);
    }

    public List<List<JSONObject>> getTemplateConfig(){
        return this.getValueOrDefault(KEY, new ArrayList<>());
    }

    public List<List<JSONObject>> getExpTemplateConfig(){
        return this.getValueOrDefault(EXP_KEY, new ArrayList<>());
    }

    public List<List<JSONObject>> getRecTemplateConfig(){
    	return this.getValueOrDefault(REC_KEY, new ArrayList<>());
    }
    
    public List<List<JSONObject>> getPersonalTemplateConfig(){
    	return this.getValueOrDefault(PERSONAL_KEY, new ArrayList<>());
    }
//    /**
//     * 解析标题模板
//     * @param modelJson
//     * @return
//     * @throws Exception
//     */
//    private HomeTemplate parseTitleLine(JSONObject modelJson) throws Exception{
//        HomeTemplate homeTemplate = new HomeTemplate();
//        homeTemplate.setModelType(HomeConsts.ModelTypeConst.TITLE_LINE);
//        List<HomeTopic> homeTopicList = new ArrayList<>();
//        HomeTopic homeTopic = new HomeTopic();
//        if (!modelJson.containsKey("title")){
//            throw new Exception("标题行未配置title");
//        }
//        // topicTitle
//        TopicTitle title = new TopicTitle();
//        title.setContent(modelJson.getString("title"));
//        String titleColor = (modelJson.containsKey("color")) ? modelJson.getString("color") : HomeConsts.DEFAULT_TITLE_COLOR;
//        title.setColor(titleColor);
//        homeTopic.setTitle(title);
//        // topicMore解析
//        if (modelJson.containsKey("more")){
//            TopicMore more = new TopicMore();
//            more.setContent(modelJson.getString("more"));
//            more.setColor(titleColor);
//            homeTopic.setMore(more);
//        }
//        homeTopicList.add(homeTopic);
//        homeTemplate.setData(homeTopicList);
//        return homeTemplate;
//    }
//
//    /**
//     * 解析数据模板
//     * @param modelJson
//     * @return
//     * @throws Exception
//     */
//    private HomeTemplate parseDataLine(JSONObject modelJson) throws Exception{
//        HomeTemplate homeTemplate = new HomeTemplate();
//        if (!modelJson.containsKey("modelType")){
//            throw new Exception("没有配modelType");
//        }
//        String modelType = modelJson.getString("modelType");
//        if (!HomeConsts.ModelTypeConst.validModelType(modelType)){
//            throw new Exception("modelType不合法:" + modelType);
//        }
//        homeTemplate.setModelType(modelType);
//        // 解析data
//        if (!modelJson.containsKey("topics")){
//            throw new Exception("没有配topics");
//        }
//        String topicStr = modelJson.getString("topics");
//        List<TopicConf> topicConfList = JSONArray.parseArray(topicStr, TopicConf.class);
//        if (topicConfList == null || topicConfList.size() == 0){
//            throw new Exception("topics配置不正确");
//        }
//        List<HomeTopic> homeTopicList = new ArrayList<>();
//        for (TopicConf topicConf : topicConfList){
//
//        }
//        return homeTemplate;
//    }
//
//    private class TopicConf{
//        private Integer id;
//        private String titleColor;
//
//        public Integer getId() {
//            return id;
//        }
//
//        public void setId(Integer id) {
//            this.id = id;
//        }
//
//        public String getTitleColor() {
//            return titleColor;
//        }
//
//        public void setTitleColor(String titleColor) {
//            this.titleColor = titleColor;
//        }
//    }
}
