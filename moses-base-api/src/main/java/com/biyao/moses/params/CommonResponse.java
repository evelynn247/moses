package com.biyao.moses.params;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * @ClassName CommonResponse
 * @Description Common rank的返回结果data
 * @Author admin
 * @Date 2019/8/5 20:20
 * @Version 1.0
 **/
@Setter
@Getter
public class CommonResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 排序后的商品id集合
     */
    private List<Long> pids;
    /**
     * 总页数
     */
    private Integer totalPage;
}
