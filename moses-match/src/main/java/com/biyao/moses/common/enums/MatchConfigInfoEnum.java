package com.biyao.moses.common.enums;

/**
 * moses-match 保持在Redis中的配置信息
 * 注意：redis中moses:match_config （hash key）对应的field 一定要填写为name配置名称
 */
public enum MatchConfigInfoEnum {

    BaseSource24hPidNumMaxLimit("baseSource_24hPidNumMaxLimit", "0","基础流量召回源每用户24小时内推出的商品个数上限"),
    BaseSourceExpectPidNunMaxLimit("baseSource_expectPidNumLimit", "0","基础流量召回源每个用户每次请求期望输出的商品个数上限")
    ;

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

    MatchConfigInfoEnum(String name, String defaultValue, String desc){
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
