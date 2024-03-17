package com.biyao.search.ui.home.dubbo.impl;

import static com.biyao.search.ui.constant.CommonConstant.HOME_ROUTE_2SEARCHPAGE_MWEB;
import static com.biyao.search.ui.constant.CommonConstant.ROUTE_2SEARCHPAGE_APP;
import static com.biyao.search.ui.constant.CommonConstant.ROUTE_2SEARCHPAGE_MINIAPP;
import static com.biyao.search.ui.constant.RedisKeyConsts.HOME_UU_TOPIC;
import static com.biyao.search.ui.home.constant.ExpConsts.EXP_TOPIC_PRODUCT_SORT;
import static com.biyao.search.ui.home.constant.ExpConsts.SFLAG_TOPIC_PRODUCT_SORT;
import static com.biyao.search.ui.home.constant.HomeConsts.DEFAULT_TITLE_COLOR;
import static com.biyao.search.ui.home.constant.HomeConsts.PARAMETER_ERROR_CODE;
import static com.biyao.search.ui.home.constant.HomeConsts.PARAMETER_ERROR_MESSAGE;
import static com.biyao.search.ui.home.constant.HomeConsts.PARAMETER_ERROR_TO_OLD;
import static com.biyao.search.ui.home.constant.HomeConsts.PARAMETER_ERROR_TO_OLD_MESSAGE;
import static com.biyao.search.ui.home.constant.HomeConsts.SUB_TITLE_COLOR;
import static com.biyao.search.ui.home.constant.HomeConsts.SYSTEM_ERROR_CODE;
import static com.biyao.search.ui.home.constant.HomeConsts.SYSTEM_ERROR_MESSAGE;
import static com.biyao.search.ui.home.constant.HomeConsts.ModelTypeConst.BLOCK_LINE;
import static com.biyao.search.ui.home.constant.HomeConsts.ModelTypeConst.DOUBLE_DUP;
import static com.biyao.search.ui.home.constant.HomeConsts.ModelTypeConst.DOUBLE_FILL;
import static com.biyao.search.ui.home.constant.HomeConsts.ModelTypeConst.DOUBLE_LEFT;
import static com.biyao.search.ui.home.constant.HomeConsts.ModelTypeConst.DOUBLE_RIGHT;
import static com.biyao.search.ui.home.constant.HomeConsts.ModelTypeConst.DOUBLE_UNFILL;
import static com.biyao.search.ui.home.constant.HomeConsts.ModelTypeConst.SINGLE_LINE;
import static com.biyao.search.ui.home.constant.HomeConsts.ModelTypeConst.TITLE_LINE;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.*;

import org.apache.catalina.util.URLEncoder;
import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.biyao.dclog.service.DCLogger;
import com.biyao.nova.novaservice.service.ContactFriendsDubboService;
import com.biyao.search.bs.service.TopicMatch;
import com.biyao.search.common.constant.SearchStatus;
import com.biyao.search.common.enums.PlatformEnum;
import com.biyao.search.common.model.RPCResult;
import com.biyao.search.common.model.TopicItem;
import com.biyao.search.ui.constant.CommonConstant;
import com.biyao.search.ui.constant.ERouterType;
import com.biyao.search.ui.constant.RedisKeyConsts;
import com.biyao.search.ui.exp.UIExperimentSpace;
import com.biyao.search.ui.home.cache.CmsDataConfigCache;
import com.biyao.search.ui.home.cache.HomeFloorConfigCache;
import com.biyao.search.ui.home.cache.HomePageCache;
import com.biyao.search.ui.home.constant.ExpConsts;
import com.biyao.search.ui.home.constant.HomeConsts;
import com.biyao.search.ui.home.constant.HomeConsts.ModelTypeConst;
import com.biyao.search.ui.home.dubbo.HomeDubboService;
import com.biyao.search.ui.home.model.HomeRequest;
import com.biyao.search.ui.home.model.HomeResponse;
import com.biyao.search.ui.home.model.HomeTemplate;
import com.biyao.search.ui.home.model.HomeTopic;
import com.biyao.search.ui.home.model.app.AppHomeRequest;
import com.biyao.search.ui.home.model.app.FeedPageData;
import com.biyao.search.ui.home.model.app.HomeFloor;
import com.biyao.search.ui.home.strategy.TemplateAdapterContext;
import com.biyao.search.ui.home.strategy.impl.FeedBuilder;
import com.biyao.search.ui.model.CmsTopic;
import com.biyao.search.ui.topic.model.TopicImage;
import com.biyao.search.ui.topic.model.TopicMore;
import com.biyao.search.ui.topic.model.TopicTitle;
import com.biyao.search.ui.util.IdCalculateUtil;
import com.biyao.search.ui.util.PlatformEnumUtil;
import com.biyao.search.ui.util.RedisUtil;
import com.biyao.search.ui.util.RouterUtil;
import com.by.profiler.annotation.BProfiler;
import com.by.profiler.annotation.MonitorType;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;

/**
 * 首页服务
 */
@Service("homeDubboServiceImpl")
public class HomeDubboServiceImpl implements HomeDubboService {

    @Autowired
    private TopicMatch topicMatch;
    @Autowired
    private HomeFloorConfigCache homeFloorConfigCache;

    @Autowired
    private ContactFriendsDubboService contactFriendsDubboService;
    @Autowired
    private CmsDataConfigCache cmsDataConfigCache;

    @Autowired
    private UIExperimentSpace uiExperimentSpace;

    @Autowired
    private FeedBuilder feedBuilder;
    @Autowired
    private HomePageCache homePageCache;
    @Autowired
    private RedisUtil redisUtil;

