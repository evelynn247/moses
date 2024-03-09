package com.biyao.moses.common.constant;

/**
 * @Description
 * @author zyj
 * @Date 2018年8月29日
 */
public class MosesTypeContans {

	
	// redis存储id
	public static final String MOSES_PAGE_ID = "moses:pid_";
	public static final String MOSES_BLOCKE_ID = "moses:bid_";
	public static final String MOSES_TEMPLATE_ID = "moses:tid_";

	// redis自增序列的key
	public static final String MOSES_PAGE_ID_INCR = "moses:pid_incr";
	public static final String MOSES_BLOCKE_ID_INCR = "moses:bid_incr";
	public static final String MOSES_TEMPLATE_ID_INCR = "moses:tid_incr";

	// 模板id名
	public static final String TEMPLATE_ID = "tid";
	public static final String BLOCKE_ID = "bid";
	public static final String PAGE_ID = "pid";
	public static final String EXP_ID_SUF = "_exp";
	public static final String PAGE_NAME = "pageName";
	public static final String TEMPLATE_NAME = "templateName";
	public static final String RELATION = "-relation";
	public static final String TEMLATE_DATA_SIZE = "dataSize";

	// 是否为feed，实验，动态数据
	public static final String IS_FEED = "feed";
	public static final String DYNAMIC = "dynamic";

	// 模板节点名称
	public static final String BLOCK_NAME = "block";
	public static final String BLOCK_LIST_NAME = "blockList";
	public static final String TEMPLATE_TYPE_NAME = "templateType";
	public static final String TEMPLATE_SOURCE_TYPE = "dataSourceType";

}
