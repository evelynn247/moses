package com.biyao.search.ui.cache.guava;

import com.biyao.search.ui.constant.CommonConstant;
import com.biyao.search.ui.home.cache.CmsDataConfigCache;
import com.biyao.search.ui.home.cache.HomeFloorConfigCache;
import com.biyao.search.ui.home.constant.HomeConsts;
import com.biyao.search.ui.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Timer;
import java.util.TimerTask;

@Component
public class GuavaRefreshTask {
    @Autowired
    HomeFloorConfigCache homeFloorConfigCache;
    @Autowired
    CmsDataConfigCache cmsDataConfigCache;

    private Logger logger = LoggerFactory.getLogger(getClass());

    @PostConstruct
    public void init(){
    	try {
    		refreshHomeFloorConfigCache();
            setRefreshTimer();
		} catch (Exception e) {
			 logger.error("[严重异常]首页楼层配置文件缓存初始化时，发生异常", e);
		}
    }


    private void setRefreshTimer(){
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                refreshHomeFloorConfigCache();
                logger.info("**********[操作日志]首页楼层配置文件缓存刷新成功*********");

            }
        }, CommonConstant.GUAVA_REFRESH_DELAY, CommonConstant.GUAVA_REFRESH_PERIOD);
        logger.info("**********[操作日志]首页楼层配置文件缓存自动刷新任务启动*********");
    }

    private void refreshHomeFloorConfigCache(){
        FileUtil.download(HomeConsts.HOME_FLOOR_CONF_URL, HomeConsts.HOME_FLOOR_CONF_PATH);
//        FileUtil.download(HomeConsts.HOME_FLOOR_CONF_URL2, HomeConsts.HOME_FLOOR_CONF_PATH2);
//        // 下载推荐楼层配置
//        FileUtil.download(HomeConsts.HOME_FLOOR_CONF_REC_URL3, HomeConsts.HOME_FLOOR_CONF_REC_PATH3);
//        FileUtil.download(HomeConsts.HOME_FLOOR_CONF_REC_URL4, HomeConsts.HOME_FLOOR_CONF_PERSONAL_PATH4);
        homeFloorConfigCache.refreshValue();
        cmsDataConfigCache.refreshValue();
        logger.info("**********[操作日志]首页楼层配置文件缓存初始化成功*********");
    }
}