    private final static String HOME_PAGE_ID = "500001";

    private final static String FLOOR_MODULE = "floor";

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private static DCLogger homeFloorLogger = DCLogger.getLogger("searchui_homefloor");

    private static Joiner LINE_JOINER = Joiner.on("_").skipNulls();

    @Override
    @BProfiler(key = "com.biyao.search.ui.home.dubbo.impl.getHomeTemplateDbList",
            monitorType = {MonitorType.TP, MonitorType.HEARTBEAT, MonitorType.
                    FUNCTION_ERROR})
    public HomeResponse<List<List<HomeTemplate>>> getHomeTemplateDbList(HomeRequest request) {
        if (!validHomeRequest(request)) {
            logger.error("[一般异常]调用首页楼层接口参数不合法: {}", JSON.toJSONString(request));
            return new HomeResponse<>(PARAMETER_ERROR_CODE, PARAMETER_ERROR_MESSAGE);
        }
        preHandleParam(request);
        try {
            if (request.getFlags() == null || request.getFlags().size() == 0) {
                //还没有接入实验
                request = uiExperimentSpace.divert(request);
            }
            logger.info("[操作日志][旧首页楼层请求]-[参数request={}]", JSONObject.toJSONString(request));
            String sid = IdCalculateUtil.createBlockId();
            StringBuffer showContent = new StringBuffer();

            List<List<HomeTemplate>> homeTemplateDbList = new ArrayList<List<HomeTemplate>>();
            //判断redis里面key，1的时候走正常流程，其余托底数据
            if (StringUtils.isBlank(redisUtil.getString(RedisKeyConsts.FLOORKEY)) || redisUtil.getString(RedisKeyConsts.FLOORKEY).equals("1")) {
                homeTemplateDbList = buildMSiteData(request, sid, showContent);
            } else {
                homeTemplateDbList = homePageCache.getHomeTemplateCache(request);
            }
            // 打印结果日志, logStr = "0_0_10001",格式为楼层ID(从0开始)，模板ID(每个楼层从0开始)，topicID
            if (showContent.length() > 0) {
                showContent.setLength(showContent.length() - 1);
            }
//            printHomeFloorLog(request, showContent, sid);
            return new HomeResponse<>(homeTemplateDbList);
        } catch (Exception e) {
            logger.error("[严重异常]首页模板数据填充失败:", e);
            return new HomeResponse<>(SYSTEM_ERROR_CODE, SYSTEM_ERROR_MESSAGE);
        }
    }

    private void preHandleParam(HomeRequest request) {
        String ctp = request.getCtp();
        JSONObject ctpJson = null;
        String did = "";
        try {
        	if (StringUtils.isNotEmpty(ctp)) {
        		ctpJson = JSONObject.parseObject(ctp);
        		if(ctpJson != null && ctpJson.getString("did") != null) {
        			did = ctpJson.getString("did");
        		}
        	}
            request.setStp(Strings.isNullOrEmpty(request.getStp())? "" : URLDecoder.decode(request.getStp(),"utf-8"));
            request.setAid(IdCalculateUtil.createAid(did));
            request.setDid(did);
        } catch (Exception e) {
            logger.error("[严重异常]stp decode error, stp is {}, request={}", request.getStp(), JSON.toJSONString(request), e );
        }
    }

    public List<List<HomeTemplate>> buildMSiteData(HomeRequest request,
                                                   String sid, StringBuffer showContent) throws Exception {
        List<List<HomeTemplate>> homeTemplateDbList = new ArrayList<>();
        // 从配置文件中读取楼层配置，每个JSONObject是一个模板，List<JSONObject>是一个楼层，List<List<JSONObject>>是多个楼层
        List<List<JSONObject>> templateJsonDbList;
	    // 增加实验，读不同的楼层配置
        if (EXP_TOPIC_PRODUCT_SORT.equals(request.getStringFlag(SFLAG_TOPIC_PRODUCT_SORT))) {
            templateJsonDbList = homeFloorConfigCache.getExpTemplateConfig();
        } else {
            templateJsonDbList = homeFloorConfigCache.getTemplateConfig();
        }
        
        Integer siteId = Integer.valueOf(request.getSiteId());
        PlatformEnum platformEnum = PlatformEnumUtil.getPlatformEnumBySiteId(siteId);
        
        templateJsonDbList = loadHomeFloorThree(request, templateJsonDbList,platformEnum);
        
        // 搜索中间页的url
        String searchPageUrl = getSearchPageUrl(platformEnum);

        Map<Integer, TopicItem> topicItemMap = new HashMap<Integer, TopicItem>();
        try {
            topicItemMap = getTopicData();
        } catch (Exception e) {
            return homePageCache.getHomeTemplateCache(request);
        }
        // add by yangle 首页第一个楼层用户相关
        // 只在新版本使用，跳转搜索的不露出
        //topicItemMap = rebuildTopicMap(topicItemMap,request.getUuid()); 20201126-此逻辑下线
        List<CmsTopic> cmsData = cmsDataConfigCache.getCmsData();
        int floorId = 0;
        for (List<JSONObject> templateJsonList : templateJsonDbList) {
            List<HomeTemplate> homeTemplateList = new ArrayList<>();
            int moduleId = 0;
            for (JSONObject modelJson : templateJsonList) {
                String modelType = modelJson.getString("modelType");
                //如果专题为空跳过楼层
                if (modelType.equals(HomeConsts.ModelTypeConst.SPECIAL)) {
                    if (cmsData == null || cmsData.size() == 0) {
                        break;
                    }
                }

                //20180908 houkun 如果是推荐中间页跳转
                if (modelJson.containsKey("routeType") && modelJson.getString("routeType").equals(String.valueOf(ERouterType.RECOMMEND.getNum()))) {
                    //推荐中间页跳转，如果版本号不对，则没有此楼层
                    if (!isTrueRecommendVersion(request)) {
                        break;
                    }
                }
                //服务器追踪参数
                Map<String, String> stp = buildSTP(request, sid);
                try {
                    if (BLOCK_LINE.equals(modelType)) {
                        // 空白行模板解析
                        HomeTemplate homeTemplate = parseBlockLine(modelJson);
                        homeTemplateList.add(homeTemplate);
                    } else if (TITLE_LINE.equals(modelType)) {
                        // 标题行模板解析
                        String pointPrefix = floorId + "_" + moduleId;
                        HomeTemplate homeTemplate = parseTitleLine(modelJson, searchPageUrl, platformEnum,
                                stp, pointPrefix, showContent,request);
                        homeTemplateList.add(homeTemplate);
                        moduleId++;
                    } else {
                        // 主题行模板解析
                        String pointPrefix = floorId + "_" + moduleId;
                        HomeTemplate homeTemplate = parseDataLine(modelJson, topicItemMap, searchPageUrl,
                                platformEnum, stp, pointPrefix, showContent,request);
                        homeTemplateList.add(homeTemplate);
                        moduleId++;
                    }
                } catch (Exception e) {
                    logger.error("[严重异常]首页模板主题行配置不正确:{}", JSON.toJSONString(modelJson));
                }
            }
            if (homeTemplateList.size() > 0) {
                homeTemplateDbList.add(homeTemplateList);
                floorId++;
            }
        }
        return homeTemplateDbList;
    }

