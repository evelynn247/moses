package com.biyao.moses.model;

import com.biyao.moses.model.template.Label;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 新手专享双排feed样式
 * 
 * @author 杨乐
 * @date 2019年4月19日
 */
@Setter
@Getter
public class TemplateNewuserTemplateInfo extends ProductTemplateInfo implements Serializable {

	private static final long serialVersionUID = 1L;

	// 新手专享模板处理label label只有爆品和精选
	public void setLabels(List<Label> labels) {
		List<Label> result = new ArrayList<Label>();
		if (labels == null) {
			super.setLabels(result);
			return;
		}
		for (Label label : labels) {
			if (label.getContent().equals("爆品") || label.getContent().equals("精选")) {
				result.add(label);
			}
		}
		super.setLabels(result);
	}

	//新手专享价格
	private String novicePrice;

	/**
	 * 直播状态标签
	 * “0”：不展示标签， “1”：展示直播中标签
	 */
	private String liveStatus;

}