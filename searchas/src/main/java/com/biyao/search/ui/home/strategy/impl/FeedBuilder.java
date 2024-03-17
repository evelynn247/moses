package com.biyao.search.ui.home.strategy.impl;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.fastjson.JSON;
import com.biyao.nova.novaservice.model.request.homercd.GuessYouLikeRequest;
import com.biyao.nova.novaservice.model.response.NovaJump;
import com.biyao.nova.novaservice.model.response.NovaResponse;
import com.biyao.nova.novaservice.model.response.common.ShowLabel;
import com.biyao.nova.novaservice.model.response.homercd.GuessYouLikeResult;
import com.biyao.nova.novaservice.model.response.homercd.NovaProduct;
import com.biyao.nova.novaservice.service.RecommendPageDubboService;
import com.biyao.search.common.enums.PlatformEnum;
import com.biyao.search.ui.constant.RedisKeyConsts;
import com.biyao.search.ui.home.cache.HomePageCache;
import com.biyao.search.ui.cache.CoffeePrivateCache;
import com.biyao.search.ui.constant.CommonConstant;
import com.biyao.search.ui.home.constant.HomeConsts;
import com.biyao.search.ui.home.constant.HomeTemplateEnum;
import com.biyao.search.ui.home.model.app.*;
import com.biyao.search.ui.util.IdCalculateUtil;
import com.biyao.search.ui.util.PlatformEnumUtil;
import com.biyao.search.ui.util.RedisUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class FeedBuilder {

	@Resource(name = "recommendPageDubboService")
	private RecommendPageDubboService recommendPageDubboService; 
	@Autowired
	private HomePageCache homePageCache;
	@Autowired
	private RedisUtil redisUtil;

	private Logger logger = LoggerFactory.getLogger(FeedBuilder.class);

	private final static String YQP_LABEL_CONTENT = "一起拼";
	private final static String TQJ_LABEL_CONTENT = "特权金";
	private final static String DEFAULT_TITLE = "为你推荐";
	private final static String HOME_PAGE_RCD = "500001.gyl_home";

	private final static DecimalFormat decimalFormat = new DecimalFormat("###################.##");

	/**
	 * 构造feed流
	 *
	 * @param request
	 * @return
	 */
	public FeedPageData buildFeed(AppHomeRequest request) {
		try {
			FeedPageData result = new FeedPageData();
			Feed feed = new Feed();
			GuessYouLikeRequest guessYouLikeRequest = buildGuessYouLikeRequest(request);
			NovaResponse<GuessYouLikeResult> response;
			if (StringUtils.isBlank(redisUtil.getString(RedisKeyConsts.NOVAKEY))
					|| redisUtil.getString(RedisKeyConsts.NOVAKEY).equals("1")) {
				response = recommendPageDubboService.getHomeRcdFeedsProducts(guessYouLikeRequest);
			} else {
				return homePageCache.getHomeFeedCache(request);
			}
			// novaResponse的code码为1表示成功
			if (response != null && response.getStatus().getCode().equals(1)) {
				// 构造goodsList
				List<NovaProduct> novaProductList = response.getData().getProducts();
				Integer moduleType = response.getData().getModuleType();
				Integer templateType = HomeTemplateEnum.feed_double.getTemplateId();
				// 商品展示类型，1为单排，2为双排
				if (moduleType == 1) {
					templateType = HomeTemplateEnum.feed_single.getTemplateId();
				}
				Integer siteId = Integer.valueOf(request.getSiteId());
				PlatformEnum platformEnum = PlatformEnumUtil.getPlatformEnumBySiteId(siteId);

				List<FeedTemplateDetailInfo> productList = buildTemplateDetailInfoList(novaProductList, platformEnum,
						request);

				// 添加模板
				List<AppHomeTemplate> goodsList = new ArrayList<>();
				ArrayList<TemplateDetailInfo> tmpList = new ArrayList<>();
				for (FeedTemplateDetailInfo feedTemplateDetailInfo : productList) {
					tmpList.add(feedTemplateDetailInfo);
					// 单排模板，一个模板只有一个TemplateDetailInfo元素，双排模板一个模板有两个TemplateDetailInfo元素
					if ((HomeTemplateEnum.feed_single.getTemplateId().equals(templateType) && tmpList.size() == 1)
							|| (HomeTemplateEnum.feed_double.getTemplateId().equals(templateType)
									&& tmpList.size() == 2)) {
						AppHomeTemplate appHomeTemplate = new AppHomeTemplate();
						appHomeTemplate.setTemplateType(templateType);
						appHomeTemplate.setData(tmpList);
						goodsList.add(appHomeTemplate);
						// 添加一个模板后，需要重置tmpList
						tmpList = new ArrayList<>();
					}
				}
				// 设置feed流标题和标题颜色、商品列表
				feed.setGoodsList(goodsList);
				String title = response.getData().getTitle();
				String titleColor = response.getData().getTitleColor();
				if (StringUtils.isEmpty(title)) {
					title = DEFAULT_TITLE;
				}
				if (StringUtils.isEmpty(titleColor)) {
					titleColor = HomeConsts.DEFAULT_TITLE_COLOR;
				}
				feed.setTitle(title);
				feed.setTitleColor(titleColor);

				Integer pageCount = response.getData().getPageCount();
				// 设置返回结果的pageCount、feed流数据
				result.setPageCount(pageCount.toString());
				result.setFeed(feed);
			}
			return result;
		} catch (Exception e) {
			logger.error("[严重异常]首页为你推荐feed流数据获取失败:request={}", JSON.toJSONString(request), e);
			return homePageCache.getHomeFeedCache(request);
		}
	}

	/**
	 * 构造为你推荐请求request
	 *
	 * @param request
	 * @return
	 */
	private GuessYouLikeRequest buildGuessYouLikeRequest(AppHomeRequest request) {
		GuessYouLikeRequest guessYouLikeRequest = new GuessYouLikeRequest();
		// uuid pf pvid pageIndex pageSize必传
		guessYouLikeRequest.setUuid(request.getUuid());
		String pvid = request.getPvid();
		// 如果没有pvid，则自己生成一个，保证调nova不报错
		if (StringUtils.isEmpty(pvid)) {
			pvid = IdCalculateUtil.createBlockId();
		}
		guessYouLikeRequest.setPvid(pvid);

		Integer siteId = Integer.valueOf(request.getSiteId());
		for (PlatformEnum platformEnum : PlatformEnum.values()) {
			if (platformEnum.getNum().equals(siteId)) {
				guessYouLikeRequest.setPlatform(platformEnum.getName());
				break;
			}
		}

		guessYouLikeRequest.setPageIndex(request.getPageIndex());
		guessYouLikeRequest.setPageSize(request.getPageSize());

		// 其他参数
		String uid = request.getUid();
		if (StringUtils.isNotEmpty(uid)) {
			guessYouLikeRequest.setUid(Integer.valueOf(uid));
		}
		String appVersionNum = request.getAvn();
		if (StringUtils.isNotEmpty(appVersionNum)) {
			guessYouLikeRequest.setAppVersionNum(Integer.valueOf(appVersionNum));
		} else {
			guessYouLikeRequest.setAppVersionNum(0);
		}
		guessYouLikeRequest.setCtp(request.getCtp());
		guessYouLikeRequest.setStp(request.getStp());
		guessYouLikeRequest.setDevice(request.getDevice());

		return guessYouLikeRequest;
	}

	/**
	 * 将从nova查到的feed流商品转为feed流模板格式
	 *
	 * @param novaProductList
	 * @param platformEnum
	 * @return
	 */
	private List<FeedTemplateDetailInfo> buildTemplateDetailInfoList(List<NovaProduct> novaProductList,
			PlatformEnum platformEnum, AppHomeRequest request) {
		List<FeedTemplateDetailInfo> templateDetailInfoList = new ArrayList<>();
		if (novaProductList == null || novaProductList.size() == 0) {
			return templateDetailInfoList;
		}
		String spmPrefix = platformEnum.getNum() + "." + HOME_PAGE_RCD;
		Integer avn = 0;
		String uid = request.getUid();
		try {
			if (StringUtils.isNotEmpty(request.getAvn())) {
				avn = Integer.valueOf(request.getAvn());
			}
		} catch (Exception e) {
			logger.error("[严重异常]AppHomeRequest请求中avn不合法，avn={}", request.getAvn(), e);
		}

		// 构造
		for (int i = 0; i < novaProductList.size(); i++) {
			NovaProduct novaProduct = novaProductList.get(i);
			try {
				// 是否过滤咖啡商品
				boolean iscoffee = false;
				// 私人咖啡过滤
				if (PlatformEnum.IOS.getName().equals(platformEnum.getName())
						&& CoffeePrivateCache.checkCoffeePid(novaProduct.getSuid())
						&& (!org.springframework.util.StringUtils.isEmpty(request.getAvn())
								&& Integer.valueOf(request.getAvn()) < CommonConstant.IOS_COFFEE_VERSION)) {
					iscoffee = true;
				}
				if (PlatformEnum.ANDROID.getName().equals(platformEnum.getName())
						&& CoffeePrivateCache.checkCoffeePid(novaProduct.getSuid())
						&& (!org.springframework.util.StringUtils.isEmpty(request.getAvn())
								&& Integer.valueOf(request.getAvn()) < CommonConstant.ANDROID_COFFEE_VERSION)) {
					iscoffee = true;
				}
				if (!iscoffee) {
					FeedTemplateDetailInfo feedTemplateDetailInfo = new FeedTemplateDetailInfo();
					// 主标题即商品标题
					feedTemplateDetailInfo.setMainTitle(novaProduct.getTitle());
					// 副标题展示制造商背景
					feedTemplateDetailInfo.setSubtitle(novaProduct.getSupplierBackground());
					// 图片地址
					feedTemplateDetailInfo.setImage(novaProduct.getImageUrl());
					// 控制左上角角标，根据PRD，只展示新品角标，否则不显示
					// 角标规则：0:不展示 1:新品 2:拼团 3:一起拼
					feedTemplateDetailInfo.setIsShowIcon("0");
					if (novaProduct.getNewProduct() != null && 1 == novaProduct.getNewProduct().intValue()) {
						feedTemplateDetailInfo.setIsShowIcon("1");
					}
					// 标签信息，直接从nova取。有一起拼标签的一定是一起拼商品，需跳到一起拼编辑器
					List<Label> labelList = new ArrayList<>();
					boolean yqp = false;
					boolean tqjLabel = false;
					for (ShowLabel showLabel : novaProduct.getLabels()) {
						Label label = new Label();
						label.setColor(showLabel.getColor());
						label.setContent(showLabel.getContent());
						label.setTextColor(showLabel.getTextColor());
						label.setRoundColor(showLabel.getRoundColor());
						labelList.add(label);
						if (YQP_LABEL_CONTENT.equals(showLabel.getContent())) {
							yqp = true;
						}
						if (TQJ_LABEL_CONTENT.equals(showLabel.getContent())) {
							tqjLabel = true;
						}
					}
					feedTemplateDetailInfo.setLabels(labelList);
					// 设置跳转链接，跳转到详情页。构造stp参数
					String trackParam = "";
					if (StringUtils.isBlank(novaProduct.getStp())) {
						NovaJump novaJump = novaProduct.getJumpData();
						Map<String, String> stp = new HashMap<>();
						stp.put("rpvid", request.getPvid());
						String spm = spmPrefix + "." + i;
						stp.put("spm", spm);
						// 加入追踪参数
						stp.put("trackParam", "");
						if (novaJump != null) {
							stp.put("trackParam", novaJump.getTrackParam());
						}
						trackParam = "stp=" + URL.encode(JSON.toJSONString(stp));
					} else {
						trackParam = "stp=" + novaProduct.getStp();
					}

					String suId = novaProduct.getSuid().toString();
					String routeUrl = getProductRouteUrl(platformEnum, suId, trackParam, yqp);
					feedTemplateDetailInfo.setRouterUrl(routeUrl);
					// 设置价格
					String priceStr = decimalFormat.format(novaProduct.getPrice());
					feedTemplateDetailInfo.setPriceStr(priceStr);
					feedTemplateDetailInfo.setPriceCent(novaProduct.getPriceInCent().toString());
					// 设置朋友买过及好评的拼接字段
					// add by yangle 20181212 novaProduct 获取friendBuyNum 拼接 (好友买 好评)
					if (StringUtils.isBlank(novaProduct.getFriendBuy())) {
						feedTemplateDetailInfo.setThirdContent(novaProduct.getComment());
					} else {
						feedTemplateDetailInfo
								.setThirdContent((novaProduct.getFriendBuy() + "  " + novaProduct.getComment()).trim());
					}
					templateDetailInfoList.add(feedTemplateDetailInfo);
				}
			} catch (Exception e) {
				logger.error("[严重异常]商品转化失败:suId={}", novaProduct.getSuid(), e);
			}
		}

		return templateDetailInfoList;
	}

	/**
	 * 获取商品详情页路由 目前app只对支持一起拼做特殊路由，其他全部跳到低模商品详情页
	 *
	 * @param platformEnum
	 * @param suId
	 * @param trackParam
	 * @param yqp
	 * @return
	 */
	private String getProductRouteUrl(PlatformEnum platformEnum, String suId, String trackParam, boolean yqp) {
		
		if (platformEnum.equals(PlatformEnum.ANDROID) || platformEnum.equals(PlatformEnum.IOS)) {

			// 私人咖啡1.3跳转拼接
			if (CoffeePrivateCache.checkCoffeePid(Long.valueOf(suId))) {
				return CommonConstant.APPRoute.COFFEE_PRODUCT_URL_PREFIX + suId + "&customCoffeeId=2" + trackParam;
			}
			// 支持一起拼
			if (yqp) {
				return CommonConstant.APPRoute.TOGETHER_PRODUCT_URL_PREFIX + suId + "&" + trackParam;
			} else {
				return CommonConstant.APPRoute.L_PRODUCT_URL_PREFIX + suId + "&" + trackParam;
			}
		} else if (platformEnum.equals(PlatformEnum.M)) {
			return "/products/" + suId + ".html?" + trackParam;
		} else if (platformEnum.equals(PlatformEnum.MINI)) {
			return "/pages/products/products?suId=" + suId + "&" + trackParam;
		} else if (platformEnum.equals(PlatformEnum.PC)) {
			return "https://www.biyao.com/products/" + suId + "-0.html?" + trackParam;
		} else {
			return "";
		}
	}

	/**
	 * 判断用户是否支持一起拼，只用于拼商品跳转routeUrl app数字版本号大于某个版本号，且有过首单。参考ProductDetailService的逻辑
	 *
	 * @param uid
	 * @param platformEnum
	 * @param avn
	 * @return
	 */
	private boolean isUserSupportYQP(String uid, PlatformEnum platformEnum, Integer avn) {
		try {
			// 未登录用户不支持一起拼
			// 20180824 zhaiweixi 不需要用户登录
			// if (uid == null || Integer.valueOf(uid.toString()) == 0){
			// return false;
			// }
			// app老版本不支持一起拼
			if (PlatformEnum.IOS.equals(platformEnum) && avn < CommonConstant.IOS_YIQIPIN121_VERSION) {
				return false;
			}
			if (PlatformEnum.ANDROID.equals(platformEnum) && avn < CommonConstant.ANDROID_YIQIPIN121_VERSION) {
				return false;
			}
			// 没有首单的用户不支持一起拼
			// 20180824 zhaiweixi 不需要用户首单
			// Result<List<BFirstOrderInfoDTO>> rpcResult = null;
			// try {
			// rpcResult =
			// orderBaseService.firstPaidOrderByCustomerId(Sets.newHashSet(Long.valueOf(uid)));
			// } catch (Exception e) {
			// logger.error("从border服务查询首单信息时发生异常，uid:" + uid);
			// return false;
			// }
			//
			// if (!rpcResult.isSuccess()) {
			// return false;
			// }
			//
			// if (rpcResult.getObj().size() == 0 || rpcResult.getObj().get(0).getOrderId()
			// == null) {
			// return false;
			// }
			// 以上均没有拦住，则支持一起拼
			return true;
		} catch (Exception e) {
			logger.error("[严重异常]判断用户是否支持一起拼失败: {}", uid, e);
			return false;
		}
	}
}
