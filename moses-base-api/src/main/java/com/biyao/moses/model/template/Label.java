package com.biyao.moses.model.template;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;

/**
 * Label
 * 
 * @Description
 * @Date 2018年9月27日
 */
@Setter
@Getter
public class Label implements Serializable {

	private static final long serialVersionUID = 1L;

	private String content;

	private String color;

	private String textColor;

	private String roundColor;

}
