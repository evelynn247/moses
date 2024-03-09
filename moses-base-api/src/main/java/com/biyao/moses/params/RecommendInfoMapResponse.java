package com.biyao.moses.params;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * @ClassName RecommendProductCardsResponse
 * @Description 响应结果
 * @Author changxw
 * @Date 2020/11/16 11:53
 * @Version 1.0
 **/
@Getter
@Setter
public class RecommendInfoMapResponse {

    /**
     * 用户好友们已购买商品list集合
     */
    private Map<String,List<RecommendInfo>> recommendInfoMap;
}
