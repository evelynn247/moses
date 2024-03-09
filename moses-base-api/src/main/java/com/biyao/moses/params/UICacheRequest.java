package com.biyao.moses.params;

import javax.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UICacheRequest {
	
	@NotBlank
	private String pageId;
	
	@NotBlank
	private String topicId;
	
}