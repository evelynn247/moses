package com.biyao.moses.controller;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.biyao.moses.cache.RedisCache;
import com.biyao.moses.common.constant.ErrorCode;
import com.biyao.moses.common.constant.RedisKeyConstant;
import com.biyao.moses.common.constant.UPCUserTypeConstants;
import com.biyao.moses.common.enums.MosesConfTypeEnum;
import com.biyao.moses.common.enums.RedisKeyTypeEnum;
import com.biyao.moses.constants.CommonConstants;
import com.biyao.moses.context.ByUser;
import com.biyao.moses.context.UserContext;
import com.biyao.moses.dto.trace.TraceDetail;
import com.biyao.moses.exp.ExpirementSpace;
import com.biyao.moses.exp.util.MosesConfUtil;
import com.biyao.moses.model.exp.ExpRequest;
import com.biyao.moses.model.exp.Expirement;
import com.biyao.moses.model.template.Block;
import com.biyao.moses.model.template.Page;
import com.biyao.moses.model.template.TemplateInfo;
import com.biyao.moses.model.template.entity.RecommendPage;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;
import com.biyao.moses.params.ApiResult;
import com.biyao.moses.params.UIBaseBody;
import com.biyao.moses.params.UITestRequest;
import com.biyao.moses.rpc.UcRpcService;
import com.biyao.moses.service.imp.AdvertInfoService;
import com.biyao.moses.service.imp.HomePageCacheServiceImpl;
import com.biyao.moses.service.imp.MatchAndRankAnsyService;
import com.biyao.moses.service.imp.PageConfigService;
import com.biyao.moses.util.*;
import com.biyao.upc.dubbo.client.business.toc.IBusinessTocDubboService;
import com.biyao.upc.dubbo.dto.VisitorInfoDTO;
import com.biyao.upc.dubbo.param.business.VisitorInfoParam;
import com.by.profiler.annotation.BProfiler;
import com.by.profiler.annotation.MonitorType;
import com.uc.domain.bean.User;
import com.uc.domain.constant.UserFieldConstants;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@RestController
@Api("测试接口")
@RequestMapping(value = "/test")
@Slf4j
public class RecommendTestController {

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private MatchAndRankAnsyService matchAndRankAnsyService;

    @Autowired
    private HomePageCacheServiceImpl homePageCacheService;

    @Autowired
    private PageConfigService pageConfigService;

    @Autowired
    private UcRpcService ucRpcService;

    private static final String BLOCK = "block";
    private static final String FEED = "feed";

    private static final String UID_DEFAULT_VALUE= "0";

