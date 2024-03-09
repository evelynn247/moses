package com.biyao.moses.model.exp;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AlgorithmConf implements Serializable{

	private static final long serialVersionUID = 1L;
	
	//修改数据使用
	private String id;
	// matchname or rankname
	private String name;
	// 权重 --商品分*权重 作为排序的
	private String weight;
	// 匹配数据  dateSource+dateType + expNum 
	private String expNum;

	// 算法描述
	private String algorithmDes;
}
