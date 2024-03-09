package com.biyao.moses.rank.impl;


import com.biyao.moses.common.enums.SortEnum;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;
import com.biyao.moses.rank.RankRequest;
import com.biyao.moses.rank.RecommendRank;
import com.by.profiler.annotation.BProfiler;
import com.by.profiler.annotation.MonitorType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Component("NovicePriceRank")
public class NovicePriceRank implements RecommendRank {

    @BProfiler(key = "NovicePriceRank.executeRecommend", monitorType = {
            MonitorType.TP, MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR })
    @Override
    public List<TotalTemplateInfo> executeRecommend(RankRequest rankRequest) {
        List<TotalTemplateInfo> oriData = rankRequest.getOriData();
        try{

            if (SortEnum.DES.getType().equals(rankRequest.getSortValue())) {
                //降序
                Collections.sort(oriData, new Comparator<TotalTemplateInfo>() {
                    @Override
                    public int compare(TotalTemplateInfo o1, TotalTemplateInfo o2) {
                        if (Double.valueOf(o1.getNovicePrice()) > Double.valueOf(o2.getNovicePrice())) {
                            return -1;
                        } else if (Double.valueOf(o1.getNovicePrice()) < Double.valueOf(o2.getNovicePrice())) {
                            return 1;
                        } else {
                            return 0;
                        }
                    }
                });
            }else{
                //升序
                Collections.sort(oriData, new Comparator<TotalTemplateInfo>() {
                    @Override
                    public int compare(TotalTemplateInfo o1, TotalTemplateInfo o2) {
                        if (Double.valueOf(o2.getNovicePrice()) > Double.valueOf(o1.getNovicePrice())) {
                            return -1;
                        } else if (Double.valueOf(o2.getNovicePrice()) < Double.valueOf(o1.getNovicePrice())) {
                            return 1;
                        } else {
                            return 0;
                        }
                    }
                });
            }

        }catch(Exception e){
            log.error("rank未知错误", e);
        }

        return oriData;
    }
}
