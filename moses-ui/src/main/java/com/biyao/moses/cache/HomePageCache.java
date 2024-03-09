package com.biyao.moses.cache;

import com.alibaba.dubbo.common.URL;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.biyao.moses.common.constant.ErrorCode;
import com.biyao.moses.common.constant.MatchRedisKeyConstant;
import com.biyao.moses.common.constant.UPCUserTypeConstants;
import com.biyao.moses.common.enums.PlatformEnum;
import com.biyao.moses.common.enums.SourceEnum;
import com.biyao.moses.constants.CommonConstants;
import com.biyao.moses.context.ByUser;
import com.biyao.moses.context.UserContext;
import com.biyao.moses.controller.RecommendUiController;
import com.biyao.moses.model.template.Block;
import com.biyao.moses.model.template.Page;
import com.biyao.moses.model.template.Template;
import com.biyao.moses.model.template.TemplateInfo;
import com.biyao.moses.model.template.entity.RecommendPage;
import com.biyao.moses.params.ApiResult;
import com.biyao.moses.params.RecommendPageRequest;
import com.biyao.moses.params.UIBaseRequest;
import com.biyao.moses.service.imp.HomePageCacheServiceImpl;
import com.biyao.moses.util.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Component
@EnableScheduling
public class HomePageCache {

	@Autowired
	HomePageCacheServiceImpl homePageCacheService;

	@Autowired
	MatchRedisUtil matchRedisUtil;

	private final  static int expireTime = 86400; //1天

    @Value("${home.feed.pageId}")
    private String homeFeedPageId;
	
	private final static String DEFAULT_CUSTOMER_UUID = "7181011141323f228e8fc0efd4e80000000";
	private final static String DEFAULT_CUSTOMER_PVID = "7181011-1011141323665-55a481590";
	private final static String DEFAULT_OLDV_UUID = "7190815065347dc0f04b73634228e0000000";
	private final static String DEFAULT_OLDV_PVID = "7181011-1011141323665-55a481591";
	private final static String DEFAULT_UID = "0";
	private final static String DEFAULT_AVN = "1";
	private final static String DEFAULT_PF = "ios";
	private final static String DEFAULT_SITEID = PlatformEnum.IOS.getNum().toString();
	private final static Integer totalPage = 40;

	//老客首页缓存数据包括首页feed流、每日上新
	private ArrayList<Page<TemplateInfo>> homePageCustomerDetail = new ArrayList<>();

	//老客首页feed流缓存数据
    private ArrayList<Page<TemplateInfo>> customerDetail = new ArrayList<>();

    //新客首页缓存数据包括feed流和新手专享标题
	private ArrayList<Page<TemplateInfo>> homePageOldVDetail = new ArrayList<>();

	public ArrayList<Page<TemplateInfo>> getHomePageDetail() {
		return homePageCustomerDetail;
	}
	
	public Page<TemplateInfo> getCurrentPageInfo(String pageIndex,String pageId, String topicId){
		// 初始化结果集
		Page<TemplateInfo> resultPage = new Page<>();
		List<Block<TemplateInfo>> resultBlockList = new ArrayList<>();
		resultPage.setBlockList(resultBlockList);
		resultPage.setPid(pageId);
		try {
			Integer page = Integer.valueOf(pageIndex);
			List<Page<TemplateInfo>> target;
			if(CommonConstants.XSZXY_FEED_TOPICID.equals(topicId)
					|| CommonConstants.XSZX1_FEED_TOPICID.equals(topicId)){
				target = homePageOldVDetail;
			}else{
                if(homeFeedPageId.equals(pageId)){
                    target = homePageCustomerDetail;
                }else{
                	target = customerDetail;
                }
            }
			if (page <= target.size()) {
				resultPage = target.get(page - 1);
			}
		} catch (Exception e) {
			log.error("[严重异常][首页缓存]获取首页缓存失败，pageId {}, topicId {}, pageIndex {}", pageId, topicId, pageIndex, e);
		}
        return resultPage;
    }

