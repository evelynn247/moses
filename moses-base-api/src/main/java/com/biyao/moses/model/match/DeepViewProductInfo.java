package com.biyao.moses.model.match;
import lombok.Data;

/**
 * @program: moses-parent
 * @description: 深度浏览商品信息
 * @author: changxiaowei
 * @create: 2021-03-02 16:46
 **/
@Data
public class DeepViewProductInfo {
    /**
     * 商品id
     */
    private Long pid;
    /**
     * 深度浏览时间
     */
    private Long viewTime;

    public DeepViewProductInfo(Long pid,Long viewTime){
        this.pid=pid;
        this.viewTime=viewTime;
    }
}
