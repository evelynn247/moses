package com.biyao.moses.params;

import javax.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class RecommendPageRequest {

	//数据定位来源，s1,个人中心推荐位 | pricategory 特权金类目页
	@NotBlank
	private String source;
}
