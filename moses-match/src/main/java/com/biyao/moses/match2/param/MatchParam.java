package com.biyao.moses.match2.param;

import com.uc.domain.bean.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author zhaiweixi@idstaff.com
 * @date 2019/10/9
 **/
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MatchParam {
    /**
     * uuid
     */
    private String uuid;
    /**
     * uid
     */
    private Integer uid;
    /**
     * 机型
     */
    private String device;
    /**
     * 用户类型  1:老客  2:新访客  3:老访客
     */
    private Integer upcUserType;
    /**
     * 用户性别  0:男 1:女 -1:未知
     */
    private Integer userSex;
    /**
     * 权重
     */
    private Double weight = 1.0;
    /**
     * pid集合（需要查找相似商品的商品pid集合）
     */
    private List<String> pidList;

    /**
     * 配置的召回源名称
     */
    private String source;

    /**
     * 数据获取策略。
     * 1：只使用uid获取数据；
     * 2：只使用uuid获取数据；
     * 3: 优先使用uid获取数据，如果没有获取到数据，则使用uuid获取数据；
     * 4：如果有uid则使用uid获取数据，如果没有则使用uuid
     * 如果召回源不配此项，则默认使用策略4获取数据
     */
    private String dataStrategy;
    /**
     * 数据从哪个redis中获取。
     * 1：从1007 算法redis中获取
     * 2：从1005 match redis中获取
     */
    private String redis;
    /**
     * ucb数据号，格式为 dataNum1,dataNum2
     */
    private String ucbDataNum;

    /**
     * 从uc获取的用户信息
     */
    private User ucUser;
    /**
     * 必要朋友V2.0项目新增
     * 好友已购业务表示：好友uid列表
     * 格式为：id1，id2，id3
     */
    private String responseMapKeys;
    /**
     * 召回源是否是人工配置  默认为false
     */
    private Boolean isMunmanualSource=false;
    /**
     * 是否需要过滤必要造物商品
     */
    private String isFilterByZw;
}
