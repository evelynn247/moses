package com.biyao.moses.exp;

import com.biyao.moses.common.constant.BizNameConst;
import com.biyao.moses.common.constant.RankNameConstants;

/**
 * @author zhaiweixi@idstaff.com
 * @date 2019/10/12
 **/
public interface MosesExpConst {

    /**
     * 实验层flag常量及flag对应的value
     */
    String FLAG_HOME_FEED = "sflag_home_feed";
    String VALUE_HOME_FEED_EXP = "hfexp";
    String VALUE_HOME_FEED_OLD = "hfold";

    /**
     * 业务召回增加基于标签的召回源
     */
    String VALUE_HOME_FEED_EXP2 = "hfexp2";

    /**
     * 业务召回增加基于实时浏览的召回源
     */
    String VALUE_HOME_FEED_EXP3 = "hfexp3";

    String VALUE_HOME_FEED_LAYER = "hfexplayer";
    //轮播图落地页实验ID
    String SLIDER_MIDDLE_PAGE_EXP = "lbtldy";
    //轮播图实验ID
    String SLIDER_EXP = "lbt";
    //买二返一频道页热门实验ID
    String M2F1_PAGE_EXP_ID = "m2f1";

    String VALUE_DEFAULT = "DEFAULT";
    /**
     * 机制bean名称的集合
     */
    String FLAG_RULE_NAME = "sflag_rule_name";
    /**
     * 新手专享实验参数
     */
    String FLAG_NEW_USER_FEED = "sflag_new_user";
    String VALUE_NEW_USER_FEED_EXP = "newnu";
    String VALUE_NEW_USER_FEED_OLD = "oldnu";

    /**
     * feed流横叉召回源及其权重参数
     */
    String FLAG_INSERT_MATCH_SOURCE_WEIGHT = "sflag_insert_match_source_weight";
    String VALUE_DEFAULT_INSERT_MATCH_SOURCE_WEIGHT = "";
    /**
     * feed流横叉机制bean名称的集合
     */
    String FLAG_INSERT_RULE_NAME = "sflag_insert_rule_name";
    /**
     * feed流横插位置信息，多个位置以逗号分隔
     */
    String FLAG_INSERT_POSITIONS = "sflag_insert_positions";

    /**
     * rank bean名称参数
     */
    String FLAG_RANK_NAME = "sflag_rank_name";
    String VALUE_DEFAULT_RANK_NAME = RankNameConstants.REDUCE_EXPOSURE_WEIGHT;

    /**
     * 轮播图落地页match bean名称参数
     */
    String FLAG_LBTLDY_MATCH_NAME = "sflag_lbtldy_match_name";
    String VALUE_DEFAULT_LBTLDY_MATCH_NAME = BizNameConst.SLIDER_MIDDLE_PAGE2;

    /**
     * 轮播图落地页走新老实验系统配置参数，
     * 该值为old，则表示走老实验系统；值为new，则表示做新实验系统
     */
    String FLAG_LBTLDY_FEED = "sflag_lbtldy_feed";
    String VALUE_LBTLDY_FEED_OLD = "oldldy";
    String VALUE_LBTLDY_FEED_NEW = "newldy";

    /**
     * match过滤规则配置
     */
    String FLAG_MATCH_RULE_NAME = "sflag_match_rule_name";

    /**
     * 人工配置召回源
     */
    String FLAG_MANUAL_SOURCES = "sflag_manual_sources";

    /**
     * 判断是否进入了感兴趣商品集实验
     */
    String FLAG_GXQSP_NEWEXP = "sflag_gxqsp_newExp";

    /**
     * 进入了实验 结果值
     */
    String VALUE_GXQSP_NEWEXP = "true";
}