	/**
	 * 模拟用户请求首页数据
	 * @param userType
	 * @return
	 */
	private ArrayList<Page<TemplateInfo>> queryHomePageData(int userType){
		long start = System.currentTimeMillis();
		ArrayList<Page<TemplateInfo>> cache = new ArrayList<>();
		try {
			ByUser user = initUser(userType);
			// user放入本地线程中
			UserContext userContext = new UserContext(user);
			//避免循环注入引用
			RecommendUiController recommendUiController = ApplicationContextProvider.getBean(RecommendUiController.class);

			RecommendPageRequest request = new RecommendPageRequest();
			if(userType == UPCUserTypeConstants.OLD_VISITOR){
				request.setSource(SourceEnum.XSZXYS.getSource());
			}else{
				request.setSource(SourceEnum.HP.getSource());
			}
			ApiResult<RecommendPage> pageIdResult = recommendUiController.getPageId(request);
			String pid = pageIdResult.getData().getPid();
			String topicId = pageIdResult.getData().getTopicId();
			UIBaseRequest uibaseRequest = new UIBaseRequest();
			uibaseRequest.setPageId(pid);
			uibaseRequest.setTopicId(StringUtils.isEmpty(topicId)?"":topicId);
			for (int i = 1; i <= totalPage; i++) {
				uibaseRequest.setPageIndex(i + "");
				ApiResult<Page<TemplateInfo>> recommendResult = recommendUiController.recommend(uibaseRequest);
				boolean flag = false;
				if (ErrorCode.SUCCESS_CODE.equals(recommendResult.getSuccess())) {
					Page<TemplateInfo> data = recommendResult.getData();
					if (data != null && data.getBlockList() != null) { // 页面数据不为空
						List<Block<TemplateInfo>> blockList = data.getBlockList();
						if (blockList.size() > 0) { //页面楼层不为空
							//最后一个楼层是feed流并且有数据
							Block<TemplateInfo> block = blockList.get(blockList.size() - 1);
							if (block != null && block.getBlock() != null && block.isFeed() && block.getBlock().size()>0
									&& block.getBlock().get(0) != null) { // 楼层模板数据不为空
								//去除实验埋点信息
								removeExpInfo(block);
								cache.add(recommendResult.getData());
								flag = true;
								UserContext.CURRENT_USER.set(user);
							}
						}
					}
				}
				if (!flag) {
					break;
				}
			}
            long end = System.currentTimeMillis();
            log.info("[任务进度][首页缓存]查询首页数据耗时{}ms, 缓存页数{}, 用户类型 {}",end - start, cache.size(), userType);
        } catch (Exception e) {
            log.error("[严重异常][邮件告警-首页缓存]查询用户失败，用户类型 {}，", userType, e);
        }finally{
            UserContext.manulClose();
        }
		return cache;
	}

