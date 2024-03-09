package com.biyao.moses.drools;

import com.biyao.moses.model.match2.MatchItem2;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @program: moses-parent
 * @description:
 * @author: changxiaowei
 * @create: 2021-03-25 11:32
 **/
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FilterContext {

    private String uuid;

    private Integer uid;
    /**
     * 待过滤的结果集
     */
    private List<MatchItem2> matchItem2List;
    /**
     * 最大期望数量
     */
    private Integer expectMaxNum;

}
