package com.biyao.moses.model.feature;

/**
 * @description: 在线解析特征配置定义
 */
public class OnlineParseFeaConf {
    /**
     * 特征名称
     */
    private String feaName;

    /**
     * 特征是否直接参与分数计算
     */
    private Integer needCalcu;

    /**
     * 特征解析公式
     */
    private String formula;

    /**
     * 缺失默认值
     */
    private String defaultValue;

    private String paramA;//解析用的参数
    private String paramB;//解析用的参数

    public String getFeaName() {
        return feaName;
    }

    public void setFeaName(String feaName) {
        this.feaName = feaName;
    }

    public Integer getNeedCalcu() {
        return needCalcu;
    }

    public void setNeedCalcu(Integer needCalcu) {
        this.needCalcu = needCalcu;
    }

    public String getFormula() {
        return formula;
    }

    public void setFormula(String formula) {
        this.formula = formula;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getParamA() {
        return paramA;
    }

    public void setParamA(String paramA) {
        this.paramA = paramA;
    }

    public String getParamB() {
        return paramB;
    }

    public void setParamB(String paramB) {
        this.paramB = paramB;
    }

    @Override
    public String toString() {
        return "OnlineParseFeaConf [feaName=" + feaName + ", needCalcu="
                + needCalcu + ", formula=" + formula + ", defaultValue="
                + defaultValue + ", paramA=" + paramA + ", paramB=" + paramB
                + "]";
    }
}