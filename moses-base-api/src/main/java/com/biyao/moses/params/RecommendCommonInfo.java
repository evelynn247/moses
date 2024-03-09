package com.biyao.moses.params;

import lombok.Getter;
import lombok.Setter;

/**
 * @program: moses-parent
 * @description:
 * @author: changxiaowei
 * @Date: 2022-02-21 13:50
 **/
@Getter
@Setter
public class RecommendCommonInfo {

    /**
     * 数据唯一id   可以表示 视频id  商品id 等
     */
    private String id;

    private String info;

}
