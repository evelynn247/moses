package com.biyao.moses.params;

import lombok.Getter;
import lombok.Setter;

/**
 * 测试请求实体类
 */
@Setter
@Getter
public class UITestRequest extends UIBaseRequest{
    //uuid
    private String uuid;
    //uid
    private Integer uid;
    //页面ID
    private String pvid;
    //请求页数
    private String pageIndex;
    //源标识
    private String source;
    //后台三级类目拼接 666,234,444
    private String categoryIds;
    //scmId拼接
    private String scmIds;
    // 排序类型 ： 综合 all 0  销量  sale 1  价格 price  2 | 筛选特权金商品 3
    private String sortType;
    // 排序值 : 价格升序 0  降序 1
    private String sortValue;
    //前台一级类目
    private String frontendCategoryId;
    //置顶商品ID（平台核心转化V2.1置顶商品ID）
    private String priorityProductId;
    //筛选的属性信息（平台核心转化V2.2新增），格式如下：
    //{
    //"color": ["红色", "黑色", "白色"],
    //"size": ["27", "28", "29"]
    //}
    private String selectedScreenAttrs;
    //服务端唯一标识
    private String sid;

}
