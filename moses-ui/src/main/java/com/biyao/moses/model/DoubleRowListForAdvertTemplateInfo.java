package com.biyao.moses.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * @ClassName DoubleRowListForAdvertTemplateInfo
 * @Description 普通商品+广告模板双排样式
 * @Author xiaojiankai
 * @Date 2020/3/6 10:19
 * @Version 1.0
 **/
@Getter
@Setter
public class DoubleRowListForAdvertTemplateInfo  extends PriDoubleRowListTemplateInfo implements Serializable {
    /**
     * 内容类型，"1" 商品， "2" 广告
     */
    private String showType;
    /**
     * 广告入口图地址
     */
    private String adImage;
    /**
     * 广告入口图webp格式地址
     */
    private String adImageWebp;
}
