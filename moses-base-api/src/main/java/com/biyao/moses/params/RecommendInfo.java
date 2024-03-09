package com.biyao.moses.params;

import lombok.Getter;
import lombok.Setter;

/**
 * @ClassName RecommendInfo
 * @Description 推荐信息
 * @Author admin
 * @Date 2020/7/1 20:00
 * @Version 1.0
 **/
@Getter
@Setter
public class RecommendInfo {
    private static final long serialVersionUID = 1L;
    /**
     * id唯一标识，店铺ID或衍生商品spuID或普通商品spuID
     */
    private String id;
    /**
     * SCM埋点信息 格式为a.b.c.d
     */
    private String scm;
}