    @BProfiler(key = "com.biyao.moses.controller.RecommendUiController.recommendTest", monitorType = {MonitorType.TP,
            MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR})
    @ApiOperation(value = "推荐测试工具接口")
    @PostMapping("/recommendtest")
    public ApiResult<Page<TemplateInfo>> recommendTest(@ApiParam UITestRequest testRequest, @RequestBody(required = false) UIBaseBody uiBaseBody) {

        ApiResult<Page<TemplateInfo>> apiResult = new ApiResult<>();

        if (StringUtils.isBlank(testRequest.getUuid()) || StringUtils.isBlank(testRequest.getSource()) ||
                testRequest.getPageIndex() == null) {
            apiResult.setError("入参有误，uuid、source、pageIndex不可为空");
            return apiResult;
        }
        apiResult.setSuccess(ErrorCode.SUCCESS_CODE);

        //处理pageId topicId 加入testRequest
        dealWithRequest(testRequest, apiResult);

        ByUser user = new ByUser();
//        UIBaseBody uiBaseBody = new UIBaseBody();
        //除了ByUser参数
        dealWithByUser(testRequest, uiBaseBody, user);

        try {
            // 0 参数校验
            if (Integer.valueOf(testRequest.getPageIndex()) <= 0) {
                return new ApiResult<Page<TemplateInfo>>(ErrorCode.PARAM_ERROR_CODE, "页码错误", null);
            }

            // user 校验
            if (!user.valid()) {
                return new ApiResult<Page<TemplateInfo>>(ErrorCode.PARAM_ERROR_CODE, "用户参数错误", null);
            }

            // 初始化结果集
            Page<TemplateInfo> resultPage = new Page<TemplateInfo>();
            List<Block<TemplateInfo>> resultblockList = new ArrayList<Block<TemplateInfo>>();
            resultPage.setBlockList(resultblockList);
            resultPage.setPid(testRequest.getPageId());
            apiResult.setData(resultPage);

            // 1 获取模板
            user.setPageId(testRequest.getPageId());
            Page<TotalTemplateInfo> page = pageConfigService.queryPageById(testRequest.getPageId(), user);
            if (page == null) {
                return new ApiResult<Page<TemplateInfo>>(ErrorCode.SYSTEM_ERROR_CODE, "获取模板失败", null);
            }

            // 2 解析模板填充数据
            resultPage.setPageName(page.getPageName());
            // 获取当前页面名称
            if (StringUtils.isNotBlank(testRequest.getTopicId())) {

                String pageName = redisUtil
                        .getString(CommonConstants.PAGENAME_DEFAULT_PREFIX + testRequest.getTopicId());
                if (StringUtils.isNotBlank(pageName)) {
                    resultPage.setPageName(pageName);
                }
            }

            int blockSize = page.getBlockList().size();
            Integer pageIndex = Integer.valueOf(testRequest.getPageIndex());
            Integer startIndex = (pageIndex - 1) * CommonConstants.PAGESIZE;

            // 楼层模板数据
            List<Block<TotalTemplateInfo>> blockList = page.getBlockList();

            // 计算feed页码
            Integer feedIndex = 0;
            Block<TotalTemplateInfo> lastBlock = blockList.get(blockList.size() - 1);
            if (lastBlock.isFeed()) {
                // 最后一个区块模板是feed
                // feed的第一页是pageIndex的第几页
                int a = blockSize % CommonConstants.PAGESIZE == 0 ? (blockSize / CommonConstants.PAGESIZE)
                        : (blockSize / CommonConstants.PAGESIZE + 1);
                feedIndex = (pageIndex - a + 1) < 0 ? 0 : (pageIndex - a + 1);
            }

            ArrayList<String> sortList = new ArrayList<String>();
            Map<String, Future<Block<TemplateInfo>>> futureMap = new HashMap<String, Future<Block<TemplateInfo>>>();
            // 楼层模板处理
            for (int i = startIndex; i < blockList.size() && i < startIndex + CommonConstants.PAGESIZE; i++) {
                Block<TotalTemplateInfo> block = blockList.get(i);
                boolean feed = block.isFeed();
                if (feed) { // 如果是feed流，拉出去单独处理
                    continue;
                }
                Future<Block<TemplateInfo>> futureResult = matchAndRankAnsyService.matchAndRank(block, 0,
                        testRequest, user, i);
                sortList.add(BLOCK + i);
                futureMap.put(BLOCK + i, futureResult);
            }

            // feed流处理
            if (feedIndex >= 1) {
                Future<Block<TemplateInfo>> feedMatchAndRank = matchAndRankAnsyService.feedMatchAndRank(lastBlock,
                        feedIndex, testRequest, user);
                sortList.add(FEED + feedIndex);
                futureMap.put(FEED + feedIndex, feedMatchAndRank);
            }
            // 异步数据处理
            for (int i = 0; i < sortList.size(); i++) {
                String key = sortList.get(i);
                Future<Block<TemplateInfo>> future = futureMap.get(key);

                Block<TemplateInfo> result = null;
                try {
                    result = future.get(1500l, TimeUnit.MILLISECONDS);
                } catch (Exception e) {
                    log.error("楼层数据获取超时");
                    continue;
                }
                if (result != null) {
                    resultblockList.add(result);
                }
            }

        } catch (Exception e) {
            log.error("[严重异常]test推荐中间页填充模板数据失败", e);
            return new ApiResult<Page<TemplateInfo>>(ErrorCode.SYSTEM_ERROR_CODE, "系统未知错误", null);
        } finally {
            UserContext.manulClose();
        }
        return apiResult;
    }

