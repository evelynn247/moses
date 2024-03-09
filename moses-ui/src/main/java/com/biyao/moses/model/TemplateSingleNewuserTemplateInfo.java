package com.biyao.moses.model;

import com.biyao.moses.model.template.Label;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @Description 新手专享单排feed样式
 * @date 2019年5月6日下午2:37:33
 * @version V1.0
 * @author 邹立强 (zouliqiang@idstaff.com)
 */
@Setter
@Getter
public class TemplateSingleNewuserTemplateInfo extends ProductTemplateInfo implements Serializable {

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