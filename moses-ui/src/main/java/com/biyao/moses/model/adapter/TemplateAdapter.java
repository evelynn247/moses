package com.biyao.moses.model.adapter;

import java.util.List;

import com.biyao.moses.context.ByUser;
import com.biyao.moses.model.template.Template;
import com.biyao.moses.model.template.TemplateInfo;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;

/**
 * 
 * @Description
 * @Date 2018年9月27日
 */
public interface TemplateAdapter {

	/**
	 * @author monkey
	 * 
	 * @param pageIndex 全局计数
	 * 
	 * @param oriTemplate
	 *            模板结构
	 * @param temData
	 *            模板数据
	 * @param feedTemPage
	 *            feed流页码
	 * @param stp
	 *            追踪参数
	 * @return Template<TemplateInfo> 填充好具体数据的模板
	 */
	Template<TemplateInfo> adapte(Integer pageIndex, Template<TotalTemplateInfo> oriTemplate, List<TotalTemplateInfo> temData,
			Integer feedTemPage, String stp,ByUser user) throws Exception;

	/**
	 * 
	 * @author monkey
	 * @return 填充当前模板需要的数据数量, -1表示为全量
	 * @throws Exception
	 */
	Integer getCurTemplateDataNum(int actualSize) throws Exception;

	/**
	 * @author monkey
	 * @param oriTemplate
	 *            模板结构
	 * @param temData
	 *            模板数据
	 * @param stp
	 *            追踪参数
	 * @return Template<TemplateInfo> 填充好具体数据的模板
	 */
	Template<TemplateInfo> adapte(Template<TotalTemplateInfo> oriTemplate, List<TotalTemplateInfo> temData, String stp,ByUser user)
			throws Exception;

}
