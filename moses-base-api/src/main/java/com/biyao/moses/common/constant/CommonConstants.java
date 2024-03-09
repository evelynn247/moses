package com.biyao.moses.common.constant;

import java.util.HashSet;
import java.util.Set;

/**
 * 
 * @Description
 * @Date 2018年9月27日
 */
public class CommonConstants {
	// feed每页模板数量
	public static final Integer PAGENUM = 10;
	public static final String LAYER_NAME_MATCH = "match";

	public static final String LAYER_NAME_RANK = "rank";

	// 默认expId 0000
	public static final String DEFAULT_EXPID = "0000";
	// 默认expNum 0000
	public static final String DEFAULT_EXPNUM = "0000";
	// 没有获取到实验时，通过此num，区分具体埋点数据
	public static final String DIFFERENTIATE_NUM = "666666";
	// 默认matchName DefaultMatch
	public static final String DEFAULT_MATCH_NAME = "DefaultMatch";

	public static final String UDM_MATCH_NAME = "UDM";

	public static final String UIDM_MATCH_NAME = "UIDM";

	public static final String DEVICE_DEFAULT_NAME = "device";

	public static final String DEFAULT_PREFIX = "moses:";
	// ------我的足迹start----------

	// 足迹横滑模板key
	public static final String MOSES_FOOT_PRINT = "moses:footprint_FootPrintMatch_0000_";

	// 足迹-精选发现 内容
	public static final String MOSES_MY_STREET = "moses:myStreet_MyStreetMatch_0000_";

	// 商品共现key
	public static final String MOSES_GOD_OCCURRENCE = "moses:god_occurrence";
	// 商品组位置rediskey的过期时间
	public static final  Integer TIME_30MIN =1800;
	// 足迹redis保存时长
	public static final int EXEPIRE_TIME = 60 * 60 * 24 * 30;

	// 我的街redis保存时长
	public static final int MY_STREET_EXEPIRE_TIME = 60 * 60 * 24 * 30;

	// 我的街足迹刷新时长
	public static final int MY_STREET_REFRESH_TIME = 60 * 60 * 8;

	public static final String MY_STREET_REFRESH_KEY = "moses:myRefreshKey_MyStreetMatch_0000_";

	// ------我的足迹end---------

	// -分隔符
	public static final String SPLIT_LINE = "_";
	public static final String SPLIT_COLON = ":";

	// ---------rank----------

	public static final String RANKSCORE_FACTOR = "moses:rs_fac_";
	public static final String RANKSCORE_DYNAMIC_FACTOR = "moses:rs_dfac_";

	public static final String RANKSCORE_PID = "moses:rs_";

	// 商品热销
	public static final String MOSES_TOTAL_HOT_SALE = "moses_total_hot_sale";

	// 商品轮播图最小数量
	public static final int PRODUCT_SPLIT_NUM = 3;

	public static final String MYSTREET_TOPICID = "10300128";

	public static final String MYSTREET_TOPICID_EXPNUM = "1000";

	public static final String SEX_LABEL = "moses:visitorSexLabel";

	// 用户对三级类目偏好
	public static final String LEVEL_HOBBY_PREFIX = "moses:level3hobby_";

	// 需要特殊处理的后台二级类目ID
	// 2019-06-13 翟伟西 眼镜39(低模眼镜) 眼镜55(隐形眼镜) 尿裤湿巾800404 女性护理6000011[目前只有卫生巾和护垫] 床品套件7101
	public static final Set<Long> SPECIAL_CATEGORY2_IDS = new HashSet<Long>(){
		{add(39l); add(55l); add(800404l); add(800404l); add(6000011l); add(7101l);}
	};
	//视频流落地页相关的场景id
	public static final Set<String> VIDEO_SCENDIDS = new HashSet<String>(){
		{add("2901"); add("2902"); add("2903");add("29");}
	};
	// 不需要分端隐藏的端 4 小程序A 5 小程序B   cps1.1  新增  4 5  cps 1.3 新增 0
	public static final Set<String> NO_SITE_FILTER = new HashSet<String>(){
		private static final long serialVersionUID = -6437329809897547690L;
		{add("4"); add("5");add("0");}
	};
	//现有的端   cps1.1  新增  4 5  cps 1.3 新增 0
	public static final Set<String> SITEID = new HashSet<String>(){
		{add("1"); add("2"); add("3"); add("4"); add("5"); add("7"); add("9");add("0");}
	};
	//眼镜后台二级类目ID 眼镜39(低模眼镜) 眼镜55(隐形眼镜)
	public static final Set<Long> GLASSES_CATEGORY2_IDS = new HashSet<Long>(){
		{add(39l); add(55l);}
	};
	//分页缓存前缀
	public static final String PAGE_CACHE_PREFIX = "moses:cache_";
	
	//首页feed流曝光
	public static final String HOME_FEED_CACHE_SUFFIX = "homefeedexp";
	
	//当天类目推荐商品数key
	public static final String CATE_PRODUCT_TODAY_KEY_PREFIEX = "moses:cate_products_today_num_";
	// 近7天类目推荐商品数key
	public static final String CATE_PRODUCT_7DAY_KEY_PREFIEX = "moses:cate_products_7day_num_";
	// 商品深度浏览深度key
	public static final String UUID_DEEPVIEW_KEY_PREFIEX = "moses:uuid_deepview_";

	// 未知性别
	public static final String UNKNOWN_SEX = "-1";
	// 中性(通用)
	public static final String COMMON_SEX = "2";
	// 男性
	public static final String MALE_SEX = "0";
	// 女性
	public static final String FEMALE_SEX = "1";
	//match异步获取召回源数据最大等待时间
	public static final int MATCH_MAX_WAIT_TIME = 200;

	//标识是广告
	public static final String SHOW_TYPE_ADVERT = "2";

	//N折商品池ScmTagId，新手专享V1.5 prd中定义为266
	public static final String DISCOUNT_N_SCM_TAGID = "266";

	//基础曝光数A
	public static final String A_BASE_NUM = "Abasenum";
	//基础曝光数N
	public static final String N_BASE_NUM = "Nbasenum";
	public static final Integer ONE =1;
	public static final Integer ZERO =0;
	public static final long dayMilliSecond = 60 * 60 * 24*1000;
	/**
	 * 一小时的秒数
	 */
	public static final int ONE_HOUR_SECOND = 3600;
}
