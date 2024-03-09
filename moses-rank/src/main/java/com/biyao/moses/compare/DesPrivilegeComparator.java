package com.biyao.moses.compare;

import com.biyao.moses.model.template.entity.TotalTemplateInfo;

import java.util.Comparator;

/**
 * 先按特权金抵扣金额降序，再按score降序
 */
public class DesPrivilegeComparator implements Comparator<TotalTemplateInfo>{

	@Override
	public int compare(TotalTemplateInfo o1, TotalTemplateInfo o2) {
		double o1PriDeductAmount = Double.valueOf(o1.getPriDeductAmount());
		double o2PriDeductAmount = Double.valueOf(o2.getPriDeductAmount());
		if (o1PriDeductAmount>o2PriDeductAmount) {
			return -1;
		}else if(o1PriDeductAmount<o2PriDeductAmount){
			return 1;
		}else{
			Double	o1Score = o1.getScore();
			Double	o2Score = o2.getScore();
			if (o1Score>o2Score) {
				return -1;
			}else if(o1Score<o2Score){
				return 1;
			}else{
				return 0;
			}
		}
	}
}
