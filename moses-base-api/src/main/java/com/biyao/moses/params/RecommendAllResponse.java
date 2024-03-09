package com.biyao.moses.params;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * @ClassName RecommendAllResponse
 * @Description 所有推荐数据响应
 * @Author admin
 * @Date 2020/7/1 19:58
 * @Version 1.0
 **/
@Getter
@Setter
public class RecommendAllResponse {
    private static final long serialVersionUID = 1L;
    /**
     * 推荐信息集合
     */
    private List<RecommendInfo> recommendInfoList;
}
