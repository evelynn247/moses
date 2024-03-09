package com.biyao.moses.model.match2;

import lombok.*;

/**
 * @author zhaiweixi@idstaff.com
 * @date 2019/10/11
 **/
@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MatchItem2 {
    /**
     * 商品ID
     */
    private Long productId;

    /**
     * match分数
     */
    private Double score;

    /**
     * match来源
     */
    private String source;

    /**
     * 入口SU
     */
    private String suId;

    /**
     * 商品标签文本信息，目前只设置1个标签
     */
    private String labelContent;

    /**
     * 衍生商品spuId或者梦工厂店铺id
     */
    private String id;
    /**
     * 表示该商品的所有者
     * 必要朋友2.0 好友已购业务中表示 该商品被哪个好友购买
     */
    private String ownerId;

}
