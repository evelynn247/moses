package com.biyao.moses.match2.exp;

/**
 * @author xiaojiankai@idstaff.com
 * @date 2019/11/20
 **/
public interface MatchExpConst {
    /**
     * 实验层flag常量及flag对应的value
     */
    String FLAG_HOME_FEED = "sflag_home_feed";
    String VALUE_HOME_FEED_LAYER = "hfexplayer";

    /**
     * 基础召回源及其权重，
     * 其值格式为ibcf:0.4,tag:0.5,hots:0.1;
     * 其中ibcf、tag、hots分别为召回源bean名称，0.4、0.5、0.1分别为其权重
     */
    String FLAG_SOURCE_AND_WEIGHT = "sflag_source_and_weight";

    /**
     * 期望返回的商品数量上限
     */
    String FLAG_EXPECT_NUM_MAX = "sflag_expect_num_max";

    /**
     * 各个召回源的规则名称
     */
    String FLAG_RULE_NAME = "sflag_rule_name";

    /**
     * match对应的rank名称
     */
    String FLAG_RANK_NAME = "sflag_rank_name";

    /**
     * 默认值
     */
    String VALUE_DEFAULT = "DEFAULT";

    /**
     * ucb系列召回源名称前缀
     */
    String VALUE_UCB_PREFIX = "ucb";
}