    /**
     * 读取redis中的数据，看看用户有没有对应的topic数据
     * key 为 uuid
     * value 为 topicId;主标题;副标题;短图,短图…;长图,长图…|topicId;主标题;副标题;短图,短图…;长图,长图…
     * @param topicItemMap
     * @param uuid
     * @return
     */
    private Map<Integer, TopicItem> rebuildTopicMap(Map<Integer, TopicItem> topicItemMap,String uuid) {
        try{
            String topicValue = redisUtil.hgetString(HOME_UU_TOPIC,uuid);
//            String topicValue = "1;nihao1;nibuhao1;1.jpg,2.jpg;3.jpg,4.jpg|2;nihao2;nibuhao2;5.jpg,6.jpg;7.jpg,8.jpg";
            if(StringUtils.isBlank(topicValue)){
                return topicItemMap;
            }
            String[] topicValues = topicValue.split("\\|");
            if(topicValues==null || topicValues.length==0){
                return topicItemMap;
            }
            for(String value:topicValues){
                try{
                    String[] subValues = value.split(";");
                    if(subValues==null || subValues.length==0 || subValues.length != 5 || StringUtils.isBlank(subValues[0])){
                        continue;
                    }
                    TopicItem topicItem = new TopicItem();
                    Integer topicId = Integer.parseInt(subValues[0]);
                    TopicItem sourceTopicItem = topicItemMap.get(topicId);
                    String title = subValues[1];
                    String subtitle = subValues[2];
                    //短图
                    String productImgUrl = subValues[3];
                    //长图
                    String imageUrl = subValues[4];
                    topicItem.setTopicId(topicId);
                    //redis中取出主标题数据不为空，用redis的，不然用默认的
                    topicItem.setShowQuery(StringUtils.isBlank(title)
                            ? (sourceTopicItem==null
                            ? ""
                            : sourceTopicItem.getShowQuery())
                            : title);
                    //redis中取出副标题数据不为空，用redis的，不然用默认的
                    topicItem.setSubTitle(StringUtils.isBlank(subtitle)
                            ?(sourceTopicItem==null
                            ? ""
                            : sourceTopicItem.getSubTitle())
                            : subtitle);
                    //redis中取出短图数据不为空，用redis的，不然用默认的
                    if(!StringUtils.isBlank(productImgUrl)){
                        List<String> productImgUrlList = Arrays.asList(productImgUrl.split(","));
                        Collections.shuffle(productImgUrlList);
                        topicItem.setProductImgUrlList(productImgUrlList);
                    }else{
                        topicItem.setProductImgUrlList(sourceTopicItem==null ? new ArrayList<>():sourceTopicItem.getProductImgUrlList());
                    }
                    //redis中取出长图数据不为空，用redis的，不然用默认的
                    if(!StringUtils.isBlank(imageUrl)){
                        List<String> imageUrlList = Arrays.asList(imageUrl.split(","));
                        Collections.shuffle(imageUrlList);
                        topicItem.setImageUrl(imageUrlList.get(0));
                    }else{
                        topicItem.setImageUrl(sourceTopicItem==null ? "":sourceTopicItem.getImageUrl());
                    }
                    topicItem.setType(2);
                    topicItemMap.put(topicId, topicItem);
                }catch (Exception e){
                    logger.error("[严重异常]首页个性化topic数据出错，error is {}, uuid is {}, value is {}" ,e,uuid,value);
                }
            }
        }catch (Exception e){
            logger.error("[严重异常]首页个性化出错，error is {}" ,e);
        }
        return topicItemMap;
    }

