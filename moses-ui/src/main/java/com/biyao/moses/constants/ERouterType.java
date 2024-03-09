package com.biyao.moses.constants;

/**
 * 
 * @Description 
 * @Date 2018年9月27日
 */
public enum ERouterType {

	DALIYPAGE("每日上新", 1), SUPPLIERPAGE("商家店铺", 2),
	TOPICLIST("专题列表", 3), TOPICDETAIL("专题详情", 4),
	SEARCH("搜索中间", 5), RECOMMEND("推荐中间", 6),
	PRODUCTDETAIL("商品详情", 7),CHOUJIANG("抽奖",8),
	ZHENGCAN("正餐",9), XINSHOU("新手专享",10),
	ADVERT("活动广告入口",11),SWIPERPICCONF("配置的轮播图",12),
	PRODUCTGROUP("商品组",13),VIDEO("推荐替换的视频流入口",14),OPEVIDEO("cms配置插入的视频流入口",15);
	private String name;
	private Integer num;

	ERouterType(String name, Integer num) {
		this.name = name;
		this.num = num;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getNum() {
		return this.num;
	}

	public void setNum(Integer num) {
		this.num = num;
	}

	public static boolean containName(String name) {
		ERouterType[] var1 = values();
		int var2 = var1.length;

		for (int var3 = 0; var3 < var2; ++var3) {
			ERouterType platform = var1[var3];
			if (platform.getName().equals(name)) {
				return true;
			}
		}

		return false;
	}

	public static Integer getValueByName(String name) {
		ERouterType[] var1 = values();
		int var2 = var1.length;

		for (int var3 = 0; var3 < var2; ++var3) {
			ERouterType platform = var1[var3];
			if (platform.getName().equals(name)) {
				return platform.getNum();
			}
		}

		return null;
	}

	public static ERouterType getByName(String name) {
		ERouterType[] var1 = values();
		int var2 = var1.length;

		for (int var3 = 0; var3 < var2; ++var3) {
			ERouterType item = var1[var3];
			if (item.name.equals(name)) {
				return item;
			}
		}

		return null;
	}

	public static ERouterType getByNum(Integer num) {
		ERouterType[] enums = values();
		for (ERouterType eRouterType : enums) {
			if (num.equals(eRouterType.num)) {
				return eRouterType;
			}
		}
		return null;
	}
}
