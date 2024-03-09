package com.biyao.moses.bean;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
/**
* @Description kafka配置
* @date 2019年7月11日下午5:40:31
* @version V1.0 
* @author 邹立强 (zouliqiang@idstaff.com)
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class KafkaConfigure implements java.io.Serializable {

	private static final long serialVersionUID = 2008006808363305213L;

	private String bootstrapServers;

	private String acks;

	private Integer retries;

	private Integer batchSize;

	private Integer lingerMs;

	private Long bufferMemory;

	private String compressionType;

	private String keySerializer;

	private String valueSerializer;
	
}