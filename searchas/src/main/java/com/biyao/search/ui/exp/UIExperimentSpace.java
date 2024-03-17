package com.biyao.search.ui.exp;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import com.biyao.search.ui.home.constant.HomeConsts;
import com.biyao.search.ui.home.model.HomeRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.biyao.experiment.ExperimentCondition;
import com.biyao.experiment.ExperimentSpace;
import com.biyao.experiment.ExperimentSpaceBuilder;
import com.biyao.search.ui.model.RequestBlock;
import com.biyao.search.ui.remote.request.UISearchRequest;
import com.biyao.search.ui.util.FileUtil;

@Component
@EnableScheduling
public class UIExperimentSpace {
	private ExperimentSpace experimentSpace;

	/*
	 * private static final String EXP_LAYER_PATH =
	 * "http://conf.nova.biyao.com/search/ui/layer.conf"; private static final
	 * String EXP_PATH = "http://conf.nova.biyao.com/search/ui/exp.conf";
	 */

//	private static final String EXP_LAYER_PATH = Thread.currentThread().getContextClassLoader().getResource("")
//			.getPath() + "/layer.conf";
//	private static final String EXP_PATH = Thread.currentThread().getContextClassLoader().getResource("").getPath()
//			+ "/exp.conf";;

	@Autowired
	UIExperimentSpace uiExperimentSpace;

	private static Logger logger = LoggerFactory.getLogger(UIExperimentSpace.class);

	@PostConstruct
	private void init() {
		refreshUIExperimentSpaceCache();
		
		logger.info("[操作日志]实验系统初始化完成");
	}

	public RequestBlock divert(RequestBlock request) {
		RequestBlock divertResult = experimentSpace.divert(request);

		return divertResult;
	}
	
	public UISearchRequest divert(UISearchRequest request) {
		UISearchRequest uiSearchRequest = experimentSpace.divert(request);

		return uiSearchRequest;
	}

	public HomeRequest divert(HomeRequest request) {
		HomeRequest homeRequest = experimentSpace.divert(request);
		return homeRequest;
	}

	public void refreshExperimentSpace(ExperimentSpace experimentSpace) {
		this.experimentSpace = experimentSpace;
	}

	@Scheduled(cron = "0 0/10 *  * * ? ")
	private void refreshUIExperimentSpaceCache() {
		
		try {
			FileUtil.download(HomeConsts.EXP_CONF_URL, HomeConsts.EXPIREMENT_EXP_PATH);
			FileUtil.download(HomeConsts.LAYER_CONF_URL, HomeConsts.EXPIREMENT_LAYER_PATH);

			Map<String, ExperimentCondition> conditions = new HashMap<String, ExperimentCondition>();
			experimentSpace = new ExperimentSpaceBuilder().setLayer(HomeConsts.EXPIREMENT_LAYER_PATH).setExps(HomeConsts.EXPIREMENT_EXP_PATH)
					.setCondition(conditions).build();

			uiExperimentSpace.refreshExperimentSpace(experimentSpace);
		} catch (Exception e) {
			logger.error("[严重异常]初始化实验异常", e);
		}
	}

}
