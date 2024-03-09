package com.biyao.moses.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.biyao.moses.model.template.Label;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class SingleGroupTemplateInfo extends ProductTemplateInfo implements Serializable {

	private static final long serialVersionUID = 1L;

	// 首页一起拼模板 不展示特权金标签
	public void setLabels(List<Label> labels) {
		List<Label> result = new ArrayList<Label>();
		if (labels == null) {
			super.setLabels(result);
			return;
		}
		for (Label label : labels) {
			if (!label.getContent().equals("特权金") || label.getContent().equals("阶梯团") || !label.getContent().equals("新手特权金") ) {
				result.add(label);
			}
		}
		super.setLabels(result);
	}

}