    private List<List<JSONObject>> loadHomeFloorThree(HomeRequest request,
			List<List<JSONObject>> templateJsonDbList, PlatformEnum platformEnum) {
		//TODO 20181204 只是配置的uuid加载homefloor3
        String recommendWhiteList = request.getStringFlag(ExpConsts.SFLAG_RECOMMEND_SPLIT);
        if (StringUtils.isNotEmpty(recommendWhiteList)) {
        	
        	String[] split = recommendWhiteList.split("\\|");//version
        	if (ExpConsts.SFLAG_RECOMMEND_VERSION_CHECK.equals(split[0])) {
        		//如果配置了version，按版本号判断，跳转推荐中间页homefloor3.conf。
        		boolean flag = false;
        		try {
        			Integer siteId = Integer.valueOf(request.getSiteId());
        			if (siteId == PlatformEnum.IOS.getNum() || siteId == PlatformEnum.ANDROID.getNum()) {
        				Integer avn = Integer.valueOf(request.getAvn());
        				//ios andriod大于4.0.0版本才显示
        				if ((siteId == PlatformEnum.IOS.getNum() && avn >= CommonConstant.IOS_ZHULI_VERSION) ||
        						(siteId == PlatformEnum.ANDROID.getNum() && avn >= CommonConstant.ANDROID_ZHULI_VERSION) ) {
        					flag = true;
        				}
					}else{
					    // m站和小程序跳转推荐
						flag = true;
					}
		    	} catch (Exception e) {
		    		logger.error("[严重异常]avn不正确:{}", JSON.toJSONString(request), e);
				}
        		
        		if (flag) {
        			//houkun 20190102 使用版本号判断后进行分流操作
        			String AASpiltFlag = request.getStringFlag(ExpConsts.SFLAG_RECOMMEND_AA_SPLIT);
        			// 不跳转搜索的 改为跳转推荐
        			if (!ExpConsts.SFLAG_RECOMMEND_AA_SPLIT_CHECK.equals(AASpiltFlag)) {
        				
        				//个性化楼层50% 流量实验
            			String personalAA = request.getStringFlag(ExpConsts.SFLAG_PERSONAL_AA_SPLIT);
            			if (ExpConsts.SFLAG_PERSONAL_AA_SPLIT_CHECK.equals(personalAA)) {
							//加载个性化楼层 homefloor3
            				templateJsonDbList = homeFloorConfigCache.getRecTemplateConfig();
						}else{
							//加载无个性化楼层homefloor4
							templateJsonDbList = homeFloorConfigCache.getPersonalTemplateConfig();
						}
					}
        		
				}
			}else{
				if (recommendWhiteList.contains(request.getUuid())) {
					templateJsonDbList = homeFloorConfigCache.getRecTemplateConfig();
				}
			}
		}
		return templateJsonDbList;
	}

    /**
     * 推荐中间页版本判断
     *
     * @param
     * @return
     * @author monkey
     */
    private boolean isTrueRecommendVersion(HomeRequest request) {
        //TODO 上线前要再次改动
        try {
			Integer siteId = Integer.valueOf(request.getSiteId());

			if (siteId == PlatformEnum.M.getNum()) {
			    return true;
			} else if (siteId == PlatformEnum.MINI.getNum()) {
			    return true;
			}

			if (org.apache.commons.lang3.StringUtils.isBlank(request.getAvn())) {
				return false;
			}
			Integer avn = Integer.valueOf(request.getAvn());
			if (siteId == PlatformEnum.IOS.getNum() && avn < CommonConstant.IOS_ZHULI_VERSION) {
			    return false;
			} else if (siteId == PlatformEnum.ANDROID.getNum() && avn < CommonConstant.ANDROID_ZHULI_VERSION) {
			    return false;
			} else {
			    return true;
			}
		} catch (Exception e) {
			return false;
		}
    }
    
    
    
    /**
     * 校验homeRequest
     *
     * @param homeRequest
     * @return
     */
    private boolean validHomeRequest(HomeRequest homeRequest) {
        if (homeRequest.getUuid() == null || "".equals(homeRequest.getUuid().trim())) {
            return false;
        }
        if (homeRequest.getSiteId() == null) {
            return false;
        }
        try {
            Integer siteId = Integer.valueOf(homeRequest.getSiteId());
            if (!validPlatform(siteId)) {
                return false;
            }
        } catch (Exception e) {
            logger.error("[一般异常]homeRequest.siteId={},非整数", homeRequest.getSiteId());
            return false;
        }
        return true;
    }

    /**
     * 校验siteId
     *
     * @param siteId
     * @return
     */
    private boolean validPlatform(Integer siteId) {
        PlatformEnum[] platformEnums = PlatformEnum.values();
        for (PlatformEnum platformEnum : platformEnums) {
            if (platformEnum.getNum().equals(siteId)) {
                return true;
            }
        }
        return false;
    }


    /**
     * 解析空白模板
     *
     * @param modelJson
     * @return
     */
    private HomeTemplate parseBlockLine(JSONObject modelJson) {
        HomeTemplate homeTemplate = new HomeTemplate();
        homeTemplate.setModelType(BLOCK_LINE);
        // 空白行模板只配置高度和颜色，均是可选配置，如果没配置，则不处理
        if (modelJson.containsKey("height")) {
            homeTemplate.setHeight(modelJson.getInteger("height"));
        }
        if (modelJson.containsKey("color")) {
            homeTemplate.setColor(modelJson.getString("color"));
        }
        return homeTemplate;
    }

