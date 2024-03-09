package com.biyao.moses.params.rank2;

import com.biyao.moses.model.rank2.RankItem2;
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
public class RankResponse2 implements Serializable {

    private static final long serialVersionUID = 4185955586183987338L;

    private List<RankItem2> rankItem2List;

    /**
     * 实验ID，多个时以"_"分隔
     */
    private String expId;

}
