package com.biyao.moses.util;

import java.util.HashSet;

import com.biyao.moses.dto.trace.TraceDetail;

/**
 * 埋点日志工具类
 * 
 */
public class TraceLogUtil {
	/**
	 * 初始化针对于当前请求的每个实验的追踪aid
	 * @return
	 */
	public static TraceDetail initAid() {
//		String aid = IdCalculateUtil.createUniqueId();
		TraceDetail traceDetail = new TraceDetail();
		HashSet<String> keys = new HashSet<String>();
		traceDetail.setKeys(keys);
		
		return traceDetail;
	}

}