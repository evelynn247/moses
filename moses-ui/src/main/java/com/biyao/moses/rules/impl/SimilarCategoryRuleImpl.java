package com.biyao.moses.rules.impl;

import com.biyao.moses.model.template.entity.TotalTemplateInfo;
import com.biyao.moses.params.AdvertInfo;
import com.biyao.moses.rules.Rule;
import com.biyao.moses.rules.RuleConst;
import com.biyao.moses.rules.RuleContext;
import com.biyao.moses.service.imp.AdvertInfoService;
import com.biyao.moses.util.PartitionUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 相邻商品(包括斜侧)相似类目岔开
 */
@Slf4j
@Component(value = RuleConst.RULE_SIMILAR_CATEGORY)
public class SimilarCategoryRuleImpl implements Rule {

    @Autowired
    private PartitionUtil partitionUtil;

    @Autowired
    private AdvertInfoService advertInfoService;

    @Override
    public List<TotalTemplateInfo> ruleRank(RuleContext ruleContext) {

        List<TotalTemplateInfo> result = new ArrayList<>();//返回集合
        if(CollectionUtils.isEmpty(ruleContext.getAllProductList())){
            return  result;
        }
        try {
            List<TotalTemplateInfo> allShowPidList = new LinkedList<>(ruleContext.getAllProductList());
            int size = allShowPidList.size();

            List<AdvertInfo> advertInfoList = new ArrayList<>(ruleContext.getAdvertInfoList());
            //获取每次循环时需要展示的活动入口集合信息
            Map<Integer, List<AdvertInfo>> loopAdvertInfosMap = advertInfoService.getLoopAdvertInfosMap(advertInfoList);

            int insertNumPerTimes = PartitionUtil.GET_PRODUCT_NUM_PER_TIMES;
            int count = size % insertNumPerTimes == 0 ? size / insertNumPerTimes : size / insertNumPerTimes + 1;
            int insertNum = 0;
            List<TotalTemplateInfo> insertNewList = new ArrayList<>(insertNumPerTimes);
            for (int index = 0; index < insertNumPerTimes; index++) {
                insertNewList.add(null);
            }

            Set<String> hasShowedPidSet = new HashSet<>();
            Set<Long> lastSimilarCate3Set = new HashSet<>();
            Set<Long> insertedSimilarCate3Set = new HashSet<>();
            for (int i = 0; i < count; i++) {
                List<AdvertInfo> loopAdvertInfos = loopAdvertInfosMap.get(i);
                if(CollectionUtils.isNotEmpty(loopAdvertInfos)){
                    for(AdvertInfo advertInfo : loopAdvertInfos){
                        //插入位置从1开始，这里需要转换为从0开始
                        int position = advertInfo.getPosition() - 1;
                        insertNewList.set(position % insertNumPerTimes, advertInfo.getTotalTemplateInfo());
                        insertNum++;
                        advertInfoList.remove(advertInfo);
                    }
                }
                partitionUtil.dealWaitShowPids(insertNewList, allShowPidList, lastSimilarCate3Set, insertedSimilarCate3Set, hasShowedPidSet);

                //获取上一页10个中的最后两个商品
                lastSimilarCate3Set.clear();
                TotalTemplateInfo last1 = insertNewList.get(insertNumPerTimes - 1);
                TotalTemplateInfo last2 = insertNewList.get(insertNumPerTimes - 2);
                if (last1 != null) {
                    Long similarCate3Tmp = partitionUtil.getSimilar3CategoryIdByPid(last1);
                    if(similarCate3Tmp != null){
                        lastSimilarCate3Set.add(similarCate3Tmp);
                    }

                }
                if (last2 != null) {
                    Long similarCate3Tmp = partitionUtil.getSimilar3CategoryIdByPid(last2);
                    if(similarCate3Tmp != null){
                        lastSimilarCate3Set.add(similarCate3Tmp);
                    }
                }

                //清除已插入的商品相似三级类目集合
                insertedSimilarCate3Set.clear();
                boolean hasNull = false;
                //将隔断后的10个结果放到最终的返回结果中，并且将插入后的集合内的元素设置为null
                for (int index = 0; index < insertNumPerTimes; index++) {
                    TotalTemplateInfo info = insertNewList.get(index);
                    //说明所有待展示商品都已经填充完成
                    if(info == null){
                        hasNull = true;
                        insertNewList.set(index, null);
                        continue;
                    }
                    //如果前面出现了null元素,则判断能否添加该元素
                    if(hasNull && !advertInfoService.isFitAtLast(result, info,ruleContext.getUiBaseRequest().getPagePositionId())){
                        insertNum--;
                        continue;
                    }
                    result.add(info);
                    insertNewList.set(index, null);
                }
                //重新计算count
                if(!hasNull){
                    int newSize = Math.min(size + insertNum, 500);
                    count =  newSize % insertNumPerTimes == 0 ? newSize / insertNumPerTimes : newSize / insertNumPerTimes + 1;
                }
            }
            //说明插入位置大于商品的个数
            if(CollectionUtils.isNotEmpty(advertInfoList)
                && advertInfoService.isFitInsertAdvertAtLast(result, advertInfoList.get(0),ruleContext.getUiBaseRequest().getPagePositionId())){
                result.add(advertInfoList.get(0).getTotalTemplateInfo());
            }
        }catch (Exception e){
            log.error("[严重异常]相似三级类目岔开错误，", e);
            result = ruleContext.getAllProductList();
        }
        return result.size()> 500 ? result.subList(0, 500) : result;
    }
}
