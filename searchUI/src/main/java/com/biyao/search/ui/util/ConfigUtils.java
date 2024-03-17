package com.biyao.search.ui.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import com.biyao.search.ui.config.DynamicConfig;
import com.by.configs.service.TypeConfigService;

/**
 * 动态配置工具类
 * @author: luozhuo
 * @date: 2017-2-22
 */
@Repository
@Deprecated
public class ConfigUtils {
    
    private static final Log logger = LogFactory.getLog(ConfigUtils.class);
    
	@Autowired
	private TypeConfigService zkConfigService;
	
	private static DynamicConfig dynamicConfig = new DynamicConfig();
	
	@Value("${byconf.path}")
    private String path;
	
	/**
	 * @description: 获取动态配置
	 * @author: luozhuo
	 * @date: 2017-2-22
	 */
	public DynamicConfig getDynamicConfig() {
		try {
			dynamicConfig = zkConfigService.getConfigByClass(path, true, DynamicConfig.class);
		} catch (Exception e) {
			logger.error("ByConfigUtils error", e);
		}
		return dynamicConfig;
	}
}
