package com.biyao.moses.model.exp;

import com.biyao.moses.params.UIBaseBody;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MatchDataSourceTypeConf extends AlgorithmConf {

	private static final long serialVersionUID = 582346341737453102L;

	// 此类型由需求方和算法提出，例如新增低价排行数据源。将此数据源维护到svn
	private String dataSourceType;

	private String expId;
	// feed流页码
	@Deprecated
	private int feedPageNum = 0;
	// true 执行的兜底数据，编号0000；false 未执行兜底数据
	private boolean defalutData = false;
	
	private String uid;

	private String device;

	private List<String> categoryIds;

	//新手专享页面查询一起拼商品的前台一级类目ID
	private String novicefrontcategoryOneId;
	
	private List<String> scmIds;

	/*p平台核心转化提升V1.7 新增*/
	// 排序类型 ： 综合 all 0  销量  sale 1  价格 price  2 | 筛选特权金商品 3
	private String sortType;
	//	用户类型 1 新客；2老客;注：当sortType=3时，必传
	private String userType;
	//特权金券类型 1 新客特权金；2 通用特权金;注：当sortType=3时，必传
	@Deprecated
	private String priCouponType;
	//最大特权金面额（单位元）传入参数值必须为数字，且必须大于0;注：当sortType=3时，必传
	@Deprecated
	private String priCouponAmount;

	/**
	 * 特权金优惠列表
	 * 格式：{类型}:{金额}
	 * 多个以英文逗号分割
	 * 特权金2.8.1后，废弃原priCouponType和priCouponAmount两个字段
	 */
	private String priCouponAmountList;
	
	private String lat;
	private String lng;
	
	private UIBaseBody uiBaseBody;
	
	private String siteId;
	/**
	 * upc 用户类型
	 */
	private Integer upcUserType;
	/**
	 * 优先展示商品
	 */
	private String priorityProductId;
	/**
	 * 筛选的属性信息（平台核心转化V2.2新增），格式如下：
	 * {
	 * "color": ["红色", "黑色", "白色"],
	 * "size": ["27", "28", "29"]
	 * }
	 */
	private Map<String, List<String>> selectedScreenAttrs;
	/**
	 * 前台类目ID
	 */
	private String frontendCategoryId;

	/**
	 * 是否展示定制商品
	 * true: 展示定制商品
	 * false：不展示定制商品
	 */
	private Boolean isShowCustomProduct;

	/**
	 * 用户性别
	 */
	private String sex;

	/**
	 * 个性化推荐设置开关。
	 * true表示开关打开，可以使用用户个性化数据做推荐
	 * false表示开关关闭，不可以使用用户个性化数据做推荐
	 */
	private boolean personalizedRecommendSwitch = true;

	/**
	 * 个性化推荐活动设置开关。
	 * true表示开关打开，可以使用用户个性化数据做推荐
	 * false表示开关关闭，不可以使用用户个性化数据做推荐
	 */
	private boolean personalizedRecommendActSwitch = true;

}
