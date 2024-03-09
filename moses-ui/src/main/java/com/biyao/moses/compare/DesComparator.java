package com.biyao.moses.compare;

import java.util.Comparator;

import com.biyao.moses.model.template.FrontendCategory;

/**
 * 
 * @Description 降序排序
 * @author Alpaca
 * @Time 2019年5月23日 下午5:15:19
 */
public class DesComparator implements Comparator<FrontendCategory>{

	@Override
	public int compare(FrontendCategory o1, FrontendCategory o2) {
		
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