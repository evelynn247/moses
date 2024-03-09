package com.biyao.moses.model.match;

import lombok.*;

/**
 *
 *
 * @date 2019-07-23
 * @author zhaiweixi@idstaff.com
 **/

@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MatchItem {
    /**
     * 商品ID
     */
    private Long productId;

    /**
     * match分数
     */
    private Double score = 0.0;

    /**
     * match来源
     */
    private String source;
}
