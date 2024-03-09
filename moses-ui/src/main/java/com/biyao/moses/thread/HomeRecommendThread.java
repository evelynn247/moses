package com.biyao.moses.thread;

import java.util.concurrent.Callable;

import com.biyao.moses.context.ByUser;
import com.biyao.moses.context.UserContext;
import com.biyao.moses.controller.RecommendUiController;
import com.biyao.moses.model.template.Page;
import com.biyao.moses.model.template.TemplateInfo;
import com.biyao.moses.params.ApiResult;
import com.biyao.moses.params.UIBaseRequest;
import com.biyao.moses.util.ApplicationContextProvider;

/**
 * 请求推荐接口 获取数据
 * @author Administrator
 *
 */
public class HomeRecommendThread implements Callable<ApiResult<Page<TemplateInfo>>> {

	private ByUser byUser;
	private UIBaseRequest uibaseRequest;
	
	public HomeRecommendThread(ByUser byUser,UIBaseRequest uibaseRequest) {
		super();
		this.byUser = byUser;
		this.uibaseRequest = uibaseRequest;
	}


	@Override
	public ApiResult<Page<TemplateInfo>> call() throws Exception {
		RecommendUiController recommendController = ApplicationContextProvider.getBean(RecommendUiController.class);
		
		ApiResult<Page<TemplateInfo>> recommend;
		try {
			UserContext userContext = new UserContext(byUser);
			recommend = recommendController.recommend(uibaseRequest);
		} finally  {
			UserContext.manulClose();
		}
		return recommend;
	}

}