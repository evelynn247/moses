package com.biyao.moses.compare;

import java.util.Comparator;

import com.biyao.moses.model.template.entity.TotalTemplateInfo;

/**
 * 价格降序，高价在上
 * @author Administrator
 *
 */
public class DesComparator implements Comparator<TotalTemplateInfo>{

	@Override
	public int compare(TotalTemplateInfo o1, TotalTemplateInfo o2) {
		
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