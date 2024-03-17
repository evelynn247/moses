package com.biyao.search.ui.home.constant;

public enum HomeTemplateEnum {
	
	title_line(1,"titleline"),//"楼层标题(有副标题显示副标题,没有不显示)"
	single_line(2,"singleline"),//,"单排模板-样式1(标题+单张横屏大图)"
	special(3,"special"),//单排模板-样式2（专题）（小程序M站本期不实现）
	double_unfill(4,"doubleunfill"),//双排模板-样式1（左1右1）有标题
	doubled_up(5,"doubledup"),//双排模板-样式1（左2右2）
	double_right(6,"doubleright"),//双排模板-样式1（左1右2）
	double_left(7,"doubleleft"),//双排模板-样式1（左2右1）
	triple(8,"triple"),//三排模板-样式1（中间无空白间隔）
	triple_gap(9,"triplegap"),//三排模板-样式1（中间有空白间隔）
	four_fold(10,"fourfold"),//四排模板
	block_line(11,"blockline"),//空白模板
	border_bottom(12,"borderBottom"),//分割线模板 小程序M站是用borderBottom字段来判定
	feed_single(13,"feedSingle"),//feed流模板单排样式   小程序M站暂无
	feed_double(14,"feedDouble"),//feed流模板双排样式   小程序M站暂无
	double_fill(15,"doublefill");//双排模板-样式1（左1右1）无标题，小程序M站已实现
	
	private Integer templateId;
	private String templateType;
	HomeTemplateEnum(Integer templateId, String templateType) {
		this.templateId = templateId;
		this.templateType = templateType;
	}
	public Integer getTemplateId() {
		return templateId;
	}
	public String getTemplateType() {
		return templateType;
	}
	
	public static Integer getTemplateIdByValue(String templateType){
		for (HomeTemplateEnum homeTemplateEnum : values()) {
			
			if (templateType.equals(homeTemplateEnum.templateType)) {
				return homeTemplateEnum.templateId;
			}
		}
		return null;
	}
	
}
