package com.biyao.moses.model.exp;

import java.io.Serializable;
import java.util.HashMap;

import com.alibaba.fastjson.JSONObject;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExpRequest implements Serializable{
	
	private static final long serialVersionUID = 1L;

	private String uuid;
	
	private String tid;
	
	private String layerName;
	private String layerId;
	
	private String expId;
	
	private String divison;
}
