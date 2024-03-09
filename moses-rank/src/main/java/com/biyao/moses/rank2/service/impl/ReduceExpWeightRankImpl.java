package com.biyao.moses.rank2.service.impl;

import com.biyao.moses.common.constant.RankNameConstants;
import com.biyao.moses.common.enums.PunishmentEnum;
import com.biyao.moses.model.rank2.RankItem2;
import com.biyao.moses.params.rank2.RankRequest2;
import com.biyao.moses.rank2.service.Rank2;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author zhaiweixi@idstaff.com
 * @date 2019/10/12
 * 曝光商品降权排序
 * since 191023 version
 **/
@Slf4j
@Component(value = RankNameConstants.REDUCE_EXPOSURE_WEIGHT)
public class ReduceExpWeightRankImpl implements Rank2 {

//    @Autowired
//    private ProductExposurePunishmentImpl productExposurePunishmentImpl;
//
//    @Autowired
//    private ProductSizePunishmentImpl productSizePunishmentImpl;
//    //引入退货退款率惩罚因子计算实现类
//    @Autowired
//    private ProductRefundRatePunishmentImpl productRefundRatePunishmentImpl;
//
//    @Autowired
//    private UcRpcService ucRpcService;
//
//    @Autowired
//    private ProductPriceFactorImpl productPriceFactor;
    @Autowired
    DroolsRankImpl droolsRank;

    @Override
    public List<RankItem2> rank(RankRequest2 rankRequest2) {
        String punishFactor = PunishmentEnum.ProductExposurePunishment.getId() + "," +
                PunishmentEnum.ProductSizePunishment.getId() + "," +
                PunishmentEnum.ProductRefundRatePunishment.getId() + "," +
                PunishmentEnum.ProductVideoFactor.getId();
        rankRequest2.setPunishFactor(punishFactor);
        rankRequest2.setRecallPoints(true);
        rankRequest2.setPriceFactor(true);
        return  droolsRank.rank(rankRequest2);
    }
}
//        List<String> fieldsList = new ArrayList<>();
//        fieldsList.add(UserFieldConstants.PERSONALSIZE);
//        fieldsList.add(UserFieldConstants.EXPPIDS);
//        String uidStr = null;
//        Integer uid = rankRequest2.getUid();
//        if(uid != null && uid > 0){
//            uidStr = uid.toString();
//        }
//        User ucUser = ucRpcService.getData(rankRequest2.getUuid(), uidStr, fieldsList, "mosesrank");
//
//        List<MatchItem2> matchItemList = rankRequest2.getMatchItemList();
//        String uuid = rankRequest2.getUuid();
//
//        try {
//            //获取日志debug开关
//            boolean debug = rankRequest2.getDebug() == null ? false : rankRequest2.getDebug();
//            String sid = rankRequest2.getSid();
//
//            if(debug){
//                log.error("[DEBUG]:sid {}，uuid {}，待曝光降权rank数据 {}", sid, uuid, JSON.toJSONString(matchItemList));
//            }
//
//            //获取曝光惩罚因子
//            Map<Long, Double> exposurePunishmentMap = productExposurePunishmentImpl.
//                    getPunishment(uuid, matchItemList, ucUser);
//            if(debug){
//                log.error("[DEBUG]:sid {}，uuid {}，曝光惩罚因子 {}", sid, uuid, JSON.toJSONString(exposurePunishmentMap));
//            }
//            //获取尺码惩罚因子
//            Map<Long, Double> sizePunishmentMap = productSizePunishmentImpl.
//                    getPunishment(uuid, matchItemList, ucUser);
//            if(debug){
//                log.error("[DEBUG]:sid {}，uuid {}，尺码惩罚因子 {}", sid, uuid, JSON.toJSONString(sizePunishmentMap));
//            }
//            //获取价格因子
//            Map<Long, Double> priceFactorMap = productPriceFactor.getPunishment(uuid, matchItemList, ucUser);
//            if(debug){
//                log.error("[DEBUG]:sid {}，uuid {}，价格因子 {}", sid, uuid, JSON.toJSONString(priceFactorMap));
//            }
//            //获取退货退款率惩罚因子
//            Map<Long, Double> refundRatePunishmentMap = productRefundRatePunishmentImpl.
//                    getPunishment(uuid, matchItemList, ucUser);
//            if(debug){
//                log.error("[DEBUG]:sid {}，uuid {}，退货退款率惩罚因子 {}", sid, uuid, JSON.toJSONString(refundRatePunishmentMap));
//            }
//
//            for (MatchItem2 item : matchItemList) {
//                double priceFactorScore = priceFactorMap.getOrDefault(item.getProductId(), 0.0);
//                double exposurePunishment = exposurePunishmentMap.getOrDefault(item.getProductId(), 1.0);
//                double sizePunishment = sizePunishmentMap.getOrDefault(item.getProductId(), 1.0);
//                double refundRatePunishment = refundRatePunishmentMap.getOrDefault(item.getProductId(),1.0);
//                item.setScore(item.getScore() * priceFactorScore * (exposurePunishment * sizePunishment*refundRatePunishment));
//            }
//
//            //按照召回分*价格*惩罚因子倒排
//            Collections.sort(matchItemList, new Comparator<MatchItem2>() {
//                @Override
//                public int compare(MatchItem2 o1, MatchItem2 o2) {
//                    return o2.getScore().compareTo(o1.getScore());
//                }
//            });
//
//            if(debug){
//                log.error("[DEBUG]:sid {}，uuid {}，曝光降权rank后结果： {}", sid, uuid, JSON.toJSONString(matchItemList));
//            }
//
//        } catch (Exception e) {
//            log.error("[严重异常]根据曝光惩罚因子排序出错  uuid {}", uuid, e);
//        }
//
//        //组装结果返回
//        List<RankItem2> rankItem2List = RankUtils.convert(matchItemList);
//
//        return rankItem2List;
//    }
//
//    /**
//     * 计算lg P （以10为底 价格的对数）
//     *
//     * @param price
//     * @return
//     */
//    private double getlgP(long price) {
//        double rt = Math.log(price) / Math.log(10);
//        return rt;
//    }
//}
