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
 * @author xiaojiankai@idstaff.com
 * @date 2019/12/23
 * 曝光商品降权排序：曝光降权 +尺码降权 + 退货退款率 + 视频因子
 **/
@Slf4j
@Component(value = RankNameConstants.BASE_REDUCE_WEIGHT_RANK)
public class BaseReduceWeightRankImpl implements Rank2 {

//    @Autowired
//    private ProductExposurePunishmentImpl productExposurePunishmentImpl;
//
//    @Autowired
//    private ProductSizePunishmentImpl productSizePunishmentImpl;
//    //引入退货退款率惩罚因子计算实现类
//    @Autowired
//    private ProductRefundRatePunishmentImpl productRefundRatePunishmentImpl;
//    @Autowired
//    private ProductVideoFactorImpl productVideoFactor;
//
//    @Autowired
//    private UcRpcService ucRpcService;
    @Autowired
    DroolsRankImpl droolsRank;

    /**
     * 内容策略项目  修改成规则引擎的形式
     * @param rankRequest2
     * @return
     */
    @Override
    public List<RankItem2> rank(RankRequest2 rankRequest2) {

//        User ucUser = ucRpcService.getUcUserForRank(rankRequest2.getUid(), rankRequest2.getUuid(), "mosesrank");
//
//        List<MatchItem2> matchItemList = rankRequest2.getMatchItemList();
//        String uuid = rankRequest2.getUuid();
        String punishFactor = PunishmentEnum.ProductExposurePunishment.getId() + "," +
                PunishmentEnum.ProductSizePunishment.getId() + "," +
                PunishmentEnum.ProductRefundRatePunishment.getId() + "," +
                PunishmentEnum.ProductVideoFactor.getId();
        rankRequest2.setPunishFactor(punishFactor);
        rankRequest2.setRecallPoints(true);
        rankRequest2.setPriceFactor(false);
        return  droolsRank.rank(rankRequest2);
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
//
//            if(debug){
//                log.error("[DEBUG]:sid {}，uuid {}，曝光惩罚因子 {}", sid, uuid, JSON.toJSONString(exposurePunishmentMap));
//            }
//            //获取尺码惩罚因子
//            Map<Long, Double> sizePunishmentMap = productSizePunishmentImpl.
//                    getPunishment(uuid, matchItemList, ucUser);
//            if(debug){
//                log.error("[DEBUG]:sid {}，uuid {}，尺码惩罚因子 {}", sid, uuid, JSON.toJSONString(sizePunishmentMap));
//            }
//            //获取退货退款率惩罚因子
//            Map<Long, Double> refundRatePunishmentMap = productRefundRatePunishmentImpl.
//                    getPunishment(uuid, matchItemList, ucUser);
//            if(debug){
//                log.error("[DEBUG]:sid {}，uuid {}，退货退款率惩罚因子 {}", sid, uuid, JSON.toJSONString(refundRatePunishmentMap));
//            }
//
//            for (MatchItem2 item : matchItemList) {
//                double exposurePunishment = exposurePunishmentMap.getOrDefault(item.getProductId(), 1.0);
//                double sizePunishment = sizePunishmentMap.getOrDefault(item.getProductId(), 1.0);
//                double refundRatePunishment = refundRatePunishmentMap.getOrDefault(item.getProductId(),1.0);
//                item.setScore(item.getScore() * (exposurePunishment * sizePunishment*refundRatePunishment));
//            }
//
//            //按照召回分*惩罚因子倒排
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

//        return rankItem2List;
    }
}
