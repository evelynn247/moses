package com.biyao.moses.match2.service.bizimpl;

import com.biyao.moses.common.constant.BizNameConst;
import com.biyao.moses.common.constant.CommonConstants;
import com.biyao.moses.match2.service.BizService;
import com.biyao.moses.match2.util.MatchUtil;
import com.biyao.moses.model.match2.MatchItem2;
import com.biyao.moses.params.match2.MatchRequest2;
import com.by.profiler.annotation.BProfiler;
import com.by.profiler.annotation.MonitorType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 首页轮播图-APP合规1.5-安卓app合规整改项目新增
 */
@Slf4j
@Component(value = BizNameConst.SLIDER_PICTURE_HOTS)
public class SliderPictureHotsServiceImpl implements BizService {

    @Autowired
    private MatchUtil matchUtil;

    @Autowired
    private SliderPicture2ServiceImpl sliderPicture2ServiceImpl;

    /**
     *  轮播图match 期望推出的商品数
     */
    private final int EXPECT_PID_NUM = 100;


    @BProfiler(key = "SliderPictureHotsServiceImpl.match",
            monitorType = {MonitorType.TP, MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR})
    @Override
    public List<MatchItem2> match(MatchRequest2 request) {

        List<MatchItem2> resultList = new ArrayList<>();

        //将用户性别设置为未知
        request.setUserSex(Integer.valueOf(CommonConstants.UNKNOWN_SEX));

        //后台二级类目下有效的商品集合缓存
        Map<Long, List<Long>> cate2ValidPidCacheMap = new HashMap<>();

        resultList.addAll(sliderPicture2ServiceImpl.getLbtHotSourceData(request, cate2ValidPidCacheMap, EXPECT_PID_NUM));

        //按照排名填充商品分，并聚合商品分
        matchUtil.calculateAndFillScore(resultList, EXPECT_PID_NUM, new HashMap<>());

        //按商品分进行排序
        resultList = resultList.stream().sorted((m1, m2) -> -m1.getScore().compareTo(m2.getScore())).collect(Collectors.toList());

        return resultList;
    }
}
