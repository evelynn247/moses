package com.biyao.search.ui.model;

import lombok.*;

/**
 * @author zhaiweixi@idstaff.com
 * @date 2019/7/23
 **/
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchItem {
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
}
