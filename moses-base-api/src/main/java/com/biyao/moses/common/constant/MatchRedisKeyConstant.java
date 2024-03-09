package com.biyao.moses.common.constant;

public class MatchRedisKeyConstant {
	/**
	 * Ibcf召回源商品id对应的redis key前缀，类型为String
	 * redis value数据格式：pid:score,pid:score,...,pid:score
	 * 数据刷入：管梓壮
	 * 刷入时间：每天早上5点
	 */
	public static final String MOSES_IBCF_PREFIX = "moses:ibcf_";

	/**
	 * Ibcf2召回源商品id对应的redis key前缀，类型为String
	 * redis value数据格式：pid:score,pid:score,...,pid:score
	 * 数据刷入：管梓壮
	 * 刷入时间：每天早上5点
	 */
	public static final String MOSES_IBCF2_PREFIX = "moses:ibcf2_";
	/**
	 * 热销召回源商品id对应的redis key前缀，类型为String
	 * redis value数据格式：pid:score,pid:score,...,pid:score
	 * 数据刷入：赵晓峰
	 * 刷入时间：每天早上7点
	 */
	public static final String MOSES_HOTS_MALE = "moses:hots_male";
	public static final String MOSES_HOTS_FEMALE = "moses:hots_female";
	public static final String MOSES_HOTS_COMMON = "moses:hots_common";

	/**
	 * tag召回源商品id对应的redis key前缀，类型为String
	 * redis value数据格式：pid:score,pid:score,...,pid:score
	 * 数据刷入：李昭
	 * 刷入时间：每天下午6点到7点半之间
	 */
	public static final String MOSES_TAG_PREFIX = "moses:tag_";

	/**
	 * rtcbr召回源商品id对应的redis key前缀，类型为String
	 * redis value数据格式：pid:score,pid:score,...,pid:score
	 * 数据刷入：李昭
	 * 刷入时间：每天
	 */
	public static final String MOSES_RTCBR_PREFIX = "moses:rtcbr_";

	/**
	 * rtbr2召回源商品id对应的redis key前缀，类型为String
	 * redis value数据格式：pid:score,pid:score,...,pid:score
	 * 数据刷入：李昭
	 * 刷入时间：每天
	 */
	public static final String MOSES_RTBR2_PREFIX = "moses:rtbr2_";

	/**
	 * 商品季节信息，类型为String，value格式为：pid:season1|season2,pid:season1,...,pid:season1:season2
	 * season值为以下几个：春、夏、秋、冬、四季
	 * 数据刷入：李晓峰
	 * 输入时间：半小时一次
	 */
	public static final String PRODUCT_SEASON = "moses:product_season";

	/**
	 * 新实验首页feed流前端界面显示scm的白名单uuid信息，格式为：uuid,uuid,...,uuid
	 */
	public static final String HOME_FEED_WHITE = "moses:home_feed_white_uuid";

	/**
	 * 不需要过滤定制商品的前台类目ID
	 */
	public static final String NOT_FILTER_COSTOM_FCATEIDS = "moses:not_filter_custom_fcateIds";

	/**
	 * 老客、老访客、新访客候选后台三级类目组集合和候选商品集合，类型为String
	 * value格式为：category3:productid,productid…|category3:productid,productid,productid,productid
	 * 数据刷入：晓峰
	 * 输入时间：每天刷新一次
	 * 注：类目和商品是有序的，类目ID越在前优先级越高，同一个类目下的候选商品越在前，优先级越高
	 */
	public static final String CANDIDATE_CATE3_PID_CUSTOMER = "moses:cate3_pid_customer";
	public static final String CANDIDATE_CATE3_PID_OLD_VISITOR = "moses:cate3_pid_old_visitor";
	public static final String CANDIDATE_CATE3_PID_NEW_VISITOR = "moses:cate3_pid_new_visitor";

	/**
	 * 商品ctvr数据
	 * 格式：pid:score,pid:score
	 * 类型：String
	 * 输入：伟西
	 * 时间：每天早晨7点，每天一次
	 */
	public static final String PRODUCT_CTVR = "moses:products_ctvr";