    /**
     * 解析标题模板
     *
     * @param modelJson
     * @return
     * @throws Exception
     */
    private HomeTemplate parseTitleLine(JSONObject modelJson, String searchPageUrl, PlatformEnum platformEnum,
                                        Map<String, String> stp, String pointPrefix, StringBuffer showContent,HomeRequest request) {
        HomeTemplate homeTemplate = new HomeTemplate();
        homeTemplate.setModelType(ModelTypeConst.TITLE_LINE);
        List<HomeTopic> homeTopicList = new ArrayList<>();
        HomeTopic homeTopic = new HomeTopic();
        // topicTitle，标题行模板只有主标题
        TopicTitle title = new TopicTitle();
        title.setContent(modelJson.getString("title"));
        String titleColor = (modelJson.containsKey("color")) ? modelJson.getString("color") : DEFAULT_TITLE_COLOR;
        title.setColor(titleColor);
        homeTopic.setTitle(title);

        // 2018-08-15 houkun 标题模板增加副标题
        if (modelJson.containsKey("subtitle")) {
            TopicTitle subtitle = new TopicTitle();
            subtitle.setContent(modelJson.getString("subtitle"));
            String subtitleColor = (modelJson.containsKey("subtitleColor")) ? modelJson.getString("subtitleColor") : DEFAULT_TITLE_COLOR;
            subtitle.setColor(subtitleColor);
            homeTopic.setSubTitle(subtitle);
        }

        // topicMore解析，标题行如果配置more选项，则显示更多按钮，更多按钮可跳转到主题搜索中间页。
        if (modelJson.containsKey("more")) {
            //houkun 2018-08-16 增加routeType解析
            if (modelJson.containsKey("routeType")) {
                TopicMore more = new TopicMore();
                ERouterType routeType = ERouterType.getByNum(Integer.valueOf(modelJson.getString("routeType")));

                //小程序routeType为专题列表,请求时不显示more
                if (!(routeType.getNum() == ERouterType.TOPICLIST.getNum() && platformEnum.getNum() == PlatformEnum.MINI.getNum())) {
                    Map<String, String> params = new HashMap<String, String>();

                    //配置文件中的路由参数信息
                    if (modelJson.containsKey("routeParam")) {
                        String routeParamStr = modelJson.getString("routeParam");
                        String[] routeParamArray = routeParamStr.split(",");
                        for (String rp : routeParamArray) {
                            String[] split = rp.split("=");
                            params.put(split[0], split[1]);
                        }
                    }

                    //搜索中间页中的参数
                    String sp = "q=" + modelJson.getString("title");
                    // 当more的内容配置为SP时，跳转商家列表页。否则跳转到主题搜索中间页
                    if ("SP".equals(modelJson.getString("more"))) {
                        sp += "&toSP=1";
                    } else {
                        sp += "&tpid=" + modelJson.getString("more") + "&toTP=1";
                    }
                    params.put("sp", URL.encode(sp));

                    //小程序要带的
                    params.put("query", new URLEncoder().encode(modelJson.getString("title")));

                    String routerUrl = RouterUtil.getRouterUrl(platformEnum, routeType, params);

                    //拼接服务器追踪参数
                    Map<String, String> realStp = getStpMap(stp, pointPrefix, showContent, null,modelJson.getString("title"),request);
                    if (realStp.size() > 0) {
                        routerUrl += "&stp=" + URL.encode(JSON.toJSONString(realStp));
                    }

                    more.setUrl(routerUrl);

                    homeTopic.setMore(more);
                }
            }
        }
        homeTopicList.add(homeTopic);
        homeTemplate.setData(homeTopicList);
        return homeTemplate;
    }

