package com.biyao.moses.controller;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 配置视图页面
 * @Description 
 * @Date 2018年9月25日
 */
//@Controller
public class MosesConfViewController {

	@GetMapping("/testjsp")
	public String testjsp(Model model) {

		model.addAttribute("requestKey", "hello jsp");

		return "test";
	}

}