	/**
	 * 清除feed流中的scm埋点信息
	 * @param block
	 */
	private void removeExpInfo(Block<TemplateInfo> block){
		if(block == null || CollectionUtils.isEmpty(block.getBlock())){
			return;
		}
		List<Template<TemplateInfo>> templateList = block.getBlock();
		for(Template<TemplateInfo> template : templateList){
			List<TemplateInfo> data = template.getData();
			if(CollectionUtils.isEmpty(data)){
				continue;
			}

			for(TemplateInfo templateInfo : data){
				try {
					Map<String, String> routerParams = templateInfo.getRouterParams();
					if (!routerParams.containsKey(CommonConstants.STP)) {
						continue;
					}
					String stp = routerParams.get(CommonConstants.STP);
					if (StringUtils.isBlank(stp)) {
						continue;
					}
					String decodeStr = URLDecoder.decode(stp, "UTF-8");
					Map<String, String> stpMap = JSONObject.parseObject(decodeStr, new TypeReference<Map<String,String>>(){});
					if(!stpMap.containsKey(CommonConstants.SCM)){
						continue;
					}
					stpMap.remove(CommonConstants.SCM);
					routerParams.put(CommonConstants.STP, URL.encode(JSONObject.toJSONString(stpMap)));
				}catch (Exception e){
					log.error("[严重异常][首页缓存]删除实验埋点信息时出现异常，", e);
				}
			}
		}
	}
	/**
	 * 更新首页缓存到redis中
	 */
	private void refreshHomePageDataToRedis(){
		String lockValue = String.valueOf(System.currentTimeMillis());
		boolean lock = matchRedisUtil.lock(MatchRedisKeyConstant.MOSES_REFRESH_CACHE_LOCK, 60, lockValue);
		if(lock){
			log.info("[任务进度][首页缓存]开始更新数据到redis");
			try{
				//更新老客缓存到redis
				int userType = UPCUserTypeConstants.CUSTOMER;
				ArrayList<Page<TemplateInfo>> customerPages = queryHomePageData(userType);
				refreshToRedis(userType, customerPages);
				//更新老访客缓存到redis
				userType = UPCUserTypeConstants.OLD_VISITOR;
				ArrayList<Page<TemplateInfo>> oldvPages = queryHomePageData(userType);
				refreshToRedis(userType, oldvPages);
			}catch (Exception e){
				log.error("[严重异常][邮件告警-首页缓存]刷新到redis失败，", e);
			}finally {
				matchRedisUtil.unlock(MatchRedisKeyConstant.MOSES_REFRESH_CACHE_LOCK, lockValue);
			}
		}
	}

	/**
	 *
	 * @param userType
	 * @param pages
	 */
	private void refreshToRedis(int userType, ArrayList<Page<TemplateInfo>> pages){
		//缓存数据必须得大于两页
		if(CollectionUtils.isEmpty(pages) || pages.size() < 2){
			log.error("[严重异常][首页缓存]首页缓存信息校验失败，不更新到redis中，用户类型 {}，pages {}",
					userType, pages == null ? "null" : JSON.toJSONString(pages));
			return;
		}
		String key;
		if(userType == UPCUserTypeConstants.OLD_VISITOR){
			key = MatchRedisKeyConstant.MOSES_OLDV_CACHE_DATA;
		}else{
			key = MatchRedisKeyConstant.MOSES_CUSTOMER_CACHE_DATA;
		}

		//清空数据
		matchRedisUtil.ltrim(key, 1, 0);
		for(Page<TemplateInfo> page : pages) {
			matchRedisUtil.rpush(key, JSONObject.toJSONString(page));
		}
		matchRedisUtil.expire(key, expireTime);
		log.info("[任务进度][首页缓存]成功更新到redis， 用户类型{}", userType);
	}

	@Scheduled(cron = "0 0/3 * * * ?")
	private void refreshRedisHomeCache() {
		refreshHomePageDataToRedis();
	}

	public void init(){
		refreshCache();
	}

	@Scheduled(cron = "40 0/3 * * * ?")
	private void refreshCache(){
		refreshCache(UPCUserTypeConstants.CUSTOMER);
		refreshCache(UPCUserTypeConstants.OLD_VISITOR);
		log.info("[任务进度][首页缓存]定时刷新首页缓存结束");
	}

