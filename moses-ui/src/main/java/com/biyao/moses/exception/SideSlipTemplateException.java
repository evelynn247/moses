package com.biyao.moses.exception;


public class SideSlipTemplateException extends Exception{

	private static final long serialVersionUID = 1L;

	@Override
	public String getMessage() {
		return "横划商品数量小于3";
	}
	
}