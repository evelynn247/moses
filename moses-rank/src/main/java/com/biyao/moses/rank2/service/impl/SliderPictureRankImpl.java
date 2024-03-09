package com.biyao.moses.rank2.service.impl;

import com.biyao.moses.cache.ProductDetailCache;
import com.biyao.moses.common.constant.RankNameConstants;
import com.biyao.moses.model.match2.MatchItem2;
import com.biyao.moses.model.rank2.RankItem2;
import com.biyao.moses.params.ProductInfo;
import com.biyao.moses.params.rank2.RankRequest2;
import com.biyao.moses.punishment.impl.ProductExposurePunishmentImpl;
import com.biyao.moses.rank2.service.Rank2;
import com.biyao.moses.rpc.UcRpcService;
import com.biyao.moses.util.RankUtils;
import com.by.profiler.annotation.BProfiler;
import com.by.profiler.annotation.MonitorType;
import com.uc.domain.bean.User;
import com.uc.domain.constant.UserFieldConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 轮播图曝光降权排序
 */
@Slf4j
@Component(value = RankNameConstants.SLIDER_PICTURE_RANK)
public class SliderPictureRankImpl implements Rank2 {

    @Autowired
    private ProductExposurePunishmentImpl productExposurePunishmentImpl;
    @Autowired
    private ProductDetailCache productDetailCache;
    @Autowired
    private UcRpcService ucRpcService;

    @BProfiler(key = "SliderPictureRankImpl.rank", monitorType = {
            MonitorType.TP, MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR })
    @Override
    public List<RankItem2> rank(RankRequest2 rankRequest2) {

        List<MatchItem2> matchItemList = rankRequest2.getMatchItemList();
        String uuid = rankRequest2.getUuid();

        try {
            List<String> fieldsList = new ArrayList<>();
            fieldsList.add(UserFieldConstants.EXPPIDS);
            String uidStr = null;
            Integer uid = rankRequest2.getUid();
            if(uid != null && uid > 0){
                uidStr = uid.toString();
            }
            User ucUser = ucRpcService.getData(rankRequest2.getUuid(), uidStr, fieldsList, "mosesrank");

            //获取曝光惩罚因子
            Map<Long, Double> exposurePunishmentMap = productExposurePunishmentImpl.
                    getPunishment(uuid, matchItemList, ucUser);

            for (MatchItem2 item : matchItemList) {
                ProductInfo productInfo = productDetailCache.getProductInfo(item.getProductId());
                double priceScore = 0;
                if (productInfo != null && productInfo.getPrice() != null) {
                    //计算lgP （以10为底 价格的对数）
                    priceScore = getlgP(productInfo.getPrice() / 100);
                }
                double exposurePunishment = exposurePunishmentMap.getOrDefault(item.getProductId(), 1.0).doubleValue();
                item.setScore((item.getScore() * priceScore * exposurePunishment));
            }

            //按照召回分*价格*曝光因子倒排
            Collections.sort(matchItemList, new Comparator<MatchItem2>() {
                @Override
                public int compare(MatchItem2 o1, MatchItem2 o2) {
                    return o2.getScore().compareTo(o1.getScore());
                }
            });
        } catch (Exception e) {
            log.error("[严重异常]根据曝光惩罚因子排序出错 uuid {},", uuid, e);
        }

        //组装结果返回
        List<RankItem2> rankItem2List = RankUtils.convert(matchItemList);

        return rankItem2List;
    }

    /**
     * 计算lg P （以10为底 价格的对数）
     *
     * @param price
     * @return
     */
    private double getlgP(long price) {
        double rt = Math.log(price) / Math.log(10);
        return rt;
    }
}
