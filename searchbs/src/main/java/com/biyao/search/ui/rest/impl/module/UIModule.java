package com.biyao.search.ui.rest.impl.module;

import com.biyao.search.common.model.Status;
import com.biyao.search.ui.model.RequestBlock;

/**
 * UI的模块定义
 * @author guochong
 * @date 2017-02-28
 */
public interface  UIModule{
	
	/**
	 * 初始化操作
	 * 程序启动时执行一次
	 */
	public void init();
	
	/**
	 * 清理操作
	 * 启动关闭时执行一次
	 */
	public void destroy();
	
	/**
	 * 处理请求
	 * 每个请求都会运行一次
	 * @param request 请求体，是一个大的数据对象
	 * @return 处理结果状态
	 */
	public Status run( RequestBlock request );
}
