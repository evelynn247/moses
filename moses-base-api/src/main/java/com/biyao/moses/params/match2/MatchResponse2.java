package com.biyao.moses.params.match2;

import com.biyao.moses.model.match2.MatchItem2;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * @author zhaiweixi@idstaff.com
 * @date 2019/10/11
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
