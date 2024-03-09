package com.biyao.moses.common.constant;

import java.util.HashSet;
import java.util.Set;

/**
 * @program: moses-parent-online
 * @description:
 * @author: changxiaowei
 * @create: 2021-10-09 16:22
 **/
public class CommonConstant {
    public static final int ONE = 1;
    public static final int ZERO = 0;
    /**
     * 处理失败
     */
    public static final int MQ_LOG_STATUS_FAILED = 0;
    /**
     * 处理成功
     */
    public static final int MQ_LOG_STATUS_SUCC = 1;
    /**
     * 默认 -1数组
     */
    public static  final Long [] DEFAULARR= new Long[1];
    // 不需要分端隐藏的端 4 小程序A 5 小程序B   cps1.1  新增  4 5  cps 1.3 新增 0
    public static final Set<Byte> NO_SITE_FILTER = new HashSet<Byte>(){
        private static final long serialVersionUID = -6437329809897547690L;
        {add((byte)4); add((byte)5);add((byte)0);}
    };
}
