package com.biyao.moses.bean;

import lombok.Data;
/**
* @Description 深度浏览日志
* @date 2019年12月31日下午2:27:41
* @version V1.0 
* @author 邹立强 (zouliqiang@idstaff.com)
 */
@Data
public class DeepViewLog implements java.io.Serializable{
	
	private static final long serialVersionUID = 6056100094517468023L;
    /**
     * 商品主图滑动次数
     */
    private Integer pslipNum=0;
    /**
     * 评论页翻页次数
     */
    private Integer cslipNum=0;
    /**
     * 评论页图片点击次数
     */
    private Integer cimgNum=0;
    /**
     * 是否收藏
     */
    private Integer fav=0;
    /**
     * 是否加购
     */
    private Integer cart=0;
    /**
     * 是否查看评论
     */
    private Integer cmt=0;
    /**
     * 是否查看有图评论
     */
    private Integer pcmt=0;
    /**
     * 是否查看商品图文详情
     */
    private Integer pd=0;
    /**
     * 是否点击商品主图放大
     */
    private Integer mpic=0;
    /**
     * 是否点击购买(单独购买拼团起拼开团、一起拼参团)
     */
    private Integer buy=0;
    /**
     * 浏览时间
     */
    private String st;
    
	public DeepViewLog(Integer pslipNum, Integer cslipNum, Integer cimgNum, Integer fav, Integer cart, Integer cmt,
			Integer pcmt, Integer pd, Integer mpic, Integer buy) {
		super();
		this.pslipNum = pslipNum;
		this.cslipNum = cslipNum;
		this.cimgNum = cimgNum;
		this.fav = fav;
		this.cart = cart;
		this.cmt = cmt;
		this.pcmt = pcmt;
		this.pd = pd;
		this.mpic = mpic;
		this.buy = buy;
	}
	
	public DeepViewLog() {
		super();
	}
}
