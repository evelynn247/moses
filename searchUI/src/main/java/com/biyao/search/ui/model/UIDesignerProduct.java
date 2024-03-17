package com.biyao.search.ui.model;

import java.io.Serializable;


/**
 * @description: 搜索返回商品信息对象
 * @author: luozhuo
 * @date: 2017年2月21日 下午3:41:46 
 */
public class UIDesignerProduct implements Serializable{
	
	private static final long serialVersionUID = -1L;

	/**
     * 设计ID
     */
    private String redirectUrl;
    
	/**
	 * 商品图片地址
	 */
	private String image;
	
	/**
	 * 商品价格
	 */
	private Float price;
	
	/**
	 * 商品短标题
	 */
	private String name;

    public String getRedirectUrl() {
        return redirectUrl;
    }

    public void setRedirectUrl(String redirectUrl) {
        this.redirectUrl = redirectUrl;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public Float getPrice() {
        return price;
    }

    public void setPrice(Float price) {
        this.price = price;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
