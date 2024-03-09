package com.biyao.moses.constants;

import java.util.HashSet;
import java.util.Set;

/**
 * 
 * @Description
 * @Date 2018年9月27日
 */
public class CommonConstants {

	public static final String GXQSP_ADVERT_ID="59";
	// page每页block数量
	public static final Integer PAGESIZE = 6;
	// feed每页模板数量
	public static final Integer PAGENUM = 10;

	public static final String LAYER_NAME_MATCH = "match";

	public static final String LAYER_NAME_RANK = "rank";

	public static final String LAYER_NAME_UI = "ui";
	// 默认expId 0000
	public static final String DEFAULT_EXPID = "0000";

	public static final String DEFAULT_PREFIX = "moses:";
	// 内容策略项目新增常量类
	public static final String VEDIO = "video:";
	public static final String PAGENAME_DEFAULT_PREFIX = "moses:pn_";
	//-分隔符
	public static final String SPLIT_LINE = "_";
	public static final String SPLIT_COLON = ":";
	public static final String SPLIT_PIPE = "|";
	
	//---------rank----------
	//分页缓存前缀
	public static final String PAGE_CACHE_PREFIX = "moses:cache_";
	//埋点key
	public static final String SPM = "spm";
	public static final String SCM = "scm";
	public static final String STP = "stp";

	public static final String UUID_TWO_UID_PREFIX = "moses:uuid2uid_";
	//首页feed流pageId
	public static final String HOME_FEED_PAGEID = "moses:pid_14";
	//首页feed流topicId
	public static final String HOME_FEED_TOPICID = "10300128";
	//新手专享数据源1 source：XSZXYD、XSZXYS对应的topicId
	public static final String XSZXY_FEED_TOPICID = "10300170";
	//新手专享数据源2 source：XSZX1、zsyzy对应的topicId
	public static final String XSZX1_FEED_TOPICID = "10300160";
	//首页feed流曝光
	public static final String HOME_FEED_CACHE_SUFFIX = "homefeedexp";
	//标准销售属性颜色和尺码的key值
	public static final String STD_SALE_ATTR_KEY_COLOR = "color";
	public static final String STD_SALE_ATTR_KEY_SIZE = "size";
	public static final String USER_VIEW_RDK_PREFIX = "moses:user_viewed_products_";
	//首页轮播图pageId 线上：moses:pid_26 测试moses:pid_101
	public static final String HOME_SWIPER_PICTURE_PAGEID = "moses:pid_26";
	//首页轮播图topicId
	public static final String HOME_SWIPER_PICTURE_TOPICID = "10300162";
	//首页轮播图落地页topicId
	public static final String SLIDER_MIDDLE_PAGE_TOPICID = "10300171";
	//类目页topicId
	public static final String CATEGORY_MIDDLE_PAGE_TOPICID = "10300148";
	//买二返一频道页热门 source：m2f1
	public static final String M2F1_PAGE_TOPICID = "10300174";
	//感兴趣商品频道页 source：gxpsp  10300175
	public static final String GXQSP_PAGE_TOPICID = "10300175";
	//感兴趣商品频道页前端页面ID
	public static final String GXQSP_FRONT_PAGE_ID = "500540";
	//买二返一频道页热门 召回源及其权重
	public static final String M2F1_PAGE_SOURCE_WEIGHT = "acrec,0.4|ibcf,0.3|bert,0.3|ucb,0.01|hots,0.001";
	//买二返一频道页前端页面ID
	public static final String M2F1_FRONT_PAGE_ID = "500431";
	//轮播图落地页前端页面ID
	public static final String LBTLDY_FRONT_PAGE_ID = "500500";
	// 未知性别
	public static final String UNKNOWN_SEX = "-1";
	//标识是商品
	public static final String SHOW_TYPE_PRODUCT = "1";
	//标识是广告
	public static final String SHOW_TYPE_ADVERT = "2";
	//标识是商品组
	public static final String SHOW_TYPE_PRO_GROUP = "3";
	//标识是视频流入口
	public static final String SHOW_TYPE_VIDEO = "4";
	//标识是人工插入的视频视频流入口
	public static final String SHOW_TYPE_OPE_VIDEO = "5";
	// 全局广告 （人工插入的广告和人工插入的视频都认为是广告  处理这些数据时按照广告处理 因为没有商品ID）
	public static final Set<String> SHOW_TYPE_ADVERT_GLOBAL = new HashSet<String>(){
		private static final long serialVersionUID = 3180528290181750754L;
		{
			add("2");add("5");
		}
	};
	//无效的商品ID
	public static final String INVALID_PRODUCT_ID= "-1";

	//空白行的高度的默认值
	public static final String BLANK_LINE_HEIGHT_DEFAULT_VALUE = "20";

	//个性化设置开关关闭时老客的召回源及其权重
	public static final String NON_PERSONALIZED_CUSTOMER_SOURCE_WEIGHT = "ucb3,1|hots,0.001";
	//个性化设置开关关闭时新访客和老访客的召回源及其权重
	public static final String NON_PERSONALIZED_OLDV_NEWV_SOURCE_WEIGHT = "ucb3,1|nchs,0.001";

	//推荐个性化活动开关配置id  APP合规1.5.1新增  RecPerSwitch
	public static final String PERSONALIZE_RECOMMEND_OFF_CONFIG_ID = "RecPerSwitch";
	//感兴趣商品集召回数量下限配置
	public static final String GXQSP_EXP_MIN_NUM = "InterestPdcMin";
	//分类页商品组展示间隔配置
	public static final String CATEGORY_PRO_GROUP_LIMIT = "carPdcGroupLimit";

	/**
	 * 个性化活动页面 是否需要将个性化开关传到match层
	 通用特权金：10300172
	 店铺关注：10300167  商品收藏：10300166
	 我的余额：10300059  精选推荐：10300059
	 特权金空列表页  10300057
	 拼划算：10300058  好物推荐 10300127  夏装街：2047  下午茶:4001 意气风发：4004 女神有约:4007  礼遇佳节：4011
	 */
	public static final Set<String> PERSONAL_ACT_TOPIC_ID=new HashSet<String>(){
		{
			add("10300172");add("10300167");add("10300166");
			add("10300059");add("10300057");add("10300058");
			add("10300127");add("2047");add("4001");
			add("4004");add("4007");add("4011");
		}
	};
	/**
	 * 不支持展示新手特权金的topicId
	 */
	public static final Set<String> NOT_SUPPORT_SHOW_PRIVILEGE=new HashSet<String>(){
		{
			add("10300175");
		}
	};
	// func 接口feed 流兜底redis key前缀  类型:string  moses_feed_cache_{场景id}
	public static final String FEED_FUNC_CACHE_REDIS_PREFIX= "moses_feed_cache_";
	// func 接口feed 流兜底redis key 类目前缀  类型:string  moses_cate_feed_cache_{场景id}_{类目id}
	public static final String FEED_CATE_FUNC_CACHE_REDIS_PREFIX="moses_cate_feed_cache_";
	// func 接口feed 流兜底redis key tag前缀  类型:string  moses_tag_feed_cache_{场景id}_{tagId}
	public static final String FEED_TAG_FUNC_CACHE_REDIS_PREFIX="moses_tag_feed_cache_";

	/**
	 * 查询redis中的商品分的KEY
	 */
	public static final String VIDEO_SCORE_REDIS_KEY = "video_score";


}