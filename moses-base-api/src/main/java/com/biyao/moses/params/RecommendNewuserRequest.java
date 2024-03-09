package com.biyao.moses.params;

import java.util.List;
import javax.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class RecommendNewuserRequest {

	
	@NotBlank
	private List<String> spuIds;
	
	@NotBlank
	private String topicId;
}