	/**
	 * slot1实验槽召回源商品id对应的redis key前缀，类型为String
	 * redis value数据格式：pid:score,pid:score,...,pid:score
	 * 数据刷入：
	 * 刷入时间：
	 * 备注：如果有uid，则通过uid获取；如果uid没有获取到，则使用uuid获取
	 */
	public static final String MOSES_SLOT1_PREFIX = "moses:slot1_";

	/**
	 * slot2实验槽召回源商品id对应的redis key前缀，类型为String
	 * redis value数据格式：pid:score,pid:score,...,pid:score
	 * 数据刷入：
	 * 刷入时间：
	 * 备注：如果有uid，则通过uid获取；如果uid没有获取到，则使用uuid获取
	 */
	public static final String MOSES_SLOT2_PREFIX = "moses:slot2_";

	/**
	 * 存储后台三级类目的复购周期区间的最小值，类型为String
	 * redis value数据格式：cate3Id:rebuyDayNum,...cate3Id:rebuyDayNum
	 * 数据刷入：李昭
	 * 刷入时间：每天
	 */
	public static  final String MOSES_CATE3_REBUY_CYCLE = "moses:cate3_rebuy_cycle";

	/**
	 * 置顶的轮播图配置信息
	 * 格式为：轮播图1非webp图,轮播图1webp图|轮播图2非webp图,轮播图2webp图
	 * 数据刷入：手动刷入
	 */
	public static final String MOSES_CONFIG_TOP_SLIDER_PIC = "moses:config_top_slider_pic";

	/**
	 * 老客兜底数据缓存
	 */
	public static final String MOSES_CUSTOMER_CACHE_DATA = "moses:customer_cache_data";
	/**
	 * 老访客兜底数据缓存
	 */
	public static final String MOSES_OLDV_CACHE_DATA = "moses:oldv_cache_data";
	/**
	 * 刷新老客、老访客兜底数据时的分布式锁
	 */
	public static final String MOSES_REFRESH_CACHE_LOCK = "moses:refresh_cache_lock";

	/**
	 * 存储用户天气召回的商品信息
	 * 类型：hash
	 *    field： 用户天气，例如“晴”、“雪”、“雨”、“霾”等
	 *    value数据格式：pid:tag,pid:tag,...,pid:tag 表示商品id及其对应标签文本信息
	 * 数据刷入： 人工刷入
	 * 刷入时间： 人工控制
	 */
	public static final String MOSES_WEATHER_PRODUCT = "moses:weather_product";

	/**
	 * 存储基础召回源候选商品集合信息
	 * 类型：String
	 * 数据刷入： 肖建凯
	 * 刷入时间： 每10分钟
	 */
	public static final String MOSES_BASE_PRODUCT = "moses:base_product";

	/**
	 * mosesmatch使用的配置信息
	 * 类型：hash
	 * field: 配置名称
	 * 数据刷入：人工
	 */
	public static final String MOSES_MATCH_CONFIG = "moses:match_config";

	/**
	 * mosesrank使用的配置信息 redis中的key
	 * 类型：hash
	 * field: 配置名称
	 * 数据刷入：人工
	 */
	public static final String MOSES_RANK_CONFIG = "moses:rank_config";

	/**
	 * 商品标签配置信息
	 * 类型：String
	 * 目前的值为：
	 * {"必粉最爱": {"color": "#AB7FD1"},
	 *  "爆品": {"color": "#AB7FD1","textColor": "#FFFFFF","roundColor": "#AB7FD1"},
	 *  "1天生产": {"color": "#D6B98C"},
	 *  "2天生产": {"color": "#D6B98C"},
	 *  "3天生产": {"color": "#D6B98C"},
	 *  "签名定制": {"color": "#D6B98C"},
	 *  "新品": {"color": "#D6B98C"},
	 *  "精选": {"color": "#AB7FD1","textColor": "#FFFFFF","roundColor": "#AB7FD1"},
	 *  "支持特权金": {"color": "#F7A701"},
	 *  "一起拼": {"color": "#FFFFFF","textColor": "#FB4C81","roundColor": "#FB4C81"},
	 *  "特权金": {"color": "#FFFFFF","textColor": "#FB4C81","roundColor": "#FB4C81"},
	 *  "阶梯团": {"color": "#FFFFFF","textColor": "#FB4C81","roundColor": "#FB4C81"},"
	 *  定制":{"textColor":"#FB4C81","roundColor":"#FB4C81","color":"#FFFFFF"}}
	 * 数据刷入：人工
	 */
	public static final String PRODUCT_LABEL_INFO_CONFIG = "search:product_label_info";

