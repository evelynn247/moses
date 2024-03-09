package com.biyao.moses.es;

import com.by.profiler.util.StringUtil;
import lombok.Getter;
import lombok.Setter;

/**
 * @program: moses-parent-online
 * @description:
 * @author: changxiaowei
 * @create: 2021-09-17 20:49
 **/
@Setter
@Getter
public class EsRequest {
    /**
     * 小时
     */
    public Integer time;
    /**
     * spu商品id集合，多个以英文逗号分隔
     */
    public String pids;

    /**
     * 秘钥
     */
    public String secretkey;

    public boolean isValid(){
        if(!"moseses".equals(secretkey)){
            return  false;
        }
        if(time == null && StringUtil.isBlank(pids)){
            return false;
        }
        return true;
    }
}
