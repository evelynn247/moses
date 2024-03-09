package com.biyao.moses.common.constant;

/**
 * @author zhaiweixi@idstaff.com
 * @date 2019/10/11
 **/
public interface RankNameConstants {
    /**
     * moses rank V1.0
     */
    /**
     * 通用排序
     */
    String COMMON_RANK = "CommonRank";
    /**
     * 默认排序
     */
    String DEFAULT_RANK = "dr";
    /**
     * 分类页排序
     */
    String CATEGORY_SHUFFLE_RANK = "csr";

    /**
     * V2.0 rank
     */
    /**
     * 曝光降权排序，包括曝光因子+尺码因子+价格因子
     */
    String REDUCE_EXPOSURE_WEIGHT="rew";
    /**
     * 基础降权排序，包括曝光因子+尺码因子
     */
    String BASE_REDUCE_WEIGHT_RANK="brwr";
    /**
     * 默认排序V2.0
     */
    String DEFAULT_RANK2 = "dr2";
    /**
     * Drools 排序规则排序
     */
    String DROOLS_RNAK = "droolsRank";
    /**
     * 新轮播图曝光降权排序
     */
    String SLIDER_PICTURE_RANK = "lbtRank";

    /**
     * ctvr排序
     */
    String CTVR_RANK = "ctvr";

    /**
     * 类目页分层排序
     */
    String CATEGORY_LEVEL_RANK = "clr";

    /**
     * 分类页算法rank对应的bean名称
     */
    String CATEGORY_ALGORITHM_RANK = "car";

    /**
     * 分类页算法新rank对应的bean名称
     */
    String CATEGORY_ALGORITHM_RANK2 = "car2";
}
