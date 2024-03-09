package com.biyao.moses.params;

import java.io.Serializable;

import javax.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

/**
 * 
 * @Description 
 */
@Setter
@Getter
public class ExpirementParam implements Serializable {

	private static final long serialVersionUID = 1L;

	// tid 或者 topicId bid
	@NotBlank
	private String id;

}