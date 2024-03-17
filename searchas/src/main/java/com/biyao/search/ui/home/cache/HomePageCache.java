package com.biyao.search.ui.home.cache;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.biyao.search.common.enums.PlatformEnum;
import com.biyao.search.ui.constant.CommonConstant;
import com.biyao.search.ui.constant.RedisKeyConsts;
import com.biyao.search.ui.home.dubbo.impl.HomeDubboServiceImpl;
import com.biyao.search.ui.home.model.HomeRequest;
import com.biyao.search.ui.home.model.HomeTemplate;
import com.biyao.search.ui.home.model.app.AppHomeRequest;
import com.biyao.search.ui.home.model.app.AppHomeTemplate;
import com.biyao.search.ui.home.model.app.Feed;
import com.biyao.search.ui.home.model.app.FeedPageData;
import com.biyao.search.ui.home.strategy.impl.FeedBuilder;
import com.biyao.search.ui.util.IdCalculateUtil;
import com.biyao.search.ui.util.RedisUtil;

/**
 * 首页缓存
 *
 * @author yangle
 * @version 1.0 2018/6/4
 */
@Component
@EnableScheduling
public class HomePageCache implements InitializingBean, DisposableBean {

    private Logger logger = LoggerFactory.getLogger(HomePageCache.class);

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    private List<List<HomeTemplate>> homeTemplateCache = new ArrayList<List<HomeTemplate>>();

    private List<List<HomeTemplate>> homeTemplateCacheM = new ArrayList<List<HomeTemplate>>();

    private List<List<HomeTemplate>> homeTemplateCacheMini = new ArrayList<List<HomeTemplate>>();

    private FeedPageData feedAppCache = new FeedPageData();

    private FeedPageData feedMCache = new FeedPageData();

    private FeedPageData feedMiniCache = new FeedPageData();

    @Autowired
    private HomeDubboServiceImpl homeDubboService;
    @Autowired
    private FeedBuilder feedBuilder;
    @Autowired
    private RedisUtil redisUtil;

    //TODO 该逻辑需要梳理 
    private void loadData() {
        logger.info("首页缓存开始加载");
        AppHomeRequest request = new AppHomeRequest();
        request.setPageIndex(1);
        request.setPageSize(120);
        request.setUuid("7181011141323f228e8fc0efd4e80000000");
        request.setPvid("7181011-1011141323665-55a481590");
        request.setSiteId(PlatformEnum.IOS.getNum() + "");
        request.setAvn(CommonConstant.IOS_HOMECACHESEARCH_VERSION + "");
        String sid = IdCalculateUtil.createBlockId();
        StringBuffer showContent = new StringBuffer();
        try {
            List<List<HomeTemplate>> homeTemplateCacheTemp = homeDubboService.buildMSiteData(request, sid, showContent);
            if (homeTemplateCacheTemp != null || homeTemplateCacheTemp.size() > 0) {
                homeTemplateCache = homeTemplateCacheTemp;
            }
            request.setSiteId(PlatformEnum.M.getNum() + "");
            List<List<HomeTemplate>> homeTemplateCacheTempM = homeDubboService.buildMSiteData(request, sid, showContent);
            if (homeTemplateCacheTemp != null || homeTemplateCacheTemp.size() > 0) {
                homeTemplateCacheM = homeTemplateCacheTempM;
            }
            request.setSiteId(PlatformEnum.MINI.getNum() + "");
            List<List<HomeTemplate>> homeTemplateCacheTempMini = homeDubboService.buildMSiteData(request, sid, showContent);
            if (homeTemplateCacheTemp != null || homeTemplateCacheTemp.size() > 0) {
                homeTemplateCacheMini = homeTemplateCacheTempMini;
            }

        } catch (Exception e) {
            logger.error("homeTemplateCache get error, error message is {}", e);
        }
        try {
            request.setSiteId(PlatformEnum.IOS.getNum() + "");
            FeedPageData feedCacheAppTemp = feedBuilder.buildFeed(request);
            feedCacheAppTemp.setPageIndex(request.getPageIndex() + "");
            if (feedCacheAppTemp != null || feedCacheAppTemp.getFeed() != null) {
                feedAppCache = feedCacheAppTemp;
            }
            request.setSiteId(PlatformEnum.M.getNum() + "");
            FeedPageData feedCacheMTemp = feedBuilder.buildFeed(request);
            feedCacheMTemp.setPageIndex(request.getPageIndex() + "");
            if (feedCacheMTemp != null || feedCacheMTemp.getFeed() != null) {
                feedMCache = feedCacheMTemp;
            }
            request.setSiteId(PlatformEnum.MINI.getNum() + "");
            FeedPageData feedCacheMiniTemp = feedBuilder.buildFeed(request);
            feedCacheMiniTemp.setPageIndex(request.getPageIndex() + "");
            if (feedCacheMiniTemp != null || feedCacheMiniTemp.getFeed() != null) {
                feedMiniCache = feedCacheMiniTemp;
            }
        } catch (Exception e) {
            logger.error("[严重异常]feedCache get error, error message is {}", e);
        }

    }

