package com.biyao.search.ui.model;

import java.io.Serializable;


/**
 * @description: 搜索返回设计师信息对象
 * @author: guochong
 * @date: 2017年2月21日 下午3:41:46 
 */
public class UIDesigner implements Serializable{
	
	private static final long serialVersionUID = -1L;

	/**
	 * 跳转链接
	 */
	private String redirectUrl;
	
	/**
	 * 头像图片地址
	 */
	private String avatar;
	
	/**
     * 设计师名字
     */
    private String name;
	
	/**
	 * 设计师在售商品数
	 */
	private Integer design;
	
	/**
     * 关注数
     */
    private Integer follow;
    
    /**
     * 设计师简介
     */
    private String desc;

    public String getRedirectUrl() {
        return redirectUrl;
    }

    public void setRedirectUrl(String redirectUrl) {
        this.redirectUrl = redirectUrl;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getDesign() {
        return design;
    }

    public void setDesign(Integer design) {
        this.design = design;
    }

    public Integer getFollow() {
        return follow;
    }

    public void setFollow(Integer follow) {
        this.follow = follow;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }
}