	/**
	 * 根据用户类型刷新数据
	 * @param userType
	 */
	private void refreshCache(int userType) {
		ArrayList<Page<TemplateInfo>> cache = new ArrayList<>();
		String key;
		if(userType == UPCUserTypeConstants.CUSTOMER){
			key = MatchRedisKeyConstant.MOSES_CUSTOMER_CACHE_DATA;
		}else{
			key = MatchRedisKeyConstant.MOSES_OLDV_CACHE_DATA;
		}
		try {
			long startTime = System.currentTimeMillis();
			List<String> pageList = matchRedisUtil.lrange(key, 0, -1);
			for (String pageStr : pageList) {
				if (StringUtils.isBlank(pageStr)) {
					continue;
				}
				Page page = JSONObject.parseObject(pageStr, Page.class);
				cache.add(page);
			}
			if(CollectionUtils.isEmpty(cache)){
				log.error("[严重异常][邮件告警-首页缓存]redis数据为空， 用户类型{}", userType);
				return;
			}
			if (userType == UPCUserTypeConstants.CUSTOMER) {
				homePageCustomerDetail = cache;
				refreshCustomerFeedCache(cache);
			} else {
				homePageOldVDetail = cache;
			}
			log.info("[任务进度][首页缓存]定时刷新首页缓存成功，用户类型{}， 缓存页数{}，耗时{}ms", userType, cache.size(), System.currentTimeMillis() - startTime);
		}catch(Exception e){
			log.error("[严重异常][邮件告警-首页缓存]定时刷新首页缓存失败， 用户类型{}", userType, e);
		}
	}

    /**
     * 更新老客首页feed流缓存
     * @param data 老客首页缓存
     */
	private void refreshCustomerFeedCache(ArrayList<Page<TemplateInfo>> data){
	    try {
            ArrayList<Page<TemplateInfo>> cache = new ArrayList<>();
            Page<TemplateInfo> firstPage = new Page<>();
            MyBeanUtil.copyNotNullProperties(data.get(0), firstPage);
            firstPage.setPageName("为你推荐");
            List<Block<TemplateInfo>> blockList = firstPage.getBlockList();
            Block<TemplateInfo> feedBlock = blockList.get(blockList.size() - 1);
            List<Block<TemplateInfo>> feedBlockList = new ArrayList<>();
            feedBlockList.add(feedBlock);
            firstPage.setBlockList(feedBlockList);
            cache.add(firstPage);
            for (int i = 1; i < data.size(); i++) {
                cache.add(data.get(i));
            }
            customerDetail = cache;
            log.info("[任务进度][首页缓存]定时刷新首页老客feed流缓存成功，缓存页数{}" , cache.size());
        }catch(Exception e){
            log.error("[严重异常][邮件告警-首页缓存]定时刷新首页老客feed流缓存失败， ", e);
        }
    }
	/**
	 * 初始化用户请求参数
	 * @param userType
	 * @return
	 */
	public ByUser initUser(int userType) {
		ByUser byUser = new ByUser();
		byUser.setUpcUserType(userType);
		if(userType == UPCUserTypeConstants.OLD_VISITOR){
			byUser.setUuid(DEFAULT_OLDV_UUID);
			byUser.setPvid(DEFAULT_OLDV_PVID);
		}else{
			byUser.setUuid(DEFAULT_CUSTOMER_UUID);
			byUser.setPvid(DEFAULT_CUSTOMER_PVID);
		}
		byUser.setSex(CommonConstants.UNKNOWN_SEX);
		byUser.setAvn(DEFAULT_AVN);
		byUser.setPf(DEFAULT_PF);
		byUser.setSiteId(DEFAULT_SITEID);
		byUser.setUid(DEFAULT_UID);
		byUser.setPlatform(PlatformEnumUtil.getPlatformEnumBySiteId(
				Integer.valueOf(byUser.getSiteId())));
		HashMap<String, String> ctpMap = new HashMap<>();
		ctpMap.put("p", byUser.getPvid());
		String ctp = JSONObject.toJSONString(ctpMap);
		byUser.setCtp(ctp);
		HashMap<String, String> stpMap = new HashMap<>();
		String stp = JSONObject.toJSONString(stpMap);
		byUser.setStp(stp);
		ConcurrentMap<String, String> aidMap = new ConcurrentHashMap<>();
		byUser.setAidMap(aidMap);
		byUser.setTrackMap(new ConcurrentHashMap<>());
		byUser.setSessionId(IdCalculateUtil.createUniqueId());
		return byUser;
	}
	
	//初始化获取pageId缓存
	@Scheduled(cron = "0 0/4 * * * ?")
	private void initpageIdCache() {
		homePageCacheService.initpageIdCache();
	}
	
}