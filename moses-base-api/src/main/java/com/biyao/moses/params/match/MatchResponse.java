package com.biyao.moses.params.match;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.biyao.moses.dto.trace.TraceDetail;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;

/**
 * 
 * @Description 
 * @author zyj
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MatchResponse implements Serializable {
	/**
	* 
	*/
	private static final long serialVersionUID = -5301614608687671675L;

	// key=dataSourceType-dataType-expId
	private Map<String, List<TotalTemplateInfo>> resultMap;
	
	//埋点参数
	private List<TraceDetail> traceDetail;
}
