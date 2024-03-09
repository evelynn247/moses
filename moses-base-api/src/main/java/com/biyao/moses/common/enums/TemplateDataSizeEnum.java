package com.biyao.moses.common.enums;

/**
 * 模板对应的data长度
 * @Description 
 * @Date 2018年9月27日
 */
public enum TemplateDataSizeEnum {

	TITLE_LINE_DATASIZE(1, 1), // "楼层标题(有副标题显示副标题,没有不显示)"
	SINGLE_LINE_DATASIZE(2, 1), // ,"单排模板-样式1(标题+单张横屏大图)"
	SPECIAL_DATASIZE(3, 1), // 单排模板-样式2（专题）（小程序M站本期不实现）
	DOUBLE_UNFILL_DATASIZE(4, 2), // 双排模板-样式1（左1右1）有标题
	DOUBLED_UP_DATASIZE(5, 2), // 双排模板-样式1（左2右2）
	DOUBLE_RIGHT_DATASIZE(6, 2), // 双排模板-样式1（左1右2）
	DOUBLE_LEFT_DATASIZE(7, 2), // 双排模板-样式1（左2右1）
	TRIPLE_DATASIZE(8, 3), // 三排模板-样式1（中间无空白间隔）
	TRIPLE_GAP_DATASIZE(9, 3), // 三排模板-样式1（中间有空白间隔）
	FOUR_FOLD_DATASIZE(10, 4), // 四排模板
	BLOCK_LINE_DATASIZE(11, 1), // 空白模板
	BORDER_BOTTOM_DATASIZE(12, 1), // 分割线模板 小程序M站是用borderBottom字段来判定
	FEED_SINGLE_DATASIZE(13, 1), // feed流模板单排样式 小程序M站暂无
	FEED_DOUBLE_DATASIZE(14, 1), // feed流模板双排样式 小程序M站暂无
	DOUBLE_FILL_DATASIZE(15, 2), // 双排模板-样式1（左1右1）无标题，小程序M站已实现
	CROSSPIC_SALEPOINT_DATASIZE(16, 1), // 横图模板
	SINGLEROW_LIST_DATASIZE(17, 1), // 单排商品模板
	THREEROW_LIST_DATASIZE(18, 3); // 三排商品模板

	private Integer templateType;
	private Integer dataSize;

	TemplateDataSizeEnum(Integer templateType, Integer dataSize) {
		this.templateType = templateType;
		this.dataSize = dataSize;
	}

	public Integer getTemplateType() {
		return templateType;
	}

	public void setTemplateType(Integer templateType) {
		this.templateType = templateType;
	}

	public Integer getDataSize() {
		return dataSize;
	}

	public void setDataSize(Integer dataSize) {
		this.dataSize = dataSize;
	}

	public static Integer getDataSizeById(Integer templateType) {
		for (TemplateDataSizeEnum templateDataSizeEnum : values()) {
			if (templateType.equals(templateDataSizeEnum.templateType)) {
				return templateDataSizeEnum.dataSize;
			}
		}
		return null;
	}
}
