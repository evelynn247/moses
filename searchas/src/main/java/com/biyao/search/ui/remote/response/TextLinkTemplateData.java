package com.biyao.search.ui.remote.response;

import java.util.ArrayList;
import java.util.List;

public class TextLinkTemplateData extends TemplateData{
	/**
	 * 搜索页顶部提示文本（目前是未完全匹配时使用）
	 * 外层的list代表多行文本
	 * 内层的list代表一行文本里的多个文本块
	 */
	private List<SearchTitle> title = new ArrayList<>();
	
	/**
	 * 按钮列表
	 */
	private List<LinkButton> buttons = new ArrayList<>();


	public List<SearchTitle> getTitle() {
		return title;
	}

	public void setTitle(List<SearchTitle> title) {
		this.title = title;
	}

	public List<LinkButton> getButtons() {
		return buttons;
	}

	public void setButtons(List<LinkButton> buttons) {
		this.buttons = buttons;
	}
	
	
}
