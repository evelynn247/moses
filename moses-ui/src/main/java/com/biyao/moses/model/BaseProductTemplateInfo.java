package com.biyao.moses.model;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

import com.biyao.moses.model.template.Label;
import com.biyao.moses.model.template.TemplateInfo;

/**
 * 商品模板base
 * 
 * @author monkey
 * @date 2018年11月13日
 */
@Setter
@Getter
public class BaseProductTemplateInfo extends TemplateInfo {

	private static final long serialVersionUID = 1L;

	private String id;
	// 商品标题
	private String mainTitle;

	// 商品图片
	private String image;
	
	// 价格
	private String priceStr;
	// 价格（是priceStr数字的100倍）
	private String priceCent;
	// 标签
	private List<Label> labels;
	//商品webp格式图片
	private String imageWebp;

	/**
	 * 是否支持签名(0 不支持 1 支持)
	 */
	private Byte supportCarve ;

	/**
	 * 低模商品类型：0-普通低模商品；1-眼镜低模商品
	 */
	private Byte rasterType ;

	/**
	 * 对所有人可见的好评数（包含默认好评数）
	 */
	private Integer goodCommentAll;

	/**
	 * 旧好评数数字
	 */
	private Integer goodComment;

}