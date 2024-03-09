package com.biyao.moses.common.constant;

public class RedisKeyConstant {

	// 所有配置的实验id，包括bid，topicId，tid
	public static final String MOSES_EXP_IDS = "moses:moses_exp_ids";

	// 单个实验前缀
	public static final String MOSES_EXP_PREFIX = "mosesexp:";

	public static final String MOSES_EXP_LOCK = "moses:expLock";
	
	public static final String MOSES_SOURCE_PREFIX="mosessou:";
	
	public static final String MOSES_SWITCH_KEY="mosesswt:avn";
	
	//black list
	public static final String MOSES_BLACK_lIST_ALL_KEY="moses:blacklist_overall";
	
	public static final String MOSES_BLACK_lIST_PAGE_KEY="moses:blacklist_page";
	
	public static final String MOSES_BLACK_lIST_TOPIC_KEY="moses:blacklist_topic";
	
	public static final String MOSES_BLACK_lIST_USER_KEY="moses:blacklist_user";
	
	public static final String MOSES_BLACK_UUID_TOPICID_KEY_PREFIX="moses:blacklist";

	//
	public static final String MOSES_ICF_P2VEC = "moses:icf_p2vec";

	//7日内商品付款订单总数key
	public static final String MOSES_PRODUCT_PORDER_NUM_KEY = "moses:product_porder_num";
	//7日内商品首单转化率key
	public static final String MOSES_PRODUCT_FORDER_RATE_KEY = "moses:product_forder_rate";
	// 商品向量
	public static final String MOSES_PRODUCT_VECTOR = "moses:p_vector";
	// 商品静态分
	public static final String MOSES_PRODUCT_STATIC_SCORE = "moses:p_staticscore";
	// 商品黄金尺码是否充足
	public static final String MOSES_PRODUCT_MUST_SIZE = "moses:product_must_size";

	// 用户浏览商品 redis key prefix
	public static final String USER_VIEW_RDK_PREFIX = "moses:user_viewed_products_";
	//用户近3天3级类目偏好
	public static final String KEY_PREFIEX_LEVEL3_HOBBY = "moses:level3hobby_";
	public static final String KEY_PREFIEX_COMMON_EXPOSURE = "moses:comm_exposure_product_";
	public static final String KEY_PREFIEX_COMMON_PAGE_CACHE = "moses:comm_page_cache_";
	public static final String SPLIT_LINE = "_";
	//特征索引文件，包含索引、特征值、PV 、CLICK 、点击率数据
	public static final String MOSES_FEA_MAP = "moses:moses_fea_map";
	//线上特征配置文件，包含特征名称、是否直接参与计算、默认值、函数等数据
	public static final String MOSES_FEA_PARSE_FEA = "moses:moses_parse_fea";
	//特征界限文件，包含特征的临界值数据
	public static final String MOSES_FEA_SPLIT_THRESHOLD = "moses:moses_split_threshold";
	//全量商品特征数据
	public static final String MOSES_FEA_PRODUCT_FEATURE = "moses:moses_product_feature";
	//存放常数项、以及系数数组
	public static final String MOSES_FEA_COEF = "moses:moses_coef";
	//获取用户uid特征数据
	public static final String MOSES_FEA_USER_FEATURE = "moses:moses_user_feature";
	//获取用户默认的uid特征数据
	public static final String MOSES_FEA_DEFAULT_UID_FEA = "-1";
	//获取访客uuid特征数据
	public static final String MOSES_FEA_VISITOR_FEATURE = "moses:moses_visitor_feature";

	/**
	 * 类目排序集合（男性）
	 * 结构：Map<categoryId,score>
	 */
	public static final String CATEGORYSCOREMALE = "moses:categoryScore_male";
	/**
	 * 类目排序集合（女性）
	 * 结构：Map<categoryId,score>
	 */
	public static final String CATEGORYSCOREFEMALE = "moses:categoryScore_female";
	/**
	 * 类目排序集合（通用）
	 * 结构：Map<categoryId,score>
	 */
	public static final String CATEGORYSCORECOMMON = "moses:categoryScore_common";

	/**************召回源redis.key*** 20191008 *************/
	/**
	 * 热销商品redis key
	 */
    public static final String FEED_RDK = "moses:gpxq_0001";
    /**
     * 低决策推荐redis key
     */
    public static final String LOW_DECISION_RDK = "moses:user_first_order";
    /**
     * 新品基础曝光流量池 redis.key
     */
    public static final String HOME_FEED_EXPOSURE_LESS_KEY = "moses:home_feed_exposure_less";
    /**
     * 上新推荐 redis key
     */
    public static final String NEW_SHELF_RDK = "moses:gpxq_0003";
    /**
     * 天气商品集合 redis.key
     */
    public static final String WEA_MATCH_DATA_KEY = "moses:wea_match_data";

	/**
	 * 上次登录时间(天)
	 */
	public static final String LAST_LOGIN_TIME = "moses:user_3daylvt_";

	/**
	 * 获取相似商品集合
	 */
	public static final String MOSES_SIMILAR_PRODUCT = "moses:similar_product";
	/**
	 * 获取算法分值，用于类目页算法排序
	 */
	public static final String KEY_PREFIEX_MOSES_CR_UP  = "moses:cr_up_";
	/**
	 * 获取算法分值，用于类目页算法排序
	 */
	public static final String KEY_PREFIEX_MOSES_CR_P  = "moses:cr_p_";
	/**
	 * 获取算法分值，用于类目页算法排序
	 */
	public static final String KEY_PREFIEX_MOSES_RS_P_DR  = "moses:rs_p_dr_";
	/**
	 * 流量扶持商品redis key
	 * 刷入：算法 宋浩男  次/5分钟
	 * 类型：String
	 */
	public static final String KEY_MOSES_VALID_SUPPORT  = "moses:valid_support";
	/**
	 * 基础曝光数 redis key
	 * 刷入：算法 宋浩男 次/天
	 * 类型：Hash
	 */
	public static final String KEY_MOSES_BASE_EXP  = "moses:base_exp";
	/**
	 * 商品实时曝光数 redis key
	 * 刷入：算法 宋浩男 次/10分钟
	 * 类型：Hash
	 */
	public static final String KEY_MOSES_RT_EXP  = "moses:rt_exp";
}