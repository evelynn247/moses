package com.biyao.moses.constant;

import com.biyao.moses.common.constant.EsIndexConstant;

import java.util.HashSet;
import java.util.Set;

/**
 * @program: moses-parent-online
 * @description:
 * @author: changxiaowei
 * @create: 2021-10-09 17:53
 **/
public class CommonConstant {
    /**
     * 普通商品索引别名前缀
     */
    public static final String PRODUCT_INDEX_ALIAS = "by_moses_product_alias_";
    //match异步获取召回源数据最大等待时间
    public static final int MATCH_MAX_WAIT_TIME = 500;
    public static final String [] fetchSource ={EsIndexConstant.PRODUCT_ID};
    //眼镜后台二级类目ID 眼镜39(低模眼镜) 眼镜55(隐形眼镜)
    public static final Set<Long> GLASSES_CATEGORY2_IDS = new HashSet<Long>(){
        {add(39L); add(55L);}
    };
    //视频流落地页相关的场景id
    public static final Set<Integer> VIDEO_SCENDIDS = new HashSet<Integer>(){
        {add(2901); add(2902); add(2903);add(29);}
    };

}
