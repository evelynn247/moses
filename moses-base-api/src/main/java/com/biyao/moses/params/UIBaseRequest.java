package com.biyao.moses.params;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;

/**
 * UIBaseRequest
 * 
 * @Description
 * @Date 2018年9月27日
 */
@Setter
@Getter
public class UIBaseRequest {

	@NotBlank
	private String pageIndex;

	@NotBlank
	private String pageId;
	/**
	 *  0：首页feeds流，1：个人中心页feeds流，2：购物车页feeds流,3:订单页feeds流,4:首页tab分类页,5:单类目中间页/三级类目中间页
	 *  http://wiki.biyao.com/pages/viewpage.action?pageId=60162054
	 */
	private String pagePositionId;
	
	// 针对相同页面模板，出不同的数据的字段 映射为 dataSourceType dataType expId 
	// dataSourceType dataType 出数据类型 expId 出数据算法
	// topicId 配置：暂时通过svn维护已经使用的topicId，redis中维护topicId映射关系
	
	//=dataSourceType
	//每个数据源下面有不同的数据类型dataType
	private String topicId;
	
	// 一个blockId，可以配置n个实验，但是对于同一个人只能走一个实验
	// 一个实验可以配置多个不同的match，同时有不同的实验号
	//bid---exp1  ---expname=matchAAA  num=1
	//				 expname=matchBBB  num=2
	//		exp2  ---expname=matchAAA  num=2
	//				 expname=matchBBB  num=1 
	
	//使用,拼接 666,234,444
	private String categoryIds;

	private String scmIds;
	// 排序类型 ： 综合 all 0  销量  sale 1  价格 price  2 | 筛选特权金商品 3
	private String sortType;

	// 排序值 : 价格升序 0  降序 1
	private String sortValue;

	/*p平台核心转化提升V1.7 新增*/
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

	/**
	 * 前台类目ID 2019-06-13 zhaiweixi [wuzhenwei 排序算法需要用到]
	 */
	private String frontendCategoryId;

	//新手专享页面查询一起拼商品的前台一级类目ID
	private String novicefrontcategoryOneId;
	
	/**
	 * e.g.1
	 * 张三请求男装排行榜 (实验A，实验B)
	 * 通过uuid分配到实验B(用户浏览match，热销match)
	 * 实验b配置了 用户浏览match-1号数据  热销match-2号数据
	 * 取出两个数据进行排序
	 * 
	 * 李四请求男装排行榜 (实验A，实验B)
	 * 通过uuid分配到实验A(用户浏览match，热销match)
	 * 实验b配置了 用户浏览match-2号数据  热销match-1号数据
	 * 取出两个数据进行排序
	 * 
	 * 取数据的key为  dataSourceType-dataType-matchName-expNum-uuid
	 * 由此可见topicId也需要配置一套实验配置
	 */
	/**
	 * 	定义实验方法
	 *  public Expirement expirement(String uuid,String blockId,String topicId){};
	 *	
	 *	class Expirement {
	 *		Integer expId;
	 *		Integer expType;  //tid  bid  topic
	 *		Map<String,String> matchName_expNum;
	 *		double score;
	 *	}
	 *	
	 */
	
	
	/**
	 * ui-->match 
	 * 
	 * 1、获取模板：通过pageId获取相应页面模板
	 * 	    模板元素 Page  Block Template TemplateInfo 设计见具体类
	 * 		
	 * 2、楼层分页，feed分页，逻辑不变：对当前page进行block分页，页面最后为feed，分页至feed后按照模板分页
	 * 		
	 * 
	 * 
	 * 
	 */
	
	/**
	 * 置顶商品ID（平台核心转化V2.1置顶商品ID）
	 */
	private String priorityProductId;

	/**
	 * 筛选的属性信息（平台核心转化V2.2新增），格式如下：
	 * {
	 * "color": ["红色", "黑色", "白色"],
	 * "size": ["27", "28", "29"]
	 * }
	 */
	private String selectedScreenAttrs;
	/**
	 * 服务端唯一标识
	 */
	private String sid;
	/**
	 * 是否需要展示定制类目
	 * 1: 展示定制类目
	 * 其他：不展示定制类目
	 */
	private String showCustomCate;
	/**
	 * 展示广告入口
	 * "1":表示需要展示x元购活动广告入口，"0"：不需要展示活动广告入口。
	 */
	private String showAdvert;

	/**
	 * 页面版本，用于判断使用哪种模板返回数据
	 * 当值为”1.0”时，则按本次新增的模板返回数据。否则按原来模板返回数据。
	 * 兼容当pageVersion为空时，则按原来模板返回数据。
	 */
	private String pageVersion;

	/**
	 * 插入的活动信息，格式类型为Map,
	 * 格式为：List<AdvertParam>的json字符串
	 */
	private String advertInfo;

	/**
	 * 当前页面的前端页面ID
	 */
	private String frontPageId;

	/**
	 * 每页大小
	 */
	private int pageSize;

	/**
	 * 配置的首页轮播图信息
	 */
	private String swiperPicConfInfo;
	// 商品是否被替换成商品组
	private String isShowProductGroup;
	/**
	 * 商品是否被替换成视频 0=false 1=true
	 */
	private String isConvert2Video;
	/**
	 * 商品替换为视频最小间隔
	 */
	private Integer videoInterval;
	/**
	 * 渠道   必要商城 1  必要分销 2 鸿源分销 3
	 * 不传默认为必要主站
	 */
	private Integer channelType = 1;
}