    /**
     * 解析数据模板
     *
     * @param modelJson
     * @return
     * @throws Exception
     */
    private HomeTemplate parseDataLine(JSONObject modelJson, Map<Integer, TopicItem> topicItemMap,
                                       String searchPageUrl, PlatformEnum platformEnum,
                                       Map<String, String> stp, String pointPrefix, StringBuffer showContent, HomeRequest request) throws Exception {
        HomeTemplate homeTemplate = new HomeTemplate();
        String modelType = modelJson.getString("modelType");
        homeTemplate.setModelType(modelType);
        // 是否显示该模板行的下边线。
        if (modelJson.containsKey("borderBottom") && "true".equals(modelJson.getString("borderBottom"))) {
            homeTemplate.setBorderBottom(true);
        }
        // 每个主题模板行的内容是一个List,可能包含1-4个主题。跳转链接均配置在图片上。
        List<HomeTopic> homeTopicList = new ArrayList<HomeTopic>();
        JSONArray topicJsonArr = (JSONArray) modelJson.get("topics");
        List<CmsTopic> cmsData = cmsDataConfigCache.getCmsData();

        for (int i = 0; i < topicJsonArr.size(); i++) {
            HomeTopic homeTopic = new HomeTopic();

            JSONObject topicJson = (JSONObject) topicJsonArr.get(i);
            TopicItem topicItem = null;
            Integer topicId = null;
            String subtitleColor = (topicJson.containsKey("subtitleColor")) ? topicJson.getString("subtitleColor") : SUB_TITLE_COLOR;
            ;
            String priceColor = "";
            //houkun 2018-08-16  判断special模板，重新组织topicItem

            if (modelType.equals(HomeConsts.ModelTypeConst.SPECIAL) && cmsData != null && cmsData.size() > 0) {

                CmsTopic cmsTopic = cmsData.get(0);
                topicItem = new TopicItem();
                topicItem.setTopicId(cmsTopic.getTopicId());//从cms缓存中获取
                topicItem.setImageUrl(cmsTopic.getEntryImageUrl());//从cms缓存中获取
                topicItem.setShowQuery(cmsTopic.getTitle());//主标题
                topicItem.setSubTitle(cmsTopic.getSubtitle());//子标题
                topicItem.setType(8);
                // 获取标题颜色 主标题 子标题

                // special模板价格颜色，在topic中添加priceColor
                priceColor = (topicJson.containsKey("priceColor")) ? topicJson.getString("priceColor") : DEFAULT_TITLE_COLOR;
                homeTopic.setPriceColor(priceColor);
                // 小程序添加 价格
                homeTopic.setPrice(cmsTopic.getPrice());

            } else {
                topicId = topicJson.getInteger("id");
                // 如果该主题行配置的主题Id没有找到，则不显示改行主题。防止多列模板显示有问题。
                if (!topicItemMap.containsKey(topicId)) {
                    throw new Exception("没有找到topic{topicId=" + topicId + "}");
                }
                topicItem = topicItemMap.get(topicId);
            }
            // 主标题的颜色可配置，默认是DEFAULT_TITLE_COLOR
            String color = (topicJson.containsKey("titleColor")) ? topicJson.getString("titleColor") : DEFAULT_TITLE_COLOR;

            // 标题: TopicItem.showQuery
            // 副标题: TopicItem.subTitle
            // 图片地址: 当模板中需要长方形的图片时，取TopicItem.imageUrl，当模板中需要用正方形图片时，取TopicItem.productImgUrlList的元素
            List<TopicImage> topicImageList = new ArrayList<>();
            // 设置主题的标题和副标题，标题内容从TopicItem中来
            // DOUBLE_FILL类型的模板行不显示标题和副标题，其他的每个主题都要显示标题和副标题
            if (!DOUBLE_FILL.equals(modelType)) {
                TopicTitle title = new TopicTitle(), subTitle = new TopicTitle();
                title.setContent(topicItem.getShowQuery());
                title.setColor(color);
                title.setContent(topicItem.getShowQuery());
                homeTopic.setTitle(title);
                subTitle.setContent(topicItem.getSubTitle());
                subTitle.setColor(subtitleColor);
                homeTopic.setSubTitle(subTitle);
            }

            // houkun 2018-08-16 
            String routerUrl = "";
            if (topicJson.containsKey("routeType")) {
                ERouterType routeType = ERouterType.getByNum(Integer.valueOf(topicJson.getString("routeType")));

                Map<String, String> params = new HashMap<String, String>();

                //配置文件中的路由参数信息
                if (topicJson.containsKey("routeParam")) {
                    String routeParamStr = topicJson.getString("routeParam");
                    String[] routeParamArray = routeParamStr.split(",");
                    for (String rp : routeParamArray) {
                        String[] split = rp.split("=");
                        if (split[0].equals("topicId")) {//topicId不是固定的，从cms中拿
                            params.put(split[0], String.valueOf(topicItem.getTopicId()));
                        } else {
                            params.put(split[0], split[1]);
                        }
                    }
                }

                //搜索中间页中的参数
                String sp = "";
                if (modelJson.containsKey("fb")) {
                    sp = "q=" + topicItem.getShowQuery() + "&tpid=" + topicItem.getTopicId() + "&toFB=1";
                } else {
                    sp = "q=" + topicItem.getShowQuery() + "&tpid=" + topicItem.getTopicId() + "&toTP=1";
                }
                params.put("sp", URL.encode(sp));

                //小程序要带的
                params.put("query", new URLEncoder().encode(topicItem.getShowQuery()));

                routerUrl = RouterUtil.getRouterUrl(platformEnum, routeType, params);
                Map<String, String> realStp = getStpMap(stp, pointPrefix, showContent, topicItem,"",request);

                if (realStp.size() > 0) {
                    routerUrl += "&stp=" + URL.encode(JSON.toJSONString(realStp));
                }

            }
            // 使用主题图(长方形图片)的类型
            if (SINGLE_LINE.equals(modelType) || DOUBLE_FILL.equals(modelType) || DOUBLE_UNFILL.equals(modelType) || modelType.equals(HomeConsts.ModelTypeConst.SPECIAL)) {
                TopicImage topicImage = new TopicImage();
                topicImage.setImageUrl(topicItem.getImageUrl());

                topicImage.setUrl(routerUrl);
                topicImageList.add(topicImage);
            } else {
                // 使用正方形图片
                List<String> productImageUrlList = topicItem.getProductImgUrlList();
                for (String productImageUrl : productImageUrlList) {
                    TopicImage topicImage = new TopicImage();
                    topicImage.setImageUrl(productImageUrl);

                    topicImage.setUrl(routerUrl);
                    topicImageList.add(topicImage);
                }
            }
            // 前端要求对topic写死图片数量，否则展示有问题，
            List<TopicImage> imgUrl = new ArrayList<>();
            // 从topicImageList中随机出图
            Collections.shuffle(topicImageList);
            if (DOUBLE_DUP.equals(modelType) || (DOUBLE_LEFT.equals(modelType) && i == 0) || (DOUBLE_RIGHT.equals(modelType) && i == 1)) {
                imgUrl.add(topicImageList.get(0));
                imgUrl.add(topicImageList.get(1));
            } else {
                imgUrl.add(topicImageList.get(0));
            }
            homeTopic.setImgUrl(imgUrl);
            homeTopicList.add(homeTopic);

        }
        homeTemplate.setData(homeTopicList);
        return homeTemplate;
    }

