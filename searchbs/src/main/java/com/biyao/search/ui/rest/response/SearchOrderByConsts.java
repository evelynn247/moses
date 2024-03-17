package com.biyao.search.ui.rest.response;

import com.biyao.search.common.enums.SearchOrderByEnum;

public class SearchOrderByConsts {
    public static final String ONE_WAY = "oneWay"; // 单向排序方式
    public static final String TWO_WAY = "twoWay"; // 双向排序方式

    /**
     * 综合
     */
    public static final SearchOrderBy NORMAL = new SearchOrderBy(ONE_WAY, "综合",
            SearchOrderByEnum.NORMAL.getCode(), SearchOrderByEnum.NORMAL.getCode());


    public static final SearchOrderBy HOT = new SearchOrderBy(ONE_WAY, "热门",
            SearchOrderByEnum.HOT.getCode(), SearchOrderByEnum.HOT.getCode());

    /**
     * 销量
     */
    public static final SearchOrderBy SALE = new SearchOrderBy(ONE_WAY, "销量",
            SearchOrderByEnum.SALE_QUANTITY.getCode(), SearchOrderByEnum.SALE_QUANTITY.getCode());

    /**
     * 销量
     */
    public static final SearchOrderBy NEW = new SearchOrderBy(ONE_WAY, "上新",
            SearchOrderByEnum.NEW.getCode(), SearchOrderByEnum.NEW.getCode());

    /**
     * 价格
     */
    public static final SearchOrderBy PRICE = new SearchOrderBy(TWO_WAY, "价格",
            SearchOrderByEnum.PRICE_ASC.getCode(), SearchOrderByEnum.PRICE_DESC.getCode());

    public static SearchOrderBy getSearchOrderBy(String name) {
        switch (name) {
            case "normal":
                return NORMAL;
            case "sale":
                return SALE;
            case "price":
                return PRICE;
            case "new":
                return NEW;
            default:
                return NORMAL;
        }
    }

    public static SearchOrderBy getGroupBuyOrderBy(String name) {
        switch (name) {
            case "normal":
                return HOT;
            case "sale":
                return SALE;
            case "price":
                return PRICE;
            case "new":
                return NEW;
            default:
                return NORMAL;
        }
    }
}
