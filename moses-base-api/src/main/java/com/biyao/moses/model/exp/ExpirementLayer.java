package com.biyao.moses.model.exp;

import java.io.Serializable;
import java.util.List;

import lombok.Data;

@Data
public class ExpirementLayer implements Serializable{

	private static final long serialVersionUID = 1L;
	
	private String layerId;

	//match  rank
	private String layerName;
	
	//分流算法
	private String divison;
	
	// 格式 20180101
	private String startDate;
	// 格式 20180101
	private String endDate;
	
	private List<Expirement> expirements;
	
}