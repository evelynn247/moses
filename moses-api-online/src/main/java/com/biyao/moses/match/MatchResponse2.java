package com.biyao.moses.match;

import lombok.*;
import java.io.Serializable;
import java.util.List;
/**
 * @program: moses-parent-online
 * @description:
 * @author: changxiaowei
 * @create: 2021-09-14 10:26
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MatchResponse2 implements Serializable {

    private static final long serialVersionUID = -2246435765460160849L;

    List<MatchItem2> matchItemList;
    String expId;
    String rankName;
    String ruleId;
}
