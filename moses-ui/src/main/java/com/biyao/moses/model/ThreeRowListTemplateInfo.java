package com.biyao.moses.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

import com.biyao.moses.model.template.Label;

/**
 * 三排商品模板
 * 
 * @author monkey
 * @date 2018年8月30日
 */
@Setter
@Getter
public class ThreeRowListTemplateInfo extends ProductTemplateInfo implements Serializable {

	private static final long serialVersionUID = 1L;

	// 三排商品模板处理label label没有爆品和精选 只有一起拼特权金
	public void setLabels(List<Label> labels) {
		List<Label> result = new ArrayList<Label>();
		if (labels == null) {
			super.setLabels(result);
			return;
		}
		for (Label label : labels) {
			if (label.getContent().equals("一起拼") || label.getContent().equals("特权金")||label.getContent().equals("阶梯团")|| label.getContent().equals("新手特权金")) {
				result.add(label);
			}
		}
		super.setLabels(result);
	}
}