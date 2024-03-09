package com.biyao.moses.common.enums;

/**
 * 模板类型
 * 
 * @Description
 * @Date 2018年9月27日
 */
public enum TemplateTypeEnum {

	TITLE_LINE(1, "titleline", 1, false), // "楼层标题(有副标题显示副标题,没有不显示)"
	SINGLE_LINE(2, "singleline", 1, false), // ,"单排模板-样式1(标题+单张横屏大图)"
	SPECIAL(3, "special", 1, false), // 单排模板-样式2（专题）（小程序M站本期不实现）
	DOUBLE_UNFILL(4, "doubleunfill", 2, false), // 双排模板-样式1（左1右1）有标题
	DOUBLED_UP(5, "doubledup", 2, false), // 双排模板-样式1（左2右2）
	DOUBLE_RIGHT(6, "doubleright", 2, false), // 双排模板-样式1（左1右2）
	DOUBLE_LEFT(7, "doubleleft", 2, false), // 双排模板-样式1（左2右1）
	TRIPLE(8, "triple", 3, false), // 三排模板-样式1（中间无空白间隔）,细线
	TRIPLE_GAP(9, "triplegap", 3, false), // 三排模板-样式1（中间有空白间隔），粗线
	FOUR_FOLD(10, "fourfold", 4, false), // 四排模板
	BLOCK_LINE(11, "blockline", 1, false), // 空白模板
	BORDER_BOTTOM(12, "separateLine", 1, false), // 分割线模板 小程序M站是用borderBottom字段来判定
	FEED_SINGLE(13, "singleRowListRecommend", 1, false), // feed流模板单排样式 小程序M站暂无
	FEED_DOUBLE(14, "doubleRowList", 2, false), // feed流模板双排样式 小程序M站暂无
	DOUBLE_FILL(15, "doublefill", 2, false), // 双排模板-样式1（左1右1）无标题，小程序M站已实现
	CROSSPIC_SALEPOINT(16, "crossPicSalePoint", 1, false), // 横图模板
	SINGLEROW_LIST(17, "singleRowList", 1, false), // 单排商品模板
	THREEROW_LIST(18, "threeRowList", 3, false), // 三排商品模板
	SWIPER_PICTURE(19, "swiperPicture", -1, true), // 轮播图模板
	SWITCH_TAB(20, "switchTab", -1, true), // 顶部导航模板
	SIDE_SLIP(21, "sideslip", 15, true), // 横滑模板
	NARROW_SINGLE_LINE(22, "narrowSingleline", 1, false),// ,"每日上新窄图单排模板-样式1(标题+单张窄横屏大图)"
	SINGLE_GROUP(23, "singleGroup", 1, false), // 一起拼首页商品模板
	TEMPLATE_NEWUSER(24, "templateNewuser", 2, false),// feed流模板双排样式 小程序M站暂无
	PRI_DOUBLE_ROWLISST (26, "priDoubleRowList", 2, false),//特权金双排商品样式
	TEMPLATE_SINGLE_NEWUSER(27, "templateSingleNewuser", 1, false),// 新手专享feed流模板单排样式
	FEED_SINGLE_COMMONPRIVILEGE(28, "singleCommonPrivilege", 1, false),// 通用特权金feed流模板单排样式
	PRI_DOUBLE_ROWLISST_FOR_ADVERT (31, "doubleRowListForAdvert", 2, false),//普通商品feed流模板双排+支持活动广告样式
	TEMPLATE_NEWUSER_FOR_ADVERT(32, "templateNewuserForAdvert", 2, false);// 新手专享feed流模板双排+支持活动广告样式;



	private Integer templateId;
	private String templateType;

	// 2018-11-02
	// 每个模板需要填充的数据量
	private Integer dataSize;
	// 当前模板是否为动态数据
	private Boolean isDynamicDataSize;

	TemplateTypeEnum(Integer templateId, String templateType) {
		this.templateId = templateId;
		this.templateType = templateType;
	}

	TemplateTypeEnum(Integer templateId, String templateType, Integer dataSize, Boolean isDynamicDataSize) {
		this.templateId = templateId;
		this.templateType = templateType;
		this.dataSize = dataSize;
		this.isDynamicDataSize = isDynamicDataSize;
	}

	public Integer getTemplateId() {
		return templateId;
	}

	public String getTemplateType() {
		return templateType;
	}

	public Integer getDataSize() {
		return dataSize;
	}

	public Boolean getIsDynamicDataSize() {
		return isDynamicDataSize;
	}

	public static Integer getTemplateIdByValue(String templateType) {
		for (TemplateTypeEnum templateTypeEnum : values()) {

			if (templateType.equals(templateTypeEnum.templateType)) {
				return templateTypeEnum.templateId;
			}
		}
		return null;
	}

	public static String getTemplateNameById(Integer templateId) {
		for (TemplateTypeEnum templateTypeEnum : values()) {
			if (templateId.equals(templateTypeEnum.getTemplateId())) {
				return templateTypeEnum.getTemplateType();
			}
		}
		return null;
	}

	@Deprecated
	public static int getTemplateDataNum(int templateId) {
		int num = 0;
		if (templateId == FEED_SINGLE.getTemplateId()) {
			num = 1;
		} else if (templateId == FEED_DOUBLE.getTemplateId()) {
			num = 2;
		} else if (templateId == CROSSPIC_SALEPOINT.getTemplateId()) {
			num = 1;
		} else if (templateId == SINGLEROW_LIST.getTemplateId()) {
			num = 1;
		} else if (templateId == THREEROW_LIST.getTemplateId()) {
			num = 3;
		}
		return num;
	}

	// 横划模板为动态数据量模板
	@Deprecated
	public static boolean isDynamicDataSizeTemplate(String templateType) {

		if (TemplateTypeEnum.SIDE_SLIP.getTemplateType().equals(templateType)) {
			return true;
		} else {
			return false;
		}

	}

	public static TemplateTypeEnum getTemplateTypeEnumByValue(String templateName) {
		for (TemplateTypeEnum templateTypeEnum : values()) {

			if (templateName.equals(templateTypeEnum.templateType)) {
				return templateTypeEnum;
			}
		}
		return null;
	}

	/**
	 * 是否是双排feed流（左1右1）模板
	 * @param templateId
	 * @return
	 */
	public static boolean isDoubleRowList(Integer templateId){
		if(templateId == null){
			return false;
		}

		if(templateId.equals(TemplateTypeEnum.FEED_DOUBLE.getTemplateId())
		    || templateId.equals(TemplateTypeEnum.DOUBLE_FILL.getTemplateId())
		    || templateId.equals(TemplateTypeEnum.TEMPLATE_NEWUSER.getTemplateId())
			|| templateId.equals(TemplateTypeEnum.PRI_DOUBLE_ROWLISST.getTemplateId())
			|| templateId.equals(TemplateTypeEnum.PRI_DOUBLE_ROWLISST_FOR_ADVERT.getTemplateId())
			|| templateId.equals(TemplateTypeEnum.TEMPLATE_NEWUSER_FOR_ADVERT.getTemplateId())){
			return true;
		}
		return false;
	}

	//判断是否为空白和分割线模板
	public static boolean expTemplateName(String templateName) {
		Integer templateType = getTemplateIdByValue(templateName);
		if (templateType == null) {
			return false;
		}else if(templateType==11 || templateType==12 ){
			return true;
		}
		return false;
	}
	
}
