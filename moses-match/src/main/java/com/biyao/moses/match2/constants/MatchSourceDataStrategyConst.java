package com.biyao.moses.match2.constants;

public interface MatchSourceDataStrategyConst {
    /**
     * 只使用uid获取数据
     */
    String DATA_STATEGY_UID = "1";
    /**
     * 只使用uuid获取数据
     */
    String DATA_STATEGY_UUID = "2";
    /**
     * 如果使用uid获取到数据，则不再适用uuid获取数据；
     * 如果使用uid没有获取到数据，则使用uuid获取数据
     */
    String DATA_STATEGY_UID_NODATA_UUID = "3";
    /**
     * 如果有uid且uid不为0则使用uid获取数据，不再使用uuid获取数据；
     * 如果没有uid或uid为0则使用uuid
     */
    String DATA_STATEGY_UID_NO_UUID = "4";

    /**
     * 兜底数据策略，只有1个redis key
     */
    String DATA_STATEGY_COMMON = "common";
    /**
     * 用户性别维度兜底数据策略，有3个redis key
     */
    String DATA_STATEGY_COMMON_SEX = "sex";

}