    /**
     *  //拼接服务器追踪参数
     * @param stp
     * @param pointPrefix
     * @param showContent
     * @param topicItem
     * @return
     */
    private Map<String, String> getStpMap(Map<String, String> stp, String pointPrefix, StringBuffer showContent, TopicItem topicItem,String title,HomeRequest request) {
        Map<String, String> realStp = new HashMap<>();
        if (stp.containsKey("spm")) {
            String spmPrefix = stp.get("spm");
            String spm = "";
            if(topicItem == null){
                spm = String.format("%s.%s_%s", spmPrefix, pointPrefix, title);
            }else{
                spm = String.format("%s.%s_%s", spmPrefix, pointPrefix, topicItem.getTopicId());
            }
            realStp.put("spm", spm);
            showContent.append(spm).append(",");
        }
        if (stp.containsKey("rpvid") && StringUtils.isNotEmpty(stp.get("rpvid"))) {
            realStp.put("rpvid", stp.get("rpvid"));
        }
        if (stp.containsKey("sid") && StringUtils.isNotEmpty(stp.get("sid"))) {
            realStp.put("sid", stp.get("sid"));
        }
        // 生成该标题更多按钮的唯一标识，用于数据跟踪。
        String uniqueID = IdCalculateUtil.createBlockId();
        realStp.put("uniqId", uniqueID);

        Map<String,String> aidMap = new HashMap<String, String>();
        JSONObject stpJson = JSONObject.parseObject(request.getStp());
        String aid = IdCalculateUtil.createAid(request.getAid()+ uniqueID);
        if(stpJson==null){
            aidMap.put("home",aid);
        }else{
            String aidJson = stpJson.getString("aid");
            if(!StringUtils.isBlank(aidJson)){
                aidMap = (Map<String, String>) JSON.parse(aidJson);
            }
            aidMap.put("home",aid);
        }
        stp.put("aid",JSON.toJSONString(aidMap));
        if (stp.containsKey("aid") && StringUtils.isNotEmpty(stp.get("aid"))) {
            realStp.put("aid", stp.get("aid"));
            showContent.append(aid).append(";");
        }

        return realStp;
    }

    /**
     * 获取搜索中间页链接url
     *
     * @param platformEnum
     * @return
     */
    private String getSearchPageUrl(PlatformEnum platformEnum) {
        switch (platformEnum) {
            case IOS:
                return ROUTE_2SEARCHPAGE_APP;
            case ANDROID:
                return ROUTE_2SEARCHPAGE_APP;
            case M:
                return HOME_ROUTE_2SEARCHPAGE_MWEB;
            case MINI:
                return ROUTE_2SEARCHPAGE_MINIAPP;
            default:
                return HOME_ROUTE_2SEARCHPAGE_MWEB;
        }
    }

    /**
     * 首页优化日志
     *
     * @param request
     * @param showContent
     */
    @Deprecated
    private void printHomeFloorLog(HomeRequest request, StringBuffer showContent, String sid) {
        StringBuffer logStr = new StringBuffer();
        PlatformEnum platformEnum = PlatformEnumUtil.getPlatformEnumBySiteId(Integer.valueOf(request.getSiteId()));
        logStr.append("lt=home_floor\t");
        logStr.append("lv=1.0\t");
        logStr.append("pf=" + platformEnum.getName() + "\t");
        logStr.append("uu=" + request.getUuid() + "\t");
        logStr.append("u=" + request.getUid() + "\t");
        logStr.append("avn=" + request.getAvn() + "\t");
        logStr.append("pvid=" + request.getPvid() + "\t");
        logStr.append("did=" + request.getDid() + "\t");
        logStr.append("d=" + request.getDevice() + "\t");
        logStr.append("sid=" + sid + "\t");
        logStr.append("aid=" + request.getAid() +"\t");
        logStr.append("stp=" + request.getStp() +"\t");
        logStr.append("ctp=" + request.getCtp() +"\t");
        logStr.append("exp=" + LINE_JOINER.join(request.getExpIds()) + "\t");
        logStr.append("resp=" + showContent);
        homeFloorLogger.printDCLog(logStr.toString());
    }


    @Override
    @BProfiler(key = "com.biyao.search.ui.home.dubbo.impl.getHomeTemplateDbList2App",
            monitorType = {MonitorType.TP, MonitorType.HEARTBEAT, MonitorType.
                    FUNCTION_ERROR})
    public HomeResponse<HomeFloor> getHomeTemplateDbList2App(AppHomeRequest request) {

        if (!request.validateHomePage() || !validHomeRequest(request)) {
            logger.error("[一般异常]调用首页楼层接口参数不合法: {}", JSON.toJSONString(request));
            return new HomeResponse<>(PARAMETER_ERROR_TO_OLD, PARAMETER_ERROR_TO_OLD_MESSAGE);
        }
        preHandleParam(request);
        uiExperimentSpace.divert(request);
        logger.info("[操作日志][新首页楼层请求]-[参数request={}]", JSONObject.toJSONString(request));
        String sid = IdCalculateUtil.createBlockId();
        StringBuffer showContent = new StringBuffer();
        //放在老接口中 新老接口实验判断
//        String stringFlag = request.getStringFlag(ExpConsts.sflag_search_app);
//        if (!ExpConsts.sflag_search_app_new.equals(stringFlag)) {
//            // 直接返回访问老接口
//            logger.error("[实验访问老接口]-[uuid={}]", request.getUuid());
//            printHomeFloorLog(request, new StringBuffer().append(""), sid);
//            return new HomeResponse<>(PARAMETER_ERROR_TO_OLD, HomeConsts.EXP_TO_OLD);
//        }

        //暂时调用老的接口
        List<List<HomeTemplate>> oldFloorList = null;
        try {
            //判断redis里面key，1的时候走正常流程，其余托底数据
            if (StringUtils.isBlank(redisUtil.getString(RedisKeyConsts.FLOORKEY)) || redisUtil.getString(RedisKeyConsts.FLOORKEY).equals("1")) {
                oldFloorList = buildMSiteData(request, sid, showContent);
            } else {
                oldFloorList = homePageCache.getHomeTemplateCache(request);
            }
        } catch (Exception e1) {
            logger.error("[严重异常][老接口报错]-[uuid={}]，exception is {}", request.getUuid(),e1);
            //上报日志
//            printHomeFloorLog(request, new StringBuffer().append(""), sid);
            return new HomeResponse<>(PARAMETER_ERROR_TO_OLD, SYSTEM_ERROR_MESSAGE);
        }
        HomeFloor resultHomeFloor = new HomeFloor();
        try {
            TemplateAdapterContext.convertOldFloor2New(oldFloorList, resultHomeFloor);
            // 解析feed流数据
            FeedPageData feed = feedBuilder.buildFeed(request);
            feed.setPageIndex(request.getPageIndex() + "");
            resultHomeFloor.setFeed(feed.getFeed());

        } catch (Exception e) {
            logger.error("[严重异常]首页模板数据填充失败，request={}",JSON.toJSONString(request) , e);
            //上报日志
//            printHomeFloorLog(request, new StringBuffer().append(""), sid);
            return new HomeResponse<>(PARAMETER_ERROR_TO_OLD, SYSTEM_ERROR_MESSAGE);
        }
        //上报日志
        if (showContent.length() > 0) {
            showContent.setLength(showContent.length() - 1);
        }
//        printHomeFloorLog(request, showContent, sid);

        return new HomeResponse<>(resultHomeFloor);
    }

