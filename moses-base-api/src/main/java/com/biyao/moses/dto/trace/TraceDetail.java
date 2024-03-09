package com.biyao.moses.dto.trace;

import java.util.Set;

import lombok.Data;

@Data
public class TraceDetail {
	
	//实验id
	private String expId;
	//获取数据的key
	private Set<String> keys;
	
	private String pids;

	private String scms;
}
