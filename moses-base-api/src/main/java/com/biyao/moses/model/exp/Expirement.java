package com.biyao.moses.model.exp;

import java.io.Serializable;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * houkun
 * @Description 具体的实验类 
 * @Date 2018年9月27日
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Expirement implements Serializable{
	
	private static final long serialVersionUID = 1L;
	// 实验编号
	private String expId;
	private String expName;
	//tid  bid  topic
	private Integer expType;
	
	//白名单
	private String whiteList;
	//0,50 左闭右开  [0,50)
	private String flow;
	
	private List<AlgorithmConf> confList;
	
	// 实验描述
	private String expDes;
}