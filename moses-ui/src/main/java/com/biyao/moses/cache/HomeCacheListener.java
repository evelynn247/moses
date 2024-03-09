package com.biyao.moses.cache;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.biyao.moses.util.ApplicationContextProvider;

@Component
public class HomeCacheListener implements ApplicationRunner {


	@Override
	public void run(ApplicationArguments args) throws Exception {
		// 需要执行的逻辑代码，当spring容器初始化完成后就会执行该方法。
		HomePageCache bean = ApplicationContextProvider.getBean(
				HomePageCache.class);
		bean.init();
	}

}
