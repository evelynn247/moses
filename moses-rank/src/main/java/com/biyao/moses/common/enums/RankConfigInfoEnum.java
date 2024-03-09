package com.biyao.moses.common.enums;
/**
 * moses-rank 保持在Redis中的配置信息
 * 注意：redis中moses:rank_config （hash key）对应的field 一定要填写为name配置名称
 */
public enum RankConfigInfoEnum {

    ReturnRefundRateLimit("returnRefundRateLimit", null,"商品受退货退款率惩罚的最小退货退款率和推荐推出商品的最大退货退款率");

    /**
     * 配置名称
     */
    private String name;

    /**
     * 默认值
     */
    private String defaultValue;
    /**
     * 配置描述
     */
    private String desc;

    RankConfigInfoEnum(String name, String defaultValue, String desc){
        this.name = name;
        this.defaultValue = defaultValue;
        this.desc = desc;
    }

    public String getName() {
        return name;
    }

    public String getDesc() {
        return desc;
    }

    public String getDefaultValue() {
        return defaultValue;
    }
}