    private void dealWithByUser(UITestRequest testRequest, UIBaseBody uiBaseBody, ByUser user) {
        if(uiBaseBody != null){
            if(StringUtils.isNotBlank(uiBaseBody.getAdvertInfo())) {
                testRequest.setAdvertInfo(uiBaseBody.getAdvertInfo());
            }
            if(StringUtils.isNotBlank(uiBaseBody.getSelectedScreenAttrs())){
                testRequest.setSelectedScreenAttrs(uiBaseBody.getSelectedScreenAttrs());
            }
        }
        user.setTest(true);
        user.setDebug(redisCache.isHomeFeedWhite(user.getUuid()));
        user.setAdvertInfoList(AdvertInfoService.parseAdvertInfo(testRequest.getAdvertInfo()));
        user.setUuid(testRequest.getUuid());
        user.setPageVersion(testRequest.getPageVersion());
        // 本次请求的唯一id
        String sid = IdCalculateUtil.createUniqueId();
        testRequest.setSid(sid);
        user.setSex(getUserSexFromUc(String.valueOf(testRequest.getUid()), testRequest.getUuid()));
        user.setAvn("288");
        user.setDevice("1");
        user.setDid("1");
        user.setPf("ios");
        user.setSiteId("7");
        user.setPlatform(PlatformEnumUtil.getPlatformEnumBySiteId(7));
        user.setAidMap(new ConcurrentHashMap<String, String>());
        // 确保pvid一定不为空
        if (StringUtils.isBlank(testRequest.getPvid())) {
            user.setPvid(IdCalculateUtil.createUniqueId());
        }
        user.setTrackMap(new ConcurrentHashMap<String, TraceDetail>());
        user.setSessionId(IdCalculateUtil.createUniqueId());
        user.setUpcUserType(getUpcUserType(testRequest.getUuid(), testRequest.getUid().toString()));
    }

    private void dealWithRequest(@ApiParam UITestRequest testRequest, ApiResult<Page<TemplateInfo>> apiResult) {
        ApiResult<RecommendPage> pageIdCache = homePageCacheService.getPageIdCache(testRequest.getSource(), "288", "ios");
        if (apiResult != null && apiResult.getSuccess().equals(ErrorCode.SUCCESS_CODE)) {
            String pid = pageIdCache.getData().getPid();
            String topicId = pageIdCache.getData().getTopicId();
            testRequest.setPageId(pid);
            testRequest.setTopicId(topicId);
        }
    }

    /**
     * 从UC中获取用户性别
     * @param uid
     * @param uuid
     * @return
     */
    private String getUserSexFromUc(String uid,String uuid) {
        String result = com.biyao.moses.common.constant.CommonConstants.UNKNOWN_SEX;
        if(UID_DEFAULT_VALUE.equals(uid) && StringUtils.isBlank(uuid)){
            log.error("[严重异常]uuid 和 uid都为空");
            return result;
        }
        try {
            List<String> fields = new ArrayList<>();
            fields.add(UserFieldConstants.SEX);
            String uidParam = UID_DEFAULT_VALUE.equals(uid) ? null : uid;
            User user = ucRpcService.getData(uuid, uidParam, fields, "moses");
            if (user != null && user.getSex() != null) {
                result = user.getSex().toString();
            }
        }catch (Exception e){
            log.error("[严重异常]获取用户性别异常， uuid {}, uid {}, e ", uuid, uid, e);
        }
        return result;
    }

    private Integer getUpcUserType(String uuid, String uid) {
        Integer result = UPCUserTypeConstants.NEW_VISITOR; // 默认新访客
        if (StringUtils.isBlank(uuid)) {
            return result;
        }
        try {
            VisitorInfoParam vi = new VisitorInfoParam();
            vi.setCallSysName("moses.biyao.com");

            if (StringUtils.isNotBlank(uid) && Long.parseLong(uid) > 0) {
                vi.setCustomerId(Long.parseLong(uid));
            } else {
                vi.setCustomerId(null);
            }
            vi.setUuid(uuid);
            IBusinessTocDubboService businessTocDubboService = ApplicationContextProvider.getBean(IBusinessTocDubboService.class);
            com.biyao.bsrd.common.client.model.result.Result<VisitorInfoDTO> visitorInfo =
                    businessTocDubboService.getVisitorInfo(vi);
            if (visitorInfo != null && visitorInfo.getObj() != null) {
                if (!visitorInfo.getObj().isMatch()) { // 老客
                    result = UPCUserTypeConstants.CUSTOMER;
                } else if (visitorInfo.getObj().getVisitorType() == 1) { // 老访客
                    result = UPCUserTypeConstants.OLD_VISITOR;
                }
            }
        } catch (Exception e) {
            log.error("调用upc接口查询用户身份出错", e);
        }

        return result;
    }
}
