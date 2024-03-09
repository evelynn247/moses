package com.biyao.moses.rules;

import com.biyao.moses.context.ByUser;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;
import com.biyao.moses.params.AdvertInfo;
import com.biyao.moses.params.BaseRequest2;
import com.biyao.moses.params.UIBaseRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author zhaiweixi@idstaff.com
 * @date 2019/10/12
 **/
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RuleContext{

    /**
     * uuid
     */
    private String uuid;
    /**
     * uid
     */
    private Integer uid;
    /**
     * 用户类型  1:老客  2:新访客  3:老访客
     */
    private Integer upcUserType;
    /**
     * 用户性别  0:男 1:女 -1:中性
     */
    private Integer userSex;
    /**
     * 待重排序列表
     */
    private List<TotalTemplateInfo> allProductList;
    /**
     * 待展示的活动广告入口集合
     */
    private List<AdvertInfo> advertInfoList;

    private BaseRequest2 baseRequest2;

    private ByUser byUser;
    /**
     * 待插入商品集合
     */
    private List<TotalTemplateInfo> waitInsertProductList;

    private UIBaseRequest uiBaseRequest;
}
