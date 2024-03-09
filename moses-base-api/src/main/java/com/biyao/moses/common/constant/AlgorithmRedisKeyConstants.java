package com.biyao.moses.common.constant;

/**
 * @ClassName AlgorithmRedisKeyConstants
 * @Description 算法Redis key汇总
 * @Author xiaojiankai
 * @Date 2019/12/23 19:21
 * @Version 1.0
 **/
public class AlgorithmRedisKeyConstants {
    /**
     * bert召回源商品id对应的redis key前缀，类型为String
     * redis value数据格式：pid:score,pid:score,...,pid:score
     * 数据刷入：管梓壮
     * 刷入时间：实时
     * 备注：如果有uid，则通过uid获取；如果没有uid，则使用uuid获取
     */
    public static final String MOSES_BERT_PREFIX = "moses:bert_";

    /**
     * ucb召回源商品id对应的redis key前缀，类型为String
     * redis value数据格式：pid:score,pid:score,...,pid:score
     * 数据刷入：李昭
     * 刷入时间：每隔10分钟
     */
    public static final String MOSES_MAB_UCB_MALE = "moses:mab_ucb_male";
    public static final String MOSES_MAB_UCB_FEMALE = "moses:mab_ucb_female";
    public static final String MOSES_MAB_UCB_COMMON = "moses:mab_ucb_common";

    /**
     * ucb2召回源区分用户性别的商品id对应的redis key前缀，类型为String
     * redis value数据格式：pid:score,pid:score,...,pid:score
     * 数据刷入：李昭
     * 刷入时间：每隔10分钟
     */
    public static final String MOSES_MAB_UCB2_MALE = "moses:mab_ucb2_male";
    public static final String MOSES_MAB_UCB2_FEMALE = "moses:mab_ucb2_female";
    public static final String MOSES_MAB_UCB2_COMMON = "moses:mab_ucb2_common";
    /**
     * ucb2召回源个性化商品id对应的redis key前缀，类型为String
     * redis value数据格式：pid:score,pid:score,...,pid:score
     * 数据刷入：郑世安
     * 刷入时间：每天凌晨2点半
     */
    public static final String MOSES_MAB_UCB2_PREFIX = "moses:mab_ucb2_";

    /**
     * ucb通用召回源商品id对应的redis key前缀，类型为String
     * redis value数据格式：pid:score,pid:score,...,pid:score
     * 数据刷入：郑世安
     * 刷入时间：每天凌晨2点半
     */
    public static final String MOSES_MAB_UCB_PREFIX = "moses:mab_ucb";

    /**
     * 性别后缀
     */
    public static final String MOSES_MALE_SUFFIX = "_male";
    public static final String MOSES_FEMALE_SUFFIX = "_female";
    public static final String MOSES_COMMON_SUFFIX = "_common";

    /**
     * acrec召回源商品id对应的redis key前缀，类型为String
     * redis value数据格式：pid:score,pid:score,...,pid:score
     * 数据刷入：管梓壮
     * 刷入时间：实时
     * 备注：先通过uid获取，获取不到再通过uuid获取
     */
    public static final String MOSES_ACREC_PREFIX = "moses:acrec_";

    /**
     * bert2召回源商品id对应的redis key前缀，类型为String
     * redis value数据格式：pid:score,pid:score,...,pid:score
     * 数据刷入：管梓壮
     * 刷入时间：
     * 备注：如果有uid，则通过uid获取；如果没有uid，则使用uuid获取
     * 复购提升专项V2.0-加价购及相关优化项目新增
     */
    public static final String MOSES_BERT2_PREFIX = "moses:bert2_";

    /**
     * bert2召回源查找相似商品redis key，类型为hash
     * 二级key为：pid， value数据格式：pid:score,pid:score,...,pid:score
     * 数据刷入：管梓壮
     * 刷入时间：
     * 备注：复购提升专项V2.0-加价购及相关优化项目新增
     */
    public static final String MOSES_BERT2_SIMILAR = "moses:bert2_similar_pid";


    /**
     * 轮播图落地页相似商品召回源redis key前缀，类型为string
     * value数据格式：pid:score,pid:score,...,pid:score
     * 数据刷入：宋浩男
     * 刷入时间：每天
     * 过期时间：3天
     * 备注：推荐V2.24.0-轮播图及分类页优化项目新增
     */
    public static final String MOSES_LBTLDY_SIMILAR_PREFIX = "moses:like_pid_";

    /**
     * 商品和视频的关系  类型：hash field：product
     * value数据格式：json
     * 数据刷入
     * 刷入时间 ：10分钟一次
     * 过期时间：3天
     * 备注：内容策略V1.0-商品内容视频化项目新增
     */
    public static final String PRODUICT_VEDIO_REDIS ="video_info";
    /**
     * 记录被提权的商品和视频时间  类型：hash field：productId
     * value数据格式：vid:time,vid1:time,
     * 数据刷入
     * 过期时间：90天
     * 备注：内容策略V1.0-商品内容视频化项目新增
     */
    public static final String MOSES_VID_PROMOTE = "moses:vid:promote";

    /**
     * 相似商品  有视频
     * 类型：String   key：pss_video:{clientId}:{scendId}:{pid}
     * value数据格式：pid，pid2 ,
     * 数据刷入
     * 过期时间：90天
     * 备注：内容策略V1.0-商品内容视频化项目新增
     */
    public static final String VIODE_SIMILAR_PID = "pss_video:";
}