    /**
     * 构造跳转链接的基本参数，即本次请求中各个跳转链接都需要添加的。
     * stp是指服务端跟踪参数，主要包含以下几个key：
     * spm是指在页面的点击位置参数
     * rpvid是指发起本次请求的pvid，需要带到跳转后的页面
     * sid是指本次请求的服务端唯一标识
     */
    private Map<String, String> buildSTP(HomeRequest request, String sid) {
        Map<String, String> stp = new HashMap<>();
        stp.put("rpvid", request.getPvid());
        // 这里只封装点击位置的前缀(站点编号.首页页面编号.楼层)，具体位置在拼跳转链接时产生
        stp.put("spm", String.format("%s.%s.%s", request.getSiteId(), HOME_PAGE_ID, FLOOR_MODULE));
        stp.put("sid", sid);
        return stp;
    }

    private Map<Integer, TopicItem> getTopicData() throws Exception {
        Map<Integer, TopicItem> topicItemMap = null;
        try {
            // 从search-bs中获取主题配置内容，用来填充模板中每个主题的内容。
            // 标题: TopicItem.showQuery
            // 副标题: TopicItem.subTitle
            // 图片地址: 当模板中需要长方形的图片时，取TopicItem.imageUrl，当模板中需要用正方形图片时，取TopicItem.productImgUrlList的元素
            RPCResult<Map<Integer, TopicItem>> topicMatchResult = topicMatch.getTopicItemMap();
            if (topicMatchResult != null && SearchStatus.OK.equals(topicMatchResult.getStatus())) {
                topicItemMap = topicMatchResult.getData();
            } else {
            	logger.error("[严重异常]从search-bs中获取主题配置内容，用来填充模板中每个主题的内容时，(TopicMatch.getTopicItemMap)接口返回失败");
                throw new Exception("调用TopicMatch.getTopicItemMap服务失败:" + topicMatchResult.getStatus().getMessage());
            }
        } catch (Exception e) {
        	logger.error("[严重异常]从search-bs中获取主题配置内容，用来填充模板中每个主题的内容时，(TopicMatch.getTopicItemMap)发生异常", e);
            throw e;
        }
        return topicItemMap;
    }

    @Override
    @BProfiler(key = "com.biyao.search.ui.home.dubbo.impl.getGuessYouLikeProducts2App",
            monitorType = {MonitorType.TP, MonitorType.HEARTBEAT, MonitorType.
                    FUNCTION_ERROR})
    public HomeResponse<FeedPageData> getGuessYouLikeProducts2App(AppHomeRequest request) {
        logger.info("[feed流请求参数]-[request={}]", JSONObject.toJSONString(request));
        if (!request.validateFeedPage() || !validHomeRequest(request)) {
            logger.error("[一般异常]调用首页feed参数非法: {}", JSON.toJSONString(request));
            return new HomeResponse<>(PARAMETER_ERROR_CODE, PARAMETER_ERROR_MESSAGE);
        }
        FeedPageData feed = null;
        try {
            // 解析feed流数据
            feed = feedBuilder.buildFeed(request);
            feed.setPageIndex(request.getPageIndex() + "");
        } catch (Exception e) {
            logger.error("[严重异常]调用首页feed失败,request={}",JSON.toJSONString(request), e);
            return new HomeResponse<>(SYSTEM_ERROR_CODE, SYSTEM_ERROR_MESSAGE);
        }
        return new HomeResponse<FeedPageData>(feed);
    }

//    public static void main(String[] args) {
//        HomeDubboServiceImpl homeDubboService = new HomeDubboServiceImpl();
//        Map<Integer, TopicItem> topicmap = new HashMap<>();
//        TopicItem topicItem = new TopicItem();
//        topicItem.setProductImgUrlList(Arrays.asList("rr.jpg,cc.jpg".split(",")));
//        topicItem.setSubTitle("3343");
//        topicItem.setShowQuery("444444");
//        topicItem.setImageUrl("xx.jpg");
//        topicmap.put(new Integer(1),topicItem);
//        Map<Integer, TopicItem> integerTopicItemMap = homeDubboService.rebuildTopicMap(topicmap, "1");
//        for(Integer key : integerTopicItemMap.keySet()){
//            System.out.println("key=" + key +" value="+integerTopicItemMap.get(key).getShowQuery()+","+integerTopicItemMap.get(key).getSubTitle()+","+integerTopicItemMap.get(key).getProductImgUrlList()+","+integerTopicItemMap.get(key).getImageUrl());
//        }
//    }
}