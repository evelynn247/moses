package com.biyao.search.ui.remote.response;

import com.biyao.search.ui.model.SaleAgent;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 搜索商品模板
 * @author zhaiweixi@idstaff.com
 * @date
 */
@Data
public class SearchProduct extends TemplateData {
	private static final long serialVersionUID = 1L;
	private String title;
	private String fullTitle;
	private String salePoint;
	private String image;
	private Integer productId;
	private String suId;
	private String priceStr;
	private Integer position;
	private Integer isShowIcon;
	private String productDes;
	private String commentInfo;
	private String redirectUrl;
	private String trackParam = "";
	private List<ProductLabel> labels = new ArrayList<>();
	/**
	 * es对商品和query的相关性评分
	 */
	private transient Double score = 0.0;
	/**
	 * 制造商背景
	 */
	private String background;
	/**
	 * 一起拼，拼后价
	 */
	private String groupBuyPriceStr;
	/**
	 * 多少个好友买过，仅对小程序和M站生效
	 */
	private String friendBuy;
	/**
	 * 阶梯团价格
	 */
	private String groupPriceStr;
	/**
	 * webp格式图片
	 */
	private String imageWebp;


	private String privilegeInfo;

	/**
	 * 大运河V1.4新增 身份信息
	 */
	private SaleAgent saleAgent;

	/**
	 * 大运河V1.4新增 pid,衍生商品为29位
	 */
	private String spuId;

	/**
	 * 大运河V1.4新增 是否是衍生商品，1:是，0:否。默认是0
	 */
	private Integer customFlag = 0;


	/*排序用字段*/
	/**
	 * 商品销量
	 */
	private Long salesVolume;
	/**
	 * 商品7日销量销量
	 */
	private Long salesVolume7;
	/**
	 * 首次上架时间
	 */
	private Date firstOnShelfTime;

	/**
	 * V直播V1.0新增 是否直播中. 1:展示直播中标签，0:不展示直播中标签。默认是0
	 */
	private Integer liveStatus = 0;
	/**
	 *  新增一个字段表示商品是否支持津贴抵扣，1：支持，0：不支持
	 */
	private Integer isAllowance = 0;
	/**
	 *  津贴抵扣金额，默认值为0
	 */
	private BigDecimal allowancePrice = BigDecimal.ZERO;

	/**
	 * sem参数
	 */
	private String semStr;
	/**
	 * 【复购提升专项V2.5-商品场景化推荐】项目新增  0:不展示视频标识。1:展示视频标识，默认是0
	 */
	private String videoStatus = "0";

	/**
	 *视频标识Icon图片地址，取CMS配置
	 */
	private String videoIcon="";

	/**
	 * 造物价
	 */
	private String creationPriceStr;

	/**
	 * 造物价文案
	 */
	private String creationPaperwork;

	/**
	 * 造物角标
	 */
	private String creationShowIcon;


	/**
	 * 造物标识 1：是造物商品   0：不是造物商品
	 */
	private Integer isCreation = 0;


}
