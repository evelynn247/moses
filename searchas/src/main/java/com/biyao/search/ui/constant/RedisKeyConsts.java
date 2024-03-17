package com.biyao.search.ui.constant;

public class RedisKeyConsts {

    /**
     * 搜索标签设置信息
     * value: {"必粉最爱": {"color": "#AB7FD1"},"爆品": {"color": "#AB7FD1","textColor": "#FFFFFF","roundColor": "#AB7FD1"},"1天生产": {"color": "#D6B98C"},"2天生产": {"color": "#D6B98C"},"3天生产": {"color": "#D6B98C"},"签名定制": {"color": "#D6B98C"},"新品": {"color": "#D6B98C"},"精选": {"color": "#AB7FD1","textColor": "#FFFFFF","roundColor": "#AB7FD1"},"支持特权金": {"color": "#F7A701"},"一起拼": {"color": "#FFFFFF","textColor": "#FB4C81","roundColor": "#FB4C81"},"特权金": {"color": "#FFFFFF","textColor": "#FB4C81","roundColor": "#FB4C81"},"定制":{"textColor":"#FB4C81","roundColor":"#FB4C81","color":"#FFFFFF"}}
     * 更新: 有新的标签加入或者修改标签配置时，使用python脚本写入
     * zhaiweixi@idstaff.com
     */
    public static final String LABEL_CONFIG_CACHE = "search:product_label_info";

    /**
     * 搜索标签的默认颜色，不应该是RedisKey
     */
    public static final String DEFAULT_LABEL_COLOR = "#D6B98C";
    /**
     * 搜索结果页面板配置
     * value: {"onOff": "on", "orderByList": ["normal", "sale", "price"], "showStyle": "double", "bottomHanging": 0}
     * 更新: 有配置修改时，使用python脚本写入
     * zhaiweixi@idstaff.com
     */
    public static final String SEARCH_PANEL_CONFIG = "search:top_panel_config";
    /**
     * 判断redis里面key，1的时候走正常流程，其余托底数据 app3.x版本首页使用
     * value: 1
     */
    public final static String FLOORKEY = "search:home_floor_cache_key";
    /**
     * //判断redis里面key，1的时候走正常流程，其余托底数据 app3.x版本首页使用
     * value: 1
     */
    public final static String NOVAKEY = "search:home_floor_nova_key";
    /**
     * 首页个性化topic实验 app3.x版本首页使用，没多大用了
     */
    public static final String HOME_UU_TOPIC = "home:uu_topic_1";
    /**
     * 个人facet面板缓存
     */
    public static final String FACET_CACHE = "search:facet_";
    /**
     * uuid-浏览新品缓存
     */
    public static final String UUID_NEWPRODUCT_CACHE = "search:%s_newProduct";

    /**
     * 普通商品间隔数量
     */
    public static final String COMMONPRODUCT_INTERVAL = "search:commonProductInterval";

    /**
     * 衍生商品最大召回数量
     */
    public static final String DERIVEPRODUCT_SIZE = "search:deriveProductSize";

    /**
     * 衍生商品白名单
     */
    public static final String DERIVEPRODUCT_WHITELIST = "search:derive_whiteList";

    /**
     * 特殊口罩商品
     */
    public final static String MASK_LIST = "search:maskList";

    /**
     * 搜索召回日志白名单
     */
    public final static String SEARCH_DETAIL_LOG_LIST = "search:search_uuid_white_list";

    /**
     * 梦工厂流量分流
     * 格式为两个区间数字，以,分隔，例如：0,10
     * 区间判断为左闭右开
     */
    public final static String FLOW_LIMIT = "search:flowLimit";

    /**
     * 商品性别
     */
    public final static String PRODUCT_SEX = "search:product_sex_label";

    /**
     * 找相似开关
     */
    public final static String SIMILAR_PRODUCT_FLAG = "search:similar_product_flag";
    /**
     * 搜索词黑名单
     */
    public static final String BLOCK_QUERY = "search:blacklist_query";
}