	/**
	 * 标准颜色销售属性排序字典配置信息
	 * 类型：String
	 * 数据刷入：人工
	 */
	public static final String STD_SALE_ATTRS_COLOR = "moses:ssa_sort_color";

	/**
	 * 标准尺码销售属性排序字典配置信息
	 * 类型：String
	 * 数据刷入：人工
	 */
	public static final String STD_SALE_ATTRS_SIZE = "moses:ssa_sort_size";

	/**
	 * 新手专享数据源1是否使用算法排序开关配置信息
	 * 类型：String
	 * 数据刷入：人工
	 */
	public static final String XSZX1_SORT_SWITCH = "moses:sxzx1sort_switch";

	/**
	 * mosesqueue消费者是否处理数据开关开关配置信息
	 * 类型：String
	 * 数据刷入：人工
	 */
	public static final String MOSESQUEUE_CONSUMER_ONOFF = "consumerOnOff";

	/**
	 * 每日上新候选商品信息
	 * 类型：String
	 * value格式：[{"pid":pid1,"img":img1,"webp":webp1},{"pid":pid2,"img":img2,"webp":webp2}]
	 * 数据刷入： 赵晓峰
	 * 刷入时间： 每30分钟
	 */
	public static final String MOSES_DN_PRODUCTS = "moses:DN_products";

	/**
	 * 商品性别信息
	 * 类型：hash  field: productId value:性别
	 * 数据刷入： 赵晓峰
	 * 刷入时间： 每30分钟
	 */
	public static final String MOSES_PRODUCT_SEX = "moses:product_sex_label";

	/**
	 * 首页轮播图热销商品信息
	 * 类型：String
	 * value格式：pid1:score1,pid2:score2...
	 * 数据刷入： 赵晓峰
	 * 刷入时间： 每30分钟
	 */
	public static final String HOT_PRODUCT_EXPNUM = "moses:10300162_SPM_2000";

	/**
	 * 首页轮播图高转化商品信息
	 * 类型：String
	 * value格式：pid1:score1,pid2:score2...
	 * 数据刷入： 赵晓峰
	 * 刷入时间： 每30分钟
	 */
	public static final String CONVERSION_PRODUCT_EXPNUM = "moses:10300162_SPM_2001";

	/**
	 * 首页轮播图基础上新商品信息
	 * 类型：String
	 * value格式：pid1:score1,pid2:score2...
	 * 数据刷入： 赵晓峰
	 * 刷入时间： 每30分钟
	 */
	public static final String BASE_PRODUCT_EXPNUM = "moses:10300162_SPM_2002";

	/**
	 * 首页轮播图天气商品信息
	 * 类型：hash  field:天气 value格式：pid1:score1,pid2:score2...
	 * 数据刷入： 赵晓峰
	 * 刷入时间： 每30分钟
	 */
	public static final String WEATHER_PRODUCT_EXPNUM = "moses:10300162_SPM_2003";
	/**
	 * 首页轮播图候选商品信息
	 * 类型：hash filed: productId
	 * value格式："{\"longImage\": \"\", \"image\": \"\", \"routerType\": 6, \"webp\": \"\", \"routeParams\": {\"topicId\": \"10300171\", \"pageId\": \"moses:pid_32\"}}"
	 * 数据刷入： 赵晓峰
	 * 刷入时间： 每30分钟
	 * 刷入脚本位置：89服务器，/usr/local/biyao/service/home_slides_and_daily_new
	 */
	public static final String PRODUCT_CANDIDATE_SET = "moses:10300162_SPM_2004";
	/**
	 * 首页轮播图高转化低决策商品信息
	 * 类型：String
	 * value格式：pid1:score1,pid2:score2...
	 * 数据刷入： 赵晓峰
	 * 刷入时间： 每30分钟
	 */
	public static final String CONVERSION_LOWER_PRODUCT = "moses:10300162_SPM_2005";

	/**
	 * 新手专享运营配置的商品信息
	 * 类型：String
	 * value格式：pid1,pid2,...
	 * 数据来源： 新手专享SCM tagId
	 */
	public static final String MOSES_NEWUSER_SPECIAL_PRODUCTS = "moses:new_user_special_product";
}