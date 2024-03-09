package com.biyao.moses.model.match;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Setter
@Getter
public class ProductImage {

	private Long productId;
	private String image;
	private String webpImage;
	private Integer routeType;
	private Map<String, String> routeParams;
}
