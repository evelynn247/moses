package com.biyao.moses.common.enums;

/**
 * @ClassName MosesConfTypeEnum
 * @Description mosesconf配置类型
 * @Author xiaojiankai
 * @Date 2019/8/16 17:18
 * @Version 1.0
 **/
public enum MosesConfTypeEnum {
    PageConf(1, "页面配置"), ExpConf(2, "实验配置"),SwitchConf(3,"开关配置"),VersionConf(4,"版本配置");

    private Integer type;
    private String des;

    private MosesConfTypeEnum(Integer type, String des){
        this.type = type;
        this.des = des;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getDes() {
        return des;
    }

    public void setDes(String des) {
        this.des = des;
    }

    public static MosesConfTypeEnum getMosesConfTypeEnumByType(Integer type){
        for(MosesConfTypeEnum mosesConfTypeEnum : values()){
            if(type.intValue() == mosesConfTypeEnum.getType().intValue()){
                return mosesConfTypeEnum;
            }
        }
        return null;
    }
}
