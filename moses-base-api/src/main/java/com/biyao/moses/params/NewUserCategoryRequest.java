package com.biyao.moses.params;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;

@Setter
@Getter
public class NewUserCategoryRequest {

@NotBlank
private String pageId;

@NotBlank
private String topicId;

@NotBlank
private String siteId;
}
