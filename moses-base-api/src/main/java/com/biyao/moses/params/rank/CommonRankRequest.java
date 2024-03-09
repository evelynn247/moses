package com.biyao.moses.params.rank;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @ClassName CommonRankRequest
 * @Description 通用Rank请求入参
 * @Author xiaojiankai
 * @Date 2019/8/5 17:10
 * @Version 1.0
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommonRankRequest {
    private static final long serialVersionUID = 1L;
    private String uuid;
    private String uid;
    /**
     * 标签ID
     */
    private String tagId;
    /**
     * 场景ID
     * 1: 一起拼运营聚合页， 2：特权金下发成功页
     */
    private String sceneId;
    /**
     * 待排序的pid集合
     */
    private List<Long> pids;
}
