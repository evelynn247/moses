package com.biyao.moses.model.rank2;

import com.biyao.moses.model.template.entity.TotalTemplateInfo;
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
public class RankItem2 {

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * match分数
     */
    private Double score;

    /**
     * 商品详细信息
     */
    private TotalTemplateInfo totalTemplateInfo;

}
