package com.biyao.moses.model.exp;

import java.io.Serializable;
import java.util.List;

import lombok.Data;

@Data
public class ExpirementDomain implements Serializable{
	
	private static final long serialVersionUID = 1L;
	
	// 实验域id
	private String domaimId;
	
	// 实验域描述
	private String domainDes;
	
	// 实验层
	private List<ExpirementLayer> layers;
	
}