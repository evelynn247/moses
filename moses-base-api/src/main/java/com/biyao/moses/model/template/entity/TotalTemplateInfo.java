package com.biyao.moses.model.template.entity;

import com.biyao.moses.model.template.Label;
import com.biyao.moses.model.template.TemplateInfo;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 包含所有的模板信息 用来json转换，convert
 * @author monkey
 * @date 2018年8月30日
 */
@Data
@EqualsAndHashCode(callSuper=false)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TotalTemplateInfo extends TemplateInfo implements Serializable {

	private static final long serialVersionUID = 1L;

	private String mainTitle;
	
	//主标题颜色
	private String mainTitleColor;
	
	//tab导航栏颜色
	private String mainColor;
	//空白模板颜色
	private String color;

	private String subtitle;
	//子标题颜色
	private String subtitleColor;

	// 图片列表  短图
	private List<String> images;
	// 长图
	private List<String> longImages;

	private Integer routerType;

	private Map<String, String> routerParams;

	// ¥123元起
	private String priceStr;
	// 12300.00
	private String priceCent;

	private String priceColor;
	
	//商品图片
	private String image;
	//0不展示 1新品 2拼团 3一起拼
	private String isShowIcon;
	//朋友买过及好评的拼接字段
	private String thirdContent;
	
	// 空白行高度 20
	private String height = "20";
	// 网关拼装商品id
	private String id;
	// 轮播图图片
	private String imageUrl;
	// 选中颜色
	private String selectColor;
	// 是否选中
	private String isSelect;
	
	private List<Label> labels;
	
	private Double score;

	/**
	 * 特权金抵扣金额 单位元
	 */
	private String priDeductAmount;
	
	//三级类目id
	private String level2Category;

   //商品webp图片
	private String imageWebp;
	//商品webp图片集合
	private List<String> imagesWebp;
	//商品webp长图集合
	private List<String> longImagesWebp;

	// 530新增：打散用 商家id
	private String supplierId;
	
	//短图对应商品Id
	private List<Long> feedProducts;
	//长图对应商品Id
	private List<Long> feedLongProducts;

	//新手专享价格
	private String novicePrice;

	//spu下所有在售sku的标准销售属性信息，已废弃，该属性值可以从ProductInfo中获取
	@Deprecated
	private Map<String, Set<String>> spuStdSaleAttrs;
	private String skuId;
	private String skuPrice;
	/**
	 * 是否支持签名(0 不支持 1 支持)
	 */
	private Byte supportCarve;

	/**
	 * 低模商品类型：0-普通低模商品；1-眼镜低模商品
	 */
	private Byte rasterType;

	/**
	 * 新实验系统数据，用于埋点
	 * expId值为满足新实验系统的Flag值
	 * source值为召回源Id，多个召回源时，使用"_"分隔
	 */
	private String expId;
	private String source;

	/**
	 * x元购物返现活动新增
	 */
	/**
	 * 内容类型，"1" 商品， "2" 广告
	 */
	private String showType;
	private String adImage;//双排活动广告图片
	private String adImageWebp;
	private String adImageSingle;//单排活动广告图片
	private String adImageWebpSingle;
	/**
	 * 直播状态标签
	 * “0”：不展示标签， “1”：展示直播中标签
	 */
	private String liveStatus;
	/**
	 * 商品标签，来自召回源
	 */
	private String labelContent;


	/**
	 * 参数校验 RouterParams 是否为空
	 * @return
	 */
	public Boolean IsRouterParamsEmpty(){
		if(this.getRouterParams()==null || this.getRouterParams().size()==0){
			return true ;
		}
		return  false;
	}

}
