package com.biyao.moses.params;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * @ClassName RecommendPidsResponse
 * @Description 响应结果
 * @Author xiaojiankai
 * @Date 2020/3/30 14:01
 * @Version 1.0
 **/
@Getter
@Setter
public class RecommendPidsResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 推出的商品pid集合
     */
    private List<String> pids;
}