    public List<List<HomeTemplate>> getHomeTemplateCache(HomeRequest request) {
        if (request.getSiteId().equals(PlatformEnum.M.getNum() + "")) {
            return homeTemplateCacheM;
        } else if (request.getSiteId().equals(PlatformEnum.MINI.getNum() + "")) {
            return homeTemplateCacheMini;
        } else {
            return homeTemplateCache;
        }
    }

    /**
     * 获取首页缓存
     * @param request
     * @return
     */
    public FeedPageData getHomeFeedCache(AppHomeRequest request) {
        FeedPageData retFeedPageData = new FeedPageData();
        int pageIndex = request.getPageIndex();
        int pageSize = request.getPageSize();
        List<AppHomeTemplate> goodsList = new ArrayList<AppHomeTemplate>();
        if (PlatformEnum.M.getNum().toString().equals(request.getSiteId())) {
            if (feedMCache.getFeed() != null) {
                goodsList = feedMCache.getFeed().getGoodsList();
            }
        } else if (PlatformEnum.MINI.getNum().toString().equals(request.getSiteId())) {
            if (feedMiniCache.getFeed() != null) {
                goodsList = feedMiniCache.getFeed().getGoodsList();
            }
        } else {
            if (feedAppCache.getFeed() != null) {
                goodsList = feedAppCache.getFeed().getGoodsList();
            }
        }
        int pageCount = (goodsList.size() / pageSize) + 1;
        List<AppHomeTemplate> subGoodsList = new ArrayList<AppHomeTemplate>();
        if (pageIndex == 1) {
            subGoodsList = goodsList.stream().limit(pageSize).parallel().collect(Collectors.toList());
        } else {
            subGoodsList = goodsList.stream().skip((pageIndex - 1) * pageSize).limit(pageSize).parallel().collect(Collectors.toList());
        }

        Feed feed = new Feed();
        feed.setGoodsList(subGoodsList);
        feed.setTitle("猜你喜欢");
        feed.setTitleColor("#4A4A4A");

        retFeedPageData.setFeed(feed);
        retFeedPageData.setPageIndex(pageIndex + "");
        retFeedPageData.setPageCount(pageCount + "");
        return retFeedPageData;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        setFlagCache();
//        Calendar now = Calendar.getInstance();
//        now.add(Calendar.DATE, 1);
//        now.set(Calendar.HOUR_OF_DAY, 6);
//        now.set(Calendar.MINUTE, 0);
//        now.set(Calendar.SECOND, 0);
//        long tomorrowFirstSecond = now.getTimeInMillis();
//
//        executor.scheduleAtFixedRate(new Runnable() {
//            @Override
//            public void run() {
//                loadData();
//            }
//        }, (tomorrowFirstSecond - System.currentTimeMillis()) / 1000, 300, TimeUnit.SECONDS);
        loadData();
    }


    /**
     * 每10分钟刷新一次
     */
    @Scheduled(cron = "0 0/5 * * * ?")
    private void refreshExpConf() {
        logger.info("定时任务开始");
        loadData();
    }

    private void setFlagCache() {
        redisUtil.setString(RedisKeyConsts.FLOORKEY, "1", -1);
        redisUtil.setString(RedisKeyConsts.NOVAKEY, "1", -1);
    }

    @Override
    public void destroy() throws Exception {
        executor.shutdown();
    }


}
