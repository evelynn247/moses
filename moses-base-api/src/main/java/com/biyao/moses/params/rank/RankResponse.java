package com.biyao.moses.params.rank;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import lombok.Data;

import com.biyao.moses.dto.trace.TraceDetail;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;

@Data
public class RankResponse implements Serializable {

	private static final long serialVersionUID = 1L;

	//key=dataSource-expId
	private Map<String, List<TotalTemplateInfo>> rankResult;
	
	private List<TraceDetail> traceDetails;
	
}