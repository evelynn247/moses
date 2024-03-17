package com.biyao.search.ui.util;

/**
 * @author biyao
 * @date
 */
@Deprecated
public class OperateUtils {

    public static final long  M = 1L ;
    public static final long  MINI = 1L<<1;
    public static final long APP = 1L<<2;

    /**
     *
     * @param states       当前的位状态码
     * @param operateState 判断是否存在的状态
     * @return 是否存在 true存在,false不存在
     * @Description: 判断是否存在某个状态
     * eg: states 此商品支持的站点
     *     operateState M站
     *     返回 是否支持M站
     */
    public static boolean hasState(long states,long operateState){
        return (states & operateState) != 0;
    }

    /**
     * @param states       当前的位状态码
     * @param operateState 添加的状态
     * @return 添加后的位状态码
     * @Description: 添加某个状态
     */
    public static long addState(long states, long operateState) {
        //当不存在该状态时才执行添加操作,否则直接返回原位状态码
        if (hasState(states, operateState)) {
            return states;
        }
        return (states | operateState);
    }

    /**
     * @param states       当前的位状态码
     * @param operateState 需要删除状态
     * @return 删除后的位状态码
     * @Description: 删除某个状态
     */
    public static long removeState(long states, long operateState) {
        //当存在该状态时才执行删除操作,否则直接返回原位状态码
        if (!hasState(states, operateState)) {
            return states;
        }
        return states ^ operateState;
    }

}
