package com.biyao.moses.params.match;

import com.biyao.moses.model.template.Block;
import com.biyao.moses.params.UIBaseBody;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 实验请求参数
 * 
 * @Description
 * @author zyj
 * @Date 2018年9月27日
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MatchRequest implements Serializable {

	private static final long serialVersionUID = 4325392005866293280L;
	// feed流页码
	@Deprecated
	private int feedPageNum;

	@SuppressWarnings("rawtypes")
	private Block block;
	
	// 单纯feed流页的topicId
	private String dataSourceType;
	
	private String sessionId;
	
	private String uuId;
	
	private String siteId;
	
	private String pageId;
	
	private String uid;
	
	private List<String> categoryIds;

	//新手专享页面查询一起拼商品的前台一级类目ID
	private String novicefrontcategoryOneId;

	private List<String> scmIds;

	private String device;

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

	//纬度
	private String lat;
	//经度
	private String lng;
	//页面刷新Id
	private String pvid;
	/**
	 * 订单id或收藏商品id
	 * private List<String> cpIds;
	 * 收藏店铺id
	 * private List<String> csIds;
	 */
	private UIBaseBody uiBaseBody;
	/**
	 * upc用户类型 1:老客|2:新访客|3:老访客
	 */
	private Integer upcUserType;
	/**
	 * 优先展示的商品
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
	 *性别
	 * 男：0，女：1，通用：2
	 */
	private String sex;
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
	 * 个性化推荐设置开关。
	 * true表示开关打开，可以使用用户个性化数据做推荐
	 * false表示开关关闭，不可以使用用户个性化数据做推荐
	 */
	private boolean personalizedRecommendSwitch = true;

	/**
	 * 个性化推荐设置开关。
	 * true表示开关打开，可以使用用户个性化数据做推荐
	 * false表示开关关闭，不可以使用用户个性化数据做推荐
	 */
	private boolean personalizedRecommendActSwitch = true;
}