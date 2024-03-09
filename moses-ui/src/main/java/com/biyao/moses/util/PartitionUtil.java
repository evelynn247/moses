package com.biyao.moses.util;

import com.biyao.moses.cache.Category3RebuyCycleCache;
import com.biyao.moses.cache.ProductDetailCache;
import com.biyao.moses.cache.SimilarCategory3IdCache;
import com.biyao.moses.common.constant.CacheRedisKeyConstant;
import com.biyao.moses.common.constant.ExpFlagsConstants;
import com.biyao.moses.common.constant.UPCUserTypeConstants;
import com.biyao.moses.common.enums.SeasonEnum;
import com.biyao.moses.constants.CommonConstants;
import com.biyao.moses.context.ByUser;
import com.biyao.moses.exp.MosesExpConst;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;
import com.biyao.moses.params.AdvertInfo;
import com.biyao.moses.params.BaseRequest2;
import com.biyao.moses.params.ProductInfo;
import com.biyao.moses.rpc.PushTokenService;
import com.biyao.moses.rpc.UcRpcService;
import com.biyao.moses.service.InsertRequest;
import com.biyao.moses.service.InsertResult;
import com.biyao.moses.service.imp.AdvertInfoService;
import com.biyao.upc.dubbo.client.business.toc.IBusinessTocDubboService;
import com.biyao.upc.dubbo.dto.VisitorInfoDTO;
import com.biyao.upc.dubbo.param.business.VisitorInfoParam;
import com.uc.domain.bean.User;
import com.uc.domain.constant.UserFieldConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

/**
 * @ClassName PartitionUtil
 * @Description 通用方法
 * @Author admin
 * @Date 2019/12/31 19:14
 * @Version 1.0
 **/
@Slf4j
@Component
public class PartitionUtil {
    @Autowired
    private SimilarCategory3IdCache similarCategory3IdCache;

    @Autowired
    private ProductDetailCache productDetailCache;

    @Autowired
    private CacheRedisUtil cacheRedisUtil;

    @Autowired
    private AdvertInfoService advertInfoService;

    @Autowired
    private UcRpcService ucRpcService;

    @Autowired
    private Category3RebuyCycleCache category3RebuyCycleCache;

    @Autowired
    private PushTokenService pushTokenService;

    //商品数量上限
    private final int PID_NUM_MAX_LIMIT = 500;

    //已插入商品数量的存储上限
    private final int INSERT_PID_NUM_MAX_LIMIT = 100;

    //新首页曝光缓存过期时间为3天
    private final int EXPIRE_TIME_3DAY = 259200;

    /**
     * 同一相似三级类目的商品个数默认值
     */
    private final int CATEGORY_NUM_DEFAULT = 2;

    /**
     * 60*60*24*1000 1天毫秒数
     */
    private static final long ONE_DAY_MS = 86400000L;
    /**
     * 60*60*24*1000*3 3天毫秒数
     */
    private static final long THREE_DAY_MS = 259200000L;
    /**
     * 60*60*24*1000*30 30天毫秒数
     */
    private static final long THIRTY_DAY_MS = 2592000000L;

    /**
     * 从全量待展示商品集合中一次取10个商品
     */
    public static final int GET_PRODUCT_NUM_PER_TIMES = 10;
    /**
     * 双排展示模板，10个商品中可插入商品的起始位置
     */
    private static final int INSERT_START_POSITION_IN_10PRODUCTS = 3;
    /**
     * 双排展示模板，10个商品中可插入商品的最后位置
     */
    private static final int INSERT_END_POSITION_IN_10PRODUCTS = 8;
    /**
     * 10个商品中最多插入3个商品
     */
    private static final int MAX_INSERT_PRODUCT = 3;

    /**
     * 每20个商品做隔断，需要分析前19后19位置的商品
     */
    public static final int BEFORE_CURRENT_PRODUCT_NUM = 19;

    private static final Random random = new Random();


    /**
     * desc 每次从待插入商品集合中选择0~3个待插商品依次插入待展示商品集合中
     *
     * @param allWaitShowProducts 全量待展示商品集合，一次请求召回500个商品,按照每页展示10个或者20个的方式返回给用户，插入待插商品后去重，更新结果依然存储在allWaitShowProducts中
     * @param waitInsertProducts  待插入商品集合，从rtbr2招会源召回0~100个用户感兴趣的商品，按照插入分倒排并过滤掉当天已经插入过的商品后存储在waitInsertProducts中
     * @param insertedProducts    本次横插操作完成，新插入待展示商品集合中的商品
     * @param pageIndex           表示用户请求的页面index，下标从1开始，用户翻页一次，pageIndex增加1
     * @param pageSize            表示一页展示的商品数量
     */
    private void insert(List<TotalTemplateInfo> allWaitShowProducts, List<TotalTemplateInfo> waitInsertProducts, List<String> insertedProducts,
                        int pageIndex, int pageSize) {
        //1.参数校验
        if (allWaitShowProducts.size() < GET_PRODUCT_NUM_PER_TIMES || waitInsertProducts.size() == 0) {
            return;
        }
        //一次取10个展示商品，需取商品总次数
        int totalTimes = pageSize / GET_PRODUCT_NUM_PER_TIMES;
        //全量待展示商品集合中未展示商品起始下标
        int startIndex = (pageIndex - 1) * pageSize;
        if (startIndex >= allWaitShowProducts.size()) {
            return;
        }

        //待展示商品集合中每次起始位置前的所有商品信息
        Set<String> beforeStartIndexPidSet = new HashSet<>();
        for (int i = 0; i < startIndex; i++) {
            TotalTemplateInfo templateInfo = allWaitShowProducts.get(i);
            if (templateInfo == null || templateInfo.getId() == null) {
                continue;
            }
            beforeStartIndexPidSet.add(templateInfo.getId());
        }
        for (int getTimes = 0; getTimes < totalTimes; getTimes++) {
            //如果未展示商品数量不足10个，不做横插直接返回
            if (allWaitShowProducts.size() - startIndex < GET_PRODUCT_NUM_PER_TIMES) {
                break;
            }
            //2.对10个未展示商品进行横插操作
            //本次插入时，当前取出的10个商品集合
            Set<String> thisTimePidSet = new HashSet<>();
            for (int index = 0; index < GET_PRODUCT_NUM_PER_TIMES; index++) {
                TotalTemplateInfo templateInfo = allWaitShowProducts.get(startIndex + index);
                if (templateInfo == null || templateInfo.getId() == null) {
                    continue;
                }
                thisTimePidSet.add(templateInfo.getId());
            }
            //本次已插入的商品个数
            int insertPidNum = 0;
            for(int index = 0; index < waitInsertProducts.size(); index++){
                if(insertPidNum >= MAX_INSERT_PRODUCT){
                    break;
                }
                TotalTemplateInfo waitInsertProduct = waitInsertProducts.get(index);
                //2.2 选择待插入商品，不能与待展示商品集合中当前位置之前商品重复；
                String waitInsertPid = waitInsertProduct.getId();
                if (beforeStartIndexPidSet.contains(waitInsertPid) || thisTimePidSet.contains(waitInsertPid)) {
                    continue;
                }
                //2.3查询待插入商品的相似三级类目Id
                Long waitInsertProductSimilar3Cate = getSimilar3CategoryIdByPid(waitInsertProduct);
                if (waitInsertProductSimilar3Cate == null) {
                    continue;
                }
                //2.4寻找插入位置
                boolean insertFlag=false;
                for (int insertPos = startIndex + INSERT_START_POSITION_IN_10PRODUCTS - 1; insertPos < startIndex + INSERT_END_POSITION_IN_10PRODUCTS + insertPidNum; insertPos++) {
                    boolean similar3ContainFlag = false;
                    //首先判断N-2、N-1、N、N+1位置商品的相似三级类目是否包含待插入商品的相似三级类目,其中N=insertPos表示商品的插入位置
                    for (int i = -2; i <= 1; i++) {
                        Long similar3CategoryId = getSimilar3CategoryIdByPid(allWaitShowProducts.get(insertPos + i));
                        if (waitInsertProductSimilar3Cate.equals(similar3CategoryId)) {
                            similar3ContainFlag = true;
                            break;
                        }
                    }
                    if (similar3ContainFlag) {
                        continue;
                    }
                    //N为奇数，判断N+2商品的相似三类目是否包含插入商品的相似三级类目
                    if (insertPos % 2 == 0) {
                        Long similar3CategoryId = getSimilar3CategoryIdByPid(allWaitShowProducts.get(insertPos + 2));
                        if (waitInsertProductSimilar3Cate.equals(similar3CategoryId)) {
                            continue;
                        }
                    } else {//N为偶数,判断N-3的商品的相似三类目是否包含插入商品的相似三级类目
                        Long similar3CategoryId = getSimilar3CategoryIdByPid(allWaitShowProducts.get(insertPos - 3));
                        if (waitInsertProductSimilar3Cate.equals(similar3CategoryId)) {
                            continue;
                        }
                    }
                    //记录已经插入的商品
                    insertedProducts.add(waitInsertProduct.getId());
                    thisTimePidSet.add(waitInsertProduct.getId());
                    //选中的商品插入待展示商品集合
                    allWaitShowProducts.add(insertPos, waitInsertProduct);
                    insertPidNum++;//插入商品数量+1
                    insertFlag=true;
                    break;
                }

                if(insertFlag){
                    //从头开始遍历
                    index = -1;
                }
            }
            //完成一轮插入商品后
            beforeStartIndexPidSet.addAll(thisTimePidSet);
            //10个商品横插结束，添加到返回结果中
            startIndex += GET_PRODUCT_NUM_PER_TIMES;
            //删除未展示商品中与已展示商品重复的商品
            for (int i = beforeStartIndexPidSet.size(); i < allWaitShowProducts.size(); ) {
                TotalTemplateInfo totalTemplateInfo = allWaitShowProducts.get(i);
                String pid = totalTemplateInfo.getId();
                if (beforeStartIndexPidSet.contains(pid)) {
                    allWaitShowProducts.remove(i);
                    continue;
                }
                i++;
            }
        }
    }


    /**
     * 根据商品Id查询替换详细三级类目Id
     *
     * @param templateInfo
     * @return
     */
    public Long getSimilar3CategoryIdByPid(TotalTemplateInfo templateInfo) {
        if (templateInfo == null || templateInfo.getId() == null) {
            return null;
        }
        ProductInfo productInfo = productDetailCache.getProductInfo(Long.parseLong(templateInfo.getId()));
        if (productInfo == null || productInfo.getThirdCategoryId() == null) {
            return null;
        }
        Long waitInsertProductCate3 = productInfo.getThirdCategoryId();
        Long waitInsertProductSimilar3Cate = similarCategory3IdCache.getSimilarCate3Id(waitInsertProductCate3);
        return waitInsertProductSimilar3Cate;
    }


    /**
     * 将待插入集合中的商品按照插入位置放到集合中
     * @param insertPositionList
     * @param waitInsertProducts
     * @param lastSimilarCate3Set 上一页的最后两个商品的相似三级类目集合
     * @return
     */
    private List<TotalTemplateInfo> dealWaitInsertPids(List<Integer> insertPositionList, List<TotalTemplateInfo> waitInsertProducts, Set<Long> lastSimilarCate3Set){
        //初始化插入后的集合
        List<TotalTemplateInfo> result = new ArrayList<>(GET_PRODUCT_NUM_PER_TIMES);
        for(int i = 0; i < GET_PRODUCT_NUM_PER_TIMES; i++){
            result.add(null);
        }

        //如果待插入商品集合或者待插入的位置集合为空，则不插入商品
        if(CollectionUtils.isEmpty(insertPositionList) || CollectionUtils.isEmpty(waitInsertProducts)){
            return result;
        }
        try {
            Set<Long> similarCate3Set = new HashSet<>();
            Set<Long> allSimilarCate3Set = new HashSet<>();
            boolean isExistNotSameSimilarCate3 = true;
            Set<Long> insertedSimilarCate3Set = new HashSet<>();
            for (Integer i : insertPositionList) {
                if(isExistNotSameSimilarCate3) {
                    boolean isInserted = false;
                    allSimilarCate3Set.clear();
                    if((i == 1) || (i == 2)){
                        allSimilarCate3Set.addAll(lastSimilarCate3Set);
                    }
                    allSimilarCate3Set.addAll(insertedSimilarCate3Set);
                    Iterator<TotalTemplateInfo> waitInsertIterator = waitInsertProducts.iterator();
                    while (waitInsertIterator.hasNext()) {
                        TotalTemplateInfo tmp = waitInsertIterator.next();
                        Long similarCate3Tmp = getSimilar3CategoryIdByPid(tmp);
                        if (similarCate3Tmp != null && !allSimilarCate3Set.contains(similarCate3Tmp)) {
                            result.set(i - 1, tmp);
                            insertedSimilarCate3Set.add(similarCate3Tmp);
                            waitInsertIterator.remove();
                            isInserted = true;
                            break;
                        }
                    }

                    //如果待插入的商品集合为空，则直接返回
                    if (CollectionUtils.isEmpty(waitInsertProducts)) {
                        return result;
                    }

                    if(!isInserted){
                        //如果没有找到能够插入的商品，则其他的坑位也找不到能够插入的商品
                        isExistNotSameSimilarCate3 = false;
                    }
                }

                if(!isExistNotSameSimilarCate3) {
                    //查找是否能找到上下左右隔断的商品
                    similarCate3Set.clear();
                    acquireSimilarCate3Set(i, similarCate3Set, lastSimilarCate3Set, result);
                    Iterator<TotalTemplateInfo> waitInsertIterator = waitInsertProducts.iterator();
                    boolean isInserted = false;
                    while (waitInsertIterator.hasNext()) {
                        TotalTemplateInfo tmp = waitInsertIterator.next();
                        Long similarCate3Tmp = getSimilar3CategoryIdByPid(tmp);
                        if (similarCate3Tmp != null && !similarCate3Set.contains(similarCate3Tmp)) {
                            result.set(i, tmp);
                            waitInsertIterator.remove();
                            isInserted = true;
                            break;
                        }
                    }

                    //如果待插入的商品集合为空，则直接返回
                    if (CollectionUtils.isEmpty(waitInsertProducts)) {
                        return result;
                    }

                    //如果没有能隔断的商品 则插入第一个商品
                    if (!isInserted) {
                        TotalTemplateInfo tmp = waitInsertProducts.get(0);
                        result.set(i, tmp);
                        waitInsertProducts.remove(0);
                    }
                }
            }
        }catch (Exception e){
            log.error("[一般异常]横插时处理待插入的商品集合发生错误 ", e);
        }
        return result;
    }

    /**
     * 将未展示商品集合中的商品放到已插入集合中
     * @param insertNewList
     * @param lastSimilarCate3Set
     * @param hasShowedPidSet
     * @return
     */
    public void dealWaitShowPids(List<TotalTemplateInfo> insertNewList, List<TotalTemplateInfo> notShowPidList, Set<Long> lastSimilarCate3Set, Set<Long> insertedSimilarCate3Set, Set<String> hasShowedPidSet){
        //如果未展示的商品集合为空，则直接返回
        if(CollectionUtils.isEmpty(notShowPidList)){
            return;
        }
        try {
            Set<Long> similarCate3Set = new HashSet<>();
            Set<Long> allSimilarCate3Set = new HashSet<>();
            boolean isExistNotSameSimilarCate3 = true;
            //遍历已插入的集合
            for (int i = 0; i < GET_PRODUCT_NUM_PER_TIMES; i++) {
                //如果已插入了商品，则遍历下一个
                if (insertNewList.get(i) != null) {
                    continue;
                }

                //遍历待展示集合
                if(isExistNotSameSimilarCate3) {
                    boolean isInserted = false;
                    allSimilarCate3Set.clear();
                    if((i == 0) || (i == 1)){
                        allSimilarCate3Set.addAll(lastSimilarCate3Set);
                    }
                    allSimilarCate3Set.addAll(insertedSimilarCate3Set);
                    Iterator<TotalTemplateInfo> notShowPidListIterator = notShowPidList.iterator();
                    while (notShowPidListIterator.hasNext()) {
                        TotalTemplateInfo tmp = notShowPidListIterator.next();
                        if (hasShowedPidSet.contains(tmp.getId())) {
                            notShowPidListIterator.remove();
                            continue;
                        }
                        Long similarCate3Tmp = getSimilar3CategoryIdByPid(tmp);
                        if (similarCate3Tmp != null && !allSimilarCate3Set.contains(similarCate3Tmp)) {
                            insertNewList.set(i, tmp);
                            hasShowedPidSet.add(tmp.getId());
                            insertedSimilarCate3Set.add(similarCate3Tmp);
                            notShowPidListIterator.remove();
                            isInserted = true;
                            break;
                        }
                    }

                    //如果未展示的商品集合为空，则直接返回
                    if (CollectionUtils.isEmpty(notShowPidList)) {
                        return;
                    }

                    if(!isInserted){
                        //如果没有找到能够插入的商品，则其他的坑位也找不到能够插入的商品
                        isExistNotSameSimilarCate3 = false;
                    }
                }

                if(!isExistNotSameSimilarCate3) {
                    similarCate3Set.clear();
                    acquireSimilarCate3Set(i, similarCate3Set, lastSimilarCate3Set, insertNewList);

                    Iterator<TotalTemplateInfo> notShowPidListIterator = notShowPidList.iterator();
                    boolean isInserted = false;
                    while (notShowPidListIterator.hasNext()) {
                        TotalTemplateInfo tmp = notShowPidListIterator.next();
                        if (hasShowedPidSet.contains(tmp.getId())) {
                            notShowPidListIterator.remove();
                            continue;
                        }
                        Long similarCate3Tmp = getSimilar3CategoryIdByPid(tmp);
                        if (similarCate3Tmp != null && !similarCate3Set.contains(similarCate3Tmp)) {
                            insertNewList.set(i, tmp);
                            hasShowedPidSet.add(tmp.getId());
                            notShowPidListIterator.remove();
                            isInserted = true;
                            break;
                        }
                    }

                    //如果未展示的商品集合为空，则直接返回
                    if (CollectionUtils.isEmpty(notShowPidList)) {
                        return;
                    }

                    //如果没有能隔断的商品 则插入第一个商品
                    if (!isInserted) {
                        TotalTemplateInfo tmp = notShowPidList.get(0);
                        insertNewList.set(i, tmp);
                        notShowPidList.remove(0);
                    }
                }
            }
        }catch (Exception e){
            log.error("[严重异常][隔断规则]横插时处理未展示的商品信息发生错误 ", e);
        }
    }

    /** 获取上下左右商品的相似三级类目集合
     * @param index 位置
     * @param similarCate3Set 返回的相似三级类目集合
     * @param lastSimilarCate3Set 上一页的后两个商品的相似三级类目集合
     * @param insertNewList 本次待填充的商品集合
     */
    private void acquireSimilarCate3Set(int index, Set<Long> similarCate3Set, Set<Long> lastSimilarCate3Set, List<TotalTemplateInfo> insertNewList){
        if ((index == 0) || (index == 1)) {
            similarCate3Set.addAll(lastSimilarCate3Set);
            for (int j = 0; j < 4; j++) {
                //跳过插入位置
                if (index == j) {
                    continue;
                }
                TotalTemplateInfo tmp = insertNewList.get(j);
                if (tmp != null) {
                    Long similarCate3Tmp = getSimilar3CategoryIdByPid(tmp);
                    if(similarCate3Tmp != null){
                        similarCate3Set.add(similarCate3Tmp);
                    }
                }
            }
        } else {
            for (int j = 0; j < 6; j++) {
                //跳过插入位置
                if (j == index) {
                    continue;
                }
                TotalTemplateInfo tmp = null;
                //如果是奇数位置
                if ((index + 1) % 2 == 1) {
                    if(index + j - 2 < GET_PRODUCT_NUM_PER_TIMES) {
                        tmp = insertNewList.get(index + j - 2);
                    }
                } else {
                    if(index + j - 3 < GET_PRODUCT_NUM_PER_TIMES) {
                        tmp = insertNewList.get(index + j - 3);
                    }
                }
                if (tmp != null) {
                    Long similarCate3Tmp = getSimilar3CategoryIdByPid(tmp);
                    if(similarCate3Tmp != null){
                        similarCate3Set.add(similarCate3Tmp);
                    }
                }
            }
        }
    }

    /**
     * 判断是否已经有广告入口
     * @param insertedWaitShowProducts
     * @return
     */
    private boolean isInsertedAdvert(List<TotalTemplateInfo> insertedWaitShowProducts){
        boolean result = false;
        if(CollectionUtils.isEmpty(insertedWaitShowProducts)){
            return result;
        }
        for(TotalTemplateInfo info : insertedWaitShowProducts){
            if(info == null){
                continue;
            }
            if(CommonConstants.SHOW_TYPE_ADVERT.equals(info.getShowType())){
                result = true;
            }
        }
        return result;
    }

    /**
     * desc 将商品插入到指定位置
     * @param insertRequest
     */
    public InsertResult insert2SpecificPosition(InsertRequest insertRequest) {
        List<TotalTemplateInfo> allWaitShowProducts = insertRequest.getAllWaitShowProducts();
        List<TotalTemplateInfo> waitInsertProducts = insertRequest.getWaitInsertProducts();
        List<Integer> insertPositionList = insertRequest.getInsertPositionList();
        int pageIndex = insertRequest.getPageIndex();
        int pageSize = insertRequest.getPageSize();
        Map<Long, Integer> userCategoryNum = insertRequest.getUserCategroyNum();
        List<AdvertInfo> advertInfoList = insertRequest.getAdvertInfoList();
        BaseRequest2 baseRequest2 = insertRequest.getBaseRequest2();

//        //标识是否需要插入活动广告
//        boolean hasAdvert = false;
//        int advertPageIndex = 0;
//        if(advertInfo != null && advertInfo.getTotalTemplateInfo() != null && advertInfo.getPosition() != null){
//            hasAdvert = true;
//            advertPageIndex = (advertInfo.getPosition()-1) / pageSize + 1;
//        }
        //获取每次循环时需要展示的活动入口集合信息，从0开始，每页10个商品
        Map<Integer, List<AdvertInfo>> loopAdvertInfosMap = advertInfoService.getLoopAdvertInfosMap(advertInfoList);

        InsertResult insertResult = new InsertResult();

        //如果没有了待展示的商品，则直接返回
        int startIndex = (pageIndex - 1) * pageSize;
        if (CollectionUtils.isEmpty(allWaitShowProducts) || startIndex >= allWaitShowProducts.size()) {
            insertResult.setAllWaitShowProducts(allWaitShowProducts);
            return insertResult;
        }

        int pidNum = allWaitShowProducts.size();
        //插入后最终返回的商品全集
        List<TotalTemplateInfo> insertedWaitShowProducts = new ArrayList<>(pidNum);
        List<String> insertedProducts = new ArrayList<>();

        //待展示商品集合中未展示的商品集合
        List<TotalTemplateInfo> notShowPidList = new LinkedList<>();
        //待展示商品集合中已展示的商品+本次需要展示的商品集合
        Set<String> hasShowedPidSet = new HashSet<>();
        for (int i = 0; i < pidNum; i++) {
            TotalTemplateInfo templateInfo = allWaitShowProducts.get(i);
            if (templateInfo == null || templateInfo.getId() == null) {
                continue;
            }
            if(i < startIndex) {
                hasShowedPidSet.add(templateInfo.getId());
                insertedWaitShowProducts.add(templateInfo);
            }else{
                notShowPidList.add(templateInfo);
            }
        }

        //一次取10个展示商品，需取商品总次数
        int totalTimes = pageSize / GET_PRODUCT_NUM_PER_TIMES;
        AdvertInfo lastAdvertInfo = null;
        for (int getTimes = 0; getTimes < totalTimes; getTimes++) {
            //如果未展示的商品为空，则返回
            if(CollectionUtils.isEmpty(notShowPidList)){
                continue;
            }
            //待插入集合中删除已展示的商品
            if(CollectionUtils.isNotEmpty(hasShowedPidSet)) {
                Iterator<TotalTemplateInfo> waitInsertIterator = waitInsertProducts.iterator();
                while (waitInsertIterator.hasNext()) {
                    TotalTemplateInfo info = waitInsertIterator.next();
                    if (info == null || hasShowedPidSet.contains(info.getId())) {
                        waitInsertIterator.remove();
                    }
                }
            }

            //本次循环时插入后待展示集合中待插入的第一个位置
//            int index = insertedWaitShowProducts.size();
//            Set<Long> lastSimilarCate3Set = new HashSet<>();
//            TotalTemplateInfo last1 = null;
//            TotalTemplateInfo last2 = null;
//            if(index > 2){
//                last1 = insertedWaitShowProducts.get(index - 1);
//                last2 = insertedWaitShowProducts.get(index - 2);
//                lastSimilarCate3Set.add(getSimilar3CategoryIdByPid(last1));
//                lastSimilarCate3Set.add(getSimilar3CategoryIdByPid(last2));
//            }
            //获取前19个商品
            List<TotalTemplateInfo> preProducts = new ArrayList<>();
            int insertedSize = insertedWaitShowProducts.size();
            if(insertedSize > BEFORE_CURRENT_PRODUCT_NUM){
                preProducts.addAll(insertedWaitShowProducts.subList(insertedSize-BEFORE_CURRENT_PRODUCT_NUM, insertedSize));
            }else{
                preProducts.addAll(insertedWaitShowProducts);
            }

            //初始化插入后的集合
            List<TotalTemplateInfo> rtList = new ArrayList<>(GET_PRODUCT_NUM_PER_TIMES);
            for(int i = 0; i < GET_PRODUCT_NUM_PER_TIMES; i++){
                rtList.add(null);
            }
            int curRealLoopIndex = (pageIndex - 1) * totalTimes + getTimes;
            //获取当前循环需要展示的活动入口信息
            List<AdvertInfo> loopAdvertInfos = loopAdvertInfosMap.get(curRealLoopIndex);
            if(CollectionUtils.isNotEmpty(loopAdvertInfos)){
                for(int i = 0; i < loopAdvertInfos.size(); i++){
                    //插入位置从1开始，这里需要转换为从0开始
                    AdvertInfo advertInfo = loopAdvertInfos.get(i);
                    lastAdvertInfo = advertInfo;
                    int position = advertInfo.getPosition() - 1;
                    //如果是第一个时需要再次考虑和前面5个位置是否有广告
                    if( i == 0 ){
                        if(advertInfoService.isFitInsertAdvert(insertedWaitShowProducts, advertInfo,insertRequest.getBaseRequest2().getPagePositionId())) {
                            rtList.set(position % GET_PRODUCT_NUM_PER_TIMES, advertInfo.getTotalTemplateInfo());
                        }
                    }else{
                        rtList.set(position % GET_PRODUCT_NUM_PER_TIMES, advertInfo.getTotalTemplateInfo());
                    }
                }
            }

            //返回1个集合，集合大小为10，先将待插入集合中的商品按照插入位置放入该集合中
            List<TotalTemplateInfo> insertNewList = insertByMaxCateAndXyPartition(insertPositionList, rtList, waitInsertProducts,preProducts,userCategoryNum, baseRequest2);
//            List<TotalTemplateInfo> insertNewList = dealWaitInsertPids(insertPositionList, waitInsertProducts, lastSimilarCate3Set);
//            Set<Long> insertedSimilarCate3Set = new HashSet<>();
            //遍历已插入的集合，将非空的商品放入到已插入集合中
            for(int i = 0; i < GET_PRODUCT_NUM_PER_TIMES; i++){
                TotalTemplateInfo totalTemplateInfo = insertNewList.get(i);
                if(totalTemplateInfo == null || CommonConstants.INVALID_PRODUCT_ID.equals(totalTemplateInfo.getId())){
                    continue;
                }
                hasShowedPidSet.add(totalTemplateInfo.getId());
                insertedProducts.add(totalTemplateInfo.getId());
//                insertedSimilarCate3Set.add(getSimilar3CategoryIdByPid(totalTemplateInfo));
            }
            //将待展示的商品放到已插入商品集合中
//            dealWaitShowPids(insertNewList, notShowPidList, lastSimilarCate3Set, insertedSimilarCate3Set, hasShowedPidSet);
            paddingProductByMaxCateAndXyPartition(insertNewList, notShowPidList, preProducts, hasShowedPidSet, userCategoryNum, baseRequest2);
            //遍历本次组装好的10个商品，将其放到返回集合中
            boolean hadNull = false;
            for(TotalTemplateInfo info : insertNewList){
                //说明所有待展示商品都已经填充完成
                if(info == null){
                    hadNull = true;
                    continue;
                }
                //如果前面出现了null元素,则判断能否添加该元素
                if(hadNull && !advertInfoService.isFitAtLast(insertedWaitShowProducts, info,insertRequest.getBaseRequest2().getPagePositionId())){
                    continue;
                }
                insertedWaitShowProducts.add(info);
            }
            //如果未填入全 并且 有活动广告 并且 所有商品已经填充完成，此时需要考虑最后位置能否填入广告
            if(hadNull && CollectionUtils.isNotEmpty(advertInfoList) && CollectionUtils.isEmpty(notShowPidList)){
                int size = insertedWaitShowProducts.size();
                //遍历广告，选择一个广告位置大于insertedWaitShowProducts的长度的广告插入
                for(AdvertInfo advertInfo : advertInfoList){
                    if (advertInfo.getPosition() <= size){
                        continue;
                    }
                    if(advertInfoService.isFitAtLast(insertedWaitShowProducts, advertInfo.getTotalTemplateInfo(),insertRequest.getBaseRequest2().getPagePositionId())){
                        insertedWaitShowProducts.add(advertInfo.getTotalTemplateInfo());
                        break;
                    }
                }
            }
        }

//        //说明插入位置大于商品的个数
//        if(CollectionUtils.isNotEmpty(advertInfoList)
//                && advertInfoService.isFitInsertAdvertAtLast(insertedWaitShowProducts, advertInfoList.get(0))){
//            insertedWaitShowProducts.add(advertInfoList.get(0).getTotalTemplateInfo());
//        }

        //最终返回的商品数量不能超过500
        if(insertedWaitShowProducts.size() > PID_NUM_MAX_LIMIT){
            List<TotalTemplateInfo> totalTemplateInfoList = insertedWaitShowProducts.subList(0, PID_NUM_MAX_LIMIT);
            insertResult.setAllWaitShowProducts(totalTemplateInfoList);
        }else{
            int fillNum = PID_NUM_MAX_LIMIT - insertedWaitShowProducts.size();
            int count = 0;
            for(TotalTemplateInfo tmp : notShowPidList){
                if(count >= fillNum){
                    break;
                }
                if(hasShowedPidSet.contains(tmp.getId())){
                    continue;
                }
                insertedWaitShowProducts.add(tmp);
                count++;
            }
            insertResult.setAllWaitShowProducts(insertedWaitShowProducts);
        }

        insertResult.setInsertedProducts(insertedProducts);

        return insertResult;
    }

    /**
     * 从redis中查询当日已插入的商品信息
     * @return
     */
    public Map<String, Integer> queryTodayInsertProduct(String uuid){
        Map<String, Integer> result = new HashMap<>();
        try {
            List<String> insertPidTimeList = cacheRedisUtil.lrange(CacheRedisKeyConstant.FEED_INSERT_PID_PREFIX + uuid, 0, -1);
            if (CollectionUtils.isEmpty(insertPidTimeList)) {
                return result;
            }
            //获取当日0点毫秒数
            long currentTime = System.currentTimeMillis();
            long zeroTime = currentTime - (currentTime + TimeZone.getDefault().getRawOffset()) % (1000 * 3600 * 24);
            for (String pidTimeStr : insertPidTimeList) {
                String[] pidTimeArray = pidTimeStr.trim().split(":");
                if (pidTimeArray.length != 2) {
                    continue;
                }
                String pid = pidTimeArray[0];
                String time = pidTimeArray[1];
                try {
                    if (Long.valueOf(time) >= zeroTime) {
                        if(result.containsKey(pid)){
                            result.put(pid, result.get(pid)+1);
                        }else{
                            result.put(pid, 1);
                        }
                    }
                } catch (Exception e) {
                    log.error("[一般异常]解析已插入商品信息异常, uuid {} , pidTime {} ", uuid, pidTimeStr, e);
                }
            }
        }catch(Exception e){
            log.error("[严重异常]查询已插入商品信息异常， uuid {} ", uuid, e);
        }
        return result;
    }
    /**
     * 更新已插入的商品信息到redis中
     * @param insertPidList
     * @param uuid
     */
    private void refreshInsertedProduct(List<String> insertPidList, String uuid, ByUser user) {
        if(CollectionUtils.isEmpty(insertPidList)){
            return;
        }

        //如果是测试，则直接返回
        if(user != null && user.isTest()){
            return;
        }

        long now = System.currentTimeMillis();
        List<String> insertPidTimeList = new ArrayList<>();
        for(String pid : insertPidList){
            if(StringUtils.isBlank(pid) || pid.equals("-1")){
                continue;
            }
            insertPidTimeList.add(pid+":"+now);
        }

        if(CollectionUtils.isEmpty(insertPidTimeList)){
            return;
        }

        String[] insertPidArray = insertPidTimeList.toArray(new String[insertPidTimeList.size()]);
        cacheRedisUtil.lpush(CacheRedisKeyConstant.FEED_INSERT_PID_PREFIX + uuid, insertPidArray);
        cacheRedisUtil.ltrim(CacheRedisKeyConstant.FEED_INSERT_PID_PREFIX + uuid, 0, INSERT_PID_NUM_MAX_LIMIT);
        cacheRedisUtil.expire(CacheRedisKeyConstant.FEED_INSERT_PID_PREFIX + uuid, EXPIRE_TIME_3DAY);
    }


    /**
     * 获取横插召回源配置的插入位置
     * @param insertPositionConf
     * @return
     */
    public List<Integer> parseInsertPosition(String insertPositionConf){
        List<Integer> result = new ArrayList<>();
        if(StringUtils.isBlank(insertPositionConf) || MosesExpConst.VALUE_DEFAULT.equals(insertPositionConf)){
            return result;
        }

        String[] positionArray = insertPositionConf.trim().split(",");
        for (String pos : positionArray) {
            try {
                if (StringUtils.isNotBlank(pos)) {
                    int position = Integer.valueOf(pos.trim());
                    //配置的位置只能是1-10
                    if (position <= 10 && position >= 1){
                        result.add(position);
                    }
                }
            }catch (Exception e){
                log.error("[严重异常][实验配置]解析横插位置配置失败， pos {} ", insertPositionConf, e);
            }
        }

        return shuffleLocation(result);
    }

    /**
     * 处理横叉商品
     * @param totalTemplateInfoList
     * @param waitInsertPidInfoList
     * @param baseRequest2
     * @param pageIndexStr
     * @param pageSize
     */
    public List<TotalTemplateInfo> dealInsert(List<TotalTemplateInfo> totalTemplateInfoList, List<TotalTemplateInfo> waitInsertPidInfoList, BaseRequest2 baseRequest2, String pageIndexStr, int pageSize, ByUser user){
        //处理横叉
        String uuid = baseRequest2.getUuid();
        List<TotalTemplateInfo> result = totalTemplateInfoList;
        try {
            if(CollectionUtils.isEmpty(totalTemplateInfoList)){
                return result;
            }

            HashMap<String, String> flags = baseRequest2.getFlags();
            if (CollectionUtils.isNotEmpty(waitInsertPidInfoList)) {
                //获取当日已插入的商品信息
                Map<String, Integer> insertedPidList = queryTodayInsertProduct(uuid);
                if (insertedPidList != null && !insertedPidList.isEmpty()) {
                    //待插入商品集合中过滤掉当日已插入的商品信息
                    Iterator<TotalTemplateInfo> iterator = waitInsertPidInfoList.iterator();
                    while (iterator.hasNext()) {
                        TotalTemplateInfo templateInfo = iterator.next();
                        if (templateInfo == null || templateInfo.getId() == null
                                || insertedPidList.containsKey(templateInfo.getId())) {
                            iterator.remove();
                        }
                    }
                }
            }

            int pageIndex = Integer.valueOf(pageIndexStr);
            List<Integer> insertPositions = parseInsertPosition(flags.get(MosesExpConst.FLAG_INSERT_POSITIONS));

            Map<Long, Integer> userCategoryNum = getUserCategoryNum(baseRequest2.getUuid(),baseRequest2.getUid());
            InsertRequest insertRequest = new InsertRequest();
            insertRequest.setAllWaitShowProducts(totalTemplateInfoList);
            insertRequest.setWaitInsertProducts(waitInsertPidInfoList);
            insertRequest.setPageIndex(pageIndex);
            insertRequest.setPageSize(pageSize);
            insertRequest.setInsertPositionList(insertPositions);
            insertRequest.setUserCategroyNum(userCategoryNum);
            insertRequest.setAdvertInfoList(advertInfoService.getAdvertInfoListByRule(user.getShowAdvert(), user.getUpcUserType(), user.getAdvertInfoList(),user,baseRequest2.getPagePositionId()));
            insertRequest.setBaseRequest2(baseRequest2);

            //将商品插入到指定位置
            InsertResult insertResult = insert2SpecificPosition(insertRequest);

            //获取本次已插入商品
            List<String> currentInsertedPidList = insertResult.getInsertedProducts();
            result = insertResult.getAllWaitShowProducts();
            //将本次已插入的商品放到redis中
            refreshInsertedProduct(currentInsertedPidList, uuid, user);

        }catch (Exception e){
            log.error("[严重异常][隔断规则]处理横叉时发生异常， uuid {} ", uuid, e);
        }
        return result;
    }

    /**
     * 根据深度浏览商品计算同一相似后台三级类目下商品个数
     * @param viewPids
     * @param categoryNumMap
     */
    private void calculateCategoryNumByViewPids(List<String> viewPids, Map<Long, Double> categoryNumMap){
        if(CollectionUtils.isEmpty(viewPids)){
            return;
        }
        long currentTime = System.currentTimeMillis();
        for(String pidTime : viewPids){
            try {
                if (StringUtils.isBlank(pidTime)) {
                    continue;
                }
                String[] pidTimeArray = pidTime.split(":");
                if (pidTimeArray.length != 2
                        || StringUtils.isBlank(pidTimeArray[0])
                        || StringUtils.isBlank(pidTimeArray[1])) {
                    continue;
                }
                String pid = pidTimeArray[0];
                long time = Long.valueOf(pidTimeArray[1]);
                ProductInfo productInfo = productDetailCache.getProductInfo(Long.valueOf(pid));
                if(productInfo == null){
                    continue;
                }
                Long similarCate3Id = similarCategory3IdCache.getSimilarCate3Id(productInfo.getThirdCategoryId());
                if(similarCate3Id == null){
                    continue;
                }
                //深度浏览商品的时间与当前时间的时间差
                long timeDiff = currentTime - time;
                double categoryPidNum = 0;
                if(timeDiff <= ONE_DAY_MS){
                    //24小时内的深度浏览的商品
                    categoryPidNum = 1;
                }else if(timeDiff > ONE_DAY_MS && timeDiff <= THREE_DAY_MS){
                    //24小时到72小时间的深度浏览商品
                    categoryPidNum = 0.5;
                }
                if(categoryNumMap.containsKey(similarCate3Id)){
                    categoryNumMap.put(similarCate3Id, categoryPidNum + categoryNumMap.get(similarCate3Id));
                }else{
                    categoryNumMap.put(similarCate3Id, categoryPidNum);
                }
            }catch (Exception e){
                log.error("[一般异常][隔断规则]根据深度浏览计算相似类目商品个数出现异常 pidTime {} ", pidTime, e);
            }
        }
    }

    /**
     * 根据曝光商品计算同一相似后台三级类目下商品个数
     * @param expPids
     * @param categoryNumMap
     */
    private void calculateCategoryNumByExpPids(List<String> expPids, Map<Long, Double> categoryNumMap){
        if(CollectionUtils.isEmpty(expPids)){
            return;
        }
        long currentTime = System.currentTimeMillis();
        for(String pidTime : expPids){
            try {
                if (StringUtils.isBlank(pidTime)) {
                    continue;
                }
                String[] pidTimeArray = pidTime.split(":");
                if (pidTimeArray.length != 2
                        || StringUtils.isBlank(pidTimeArray[0])
                        || StringUtils.isBlank(pidTimeArray[1])) {
                    continue;
                }
                String pid = pidTimeArray[0];
                long time = Long.valueOf(pidTimeArray[1]);
                ProductInfo productInfo = productDetailCache.getProductInfo(Long.valueOf(pid));
                if(productInfo == null){
                    continue;
                }
                Long similarCate3Id = similarCategory3IdCache.getSimilarCate3Id(productInfo.getThirdCategoryId());
                if(similarCate3Id == null){
                    continue;
                }
                //深度浏览商品的时间与当前时间的时间差
                long timeDiff = currentTime - time;
                double categoryPidNum = 0;
                if(timeDiff <= ONE_DAY_MS){
                    //24小时内的深度浏览的商品
                    categoryPidNum = -0.044;
                }else if(timeDiff > ONE_DAY_MS && timeDiff <= THREE_DAY_MS){
                    //24小时到72小时间的深度浏览商品
                    categoryPidNum = -0.022;
                }
                if(categoryNumMap.containsKey(similarCate3Id)){
                    categoryNumMap.put(similarCate3Id, categoryPidNum + categoryNumMap.get(similarCate3Id));
                }else{
                    categoryNumMap.put(similarCate3Id, categoryPidNum);
                }
            }catch (Exception e){
                log.error("[一般异常][隔断规则]根据深度浏览计算相似类目商品个数出现异常 pidTime {}", pidTime, e);
            }
        }
    }

    /**
     * 根据用户三级类目偏好计算同一相似后台三级类目下商品个数
     * @param level3Hobby
     * @param categoryNumMap
     */
    private void calculateCategoryNumByLevel3Hobby(Map<String, BigDecimal> level3Hobby, Map<Long, Double> categoryNumMap){
        if(level3Hobby == null || level3Hobby.size() == 0){
            return;
        }

        for(Map.Entry<String, BigDecimal> entry : level3Hobby.entrySet()){
            try {
                String cate3Id = entry.getKey();
                BigDecimal score = entry.getValue();
                if(StringUtils.isBlank(cate3Id) || score == null){
                    log.error("[一般异常][隔断规则]解析用户三级类目偏好出现错误 cate3Id {}, score {}", cate3Id, score);
                    continue;
                }
                //过滤类目兴趣分小于5分
                if(score.doubleValue() <= 5){
                    continue;
                }
                Long similarCate3Id = similarCategory3IdCache.getSimilarCate3Id(Long.valueOf(cate3Id));
                if(similarCate3Id == null){
                    continue;
                }
                double categoryPidNum = 1;
                if(categoryNumMap.containsKey(similarCate3Id)){
                    categoryNumMap.put(similarCate3Id, categoryPidNum + categoryNumMap.get(similarCate3Id));
                }else{
                    categoryNumMap.put(similarCate3Id, categoryPidNum);
                }
            }catch (Exception e){
                log.error("[一般异常][隔断规则]根据用户三级类目偏好计算同一相似后台三级类目下商品个数异常 ", e);
            }
        }
    }

    /**
     * 根据用户已购买商品计算同一相似后台三级类目下商品个数
     * @param orderPids
     * @param categoryNumMap
     */
    private void calculateCategoryNumByOrderPids(List<String> orderPids, Map<Long, Double> categoryNumMap){
        if(CollectionUtils.isEmpty(orderPids)){
            return;
        }
        long currentTime = System.currentTimeMillis();
        for(String pidTime : orderPids){
            try{
                if(StringUtils.isBlank(pidTime)){
                    continue;
                }
                String[] pidTimeArray = pidTime.split(":");
                if (pidTimeArray.length != 2
                        || StringUtils.isBlank(pidTimeArray[0])
                        || StringUtils.isBlank(pidTimeArray[1])) {
                    continue;
                }
                String pid = pidTimeArray[0];
                long orderTime = Long.valueOf(pidTimeArray[1]);
                ProductInfo productInfo = productDetailCache.getProductInfo(Long.valueOf(pid));
                if(productInfo == null){
                    continue;
                }
                Long similarCate3Id = similarCategory3IdCache.getSimilarCate3Id(productInfo.getThirdCategoryId());
                if(similarCate3Id == null){
                    continue;
                }

                Long rebuyCycleMs = category3RebuyCycleCache.getRebuyCycleMs(productInfo.getThirdCategoryId());
                if(rebuyCycleMs == null){
                    rebuyCycleMs = THIRTY_DAY_MS;
                }
                long timeDiff = currentTime - orderTime;
                if(timeDiff < rebuyCycleMs){
                    double categoryPidNum = -2;
                    if(categoryNumMap.containsKey(similarCate3Id)){
                        categoryNumMap.put(similarCate3Id, categoryPidNum + categoryNumMap.get(similarCate3Id));
                    }else{
                        categoryNumMap.put(similarCate3Id, categoryPidNum);
                    }
                }
            }catch (Exception e){
                log.error("[一般异常][隔断规则]根据用户已购买商品计算同一相似后台三级类目下商品个数异常, pidTime {}, ", pidTime, e);
            }
        }
    }

    /**
     * 根据用户不感兴趣商品计算同一相似后台三级类目下商品个数
     * @param disinterestPids
     * @param categoryNumMap
     */
    private void calculateCategoryNumByDisinterestPids(Set<Long> disinterestPids, Map<Long, Double> categoryNumMap){
        if(CollectionUtils.isEmpty(disinterestPids)){
            return;
        }

        for(Long pid : disinterestPids){
            try{
                if(pid == null){
                    continue;
                }
                ProductInfo productInfo = productDetailCache.getProductInfo(pid);
                if(productInfo == null){
                    continue;
                }
                Long similarCate3Id = similarCategory3IdCache.getSimilarCate3Id(productInfo.getThirdCategoryId());
                if(similarCate3Id == null){
                    continue;
                }
                double categoryPidNum = -1;
                if(categoryNumMap.containsKey(similarCate3Id)){
                    categoryNumMap.put(similarCate3Id, categoryPidNum + categoryNumMap.get(similarCate3Id));
                }else{
                    categoryNumMap.put(similarCate3Id, categoryPidNum);
                }
            }catch (Exception e){
                log.error("[一般异常][隔断规则]根据用户已购买商品计算同一相似后台三级类目下商品个数, pid {}, ", pid, e);
            }
        }
    }
    /**
     * 获取用户感兴趣、曝光、深度浏览类目以及对应商品数
     * @param uuid
     * @return
     */
    public Map<Long,Integer> getUserCategoryNum(String uuid, Integer uid) {
        String ucUid = null;
        if(uid!=null && uid!=0){
            ucUid = uid.toString();
        }

        //key:相似三级类目 val:最大推荐数
        Map<Long, Integer> rtMap = new HashMap<>();

        try {
            //从uc获取感兴趣类目、深度浏览商品、曝光商品
            List<String> fields = new ArrayList<>();
            fields.add(UserFieldConstants.LEVEL3HOBBY);
            fields.add(UserFieldConstants.EXPPIDS);
            fields.add(UserFieldConstants.VIEWPIDS);
            fields.add(UserFieldConstants.ORDERPIDS);
            fields.add(UserFieldConstants.DISINTERESTPIDS);
            User user = ucRpcService.getData(uuid, ucUid, fields, "moses");
            if(user==null){
                return rtMap;
            }
//            log.info("uuid {}, user {}", uuid, JSON.toJSONString(user));
            Map<Long, Double> categoryPidNumMap = new HashMap<>();
            calculateCategoryNumByLevel3Hobby(user.getLevel3Hobby(), categoryPidNumMap);
//            log.info("Level3Hobby uuid {}, categoryPidNumMap {}", uuid, JSON.toJSONString(categoryPidNumMap));
            calculateCategoryNumByViewPids(user.getViewPids(), categoryPidNumMap);
//            log.info("ViewPids uuid {}, categoryPidNumMap {}", uuid, JSON.toJSONString(categoryPidNumMap));
            calculateCategoryNumByExpPids(user.getExpPids(), categoryPidNumMap);
//            log.info("ExpPids uuid {}, categoryPidNumMap {}", uuid, JSON.toJSONString(categoryPidNumMap));
            calculateCategoryNumByOrderPids(user.getOrderPids(), categoryPidNumMap);
//            log.info("OrderPids uuid {}, categoryPidNumMap {}", uuid, JSON.toJSONString(categoryPidNumMap));
            calculateCategoryNumByDisinterestPids(user.getDisinterestPids(), categoryPidNumMap);
//            log.info("DisinterestPids uuid {}, categoryPidNumMap {}", uuid, JSON.toJSONString(categoryPidNumMap));
            categoryPidNumMap.forEach((similarCate3Id, pidNum)->{
                int pidNumFinally = (int)Math.ceil(pidNum + CATEGORY_NUM_DEFAULT);
                //最大不超过5个，最小为1
                if(pidNumFinally > 5){
                    pidNumFinally = 5;
                }else if(pidNumFinally < 1){
                    pidNumFinally = 1;
                }
                rtMap.put(similarCate3Id, pidNumFinally);
            });
//            log.info("最终结果 uuid {}, categoryPidNumMap {}", uuid, JSON.toJSONString(rtMap));
        } catch (Exception e) {
            log.error("[严重异常][隔断规则]根据用户信息计算同一相似后台三级类目下商品个数出现异常，uuid {}, uid {},",uuid, uid, e);
            return rtMap;
        }

        return rtMap;
    }

    /**
     * 判断商品是否符合最大二级类目、最大三级类目限制,判断能否推出
     * @param otherProductsList 其他商品id(最大19个)
     * @param currentProductId 当前商品id
     * @param categroyAndMaxNumMap 类目、最大推荐数集合
     * @return
     */
    public boolean isFitMaxCategoryProductNum(List<TotalTemplateInfo> otherProductsList,String currentProductId,
                                              Map<Long,Integer> categroyAndMaxNumMap) {
        if (CollectionUtils.isEmpty(otherProductsList)) {
            return true;
        }
        if (StringUtils.isBlank(currentProductId)) {
            return false;
        }
        //其他商品后台相似三级类目集合 key:后台相似三级类目id val:商品数
        Map<Long, Integer> otherSpu3CategroyMap = new HashMap<>();
        //其他商品后台二级类目集合
        Map<Long, Integer> otherSpu2CategroyMap = new HashMap<>();

        for (TotalTemplateInfo tti : otherProductsList) {
            if(tti==null){
                continue;
            }
            ProductInfo productInfo = productDetailCache.getProductInfo(Long.parseLong(tti.getId()));
            if (productInfo == null) {
                continue;
            }
            Long similar3CategoryId = similarCategory3IdCache.getSimilarCate3Id(productInfo.getThirdCategoryId());
            Long secondCategoryId = productInfo.getSecondCategoryId();
            Integer num3Cate = otherSpu3CategroyMap.get(similar3CategoryId);
            Integer num2Cate = otherSpu2CategroyMap.get(secondCategoryId);
            if (num3Cate == null) {
                otherSpu3CategroyMap.put(similar3CategoryId, 1);
            } else {
                num3Cate++;
                otherSpu3CategroyMap.put(similar3CategoryId,num3Cate);
            }

            if (num2Cate == null) {
                otherSpu2CategroyMap.put(secondCategoryId, 1);
            } else {
                num2Cate++;
                otherSpu2CategroyMap.put(secondCategoryId,num2Cate);
            }
        }

        ProductInfo productInfo = productDetailCache.getProductInfo(Long.parseLong(currentProductId));
        if (productInfo == null) {
            return false;
        }
        Long secondCategoryId = productInfo.getSecondCategoryId();
        Long similar3CategoryId = similarCategory3IdCache.getSimilarCate3Id(productInfo.getThirdCategoryId());
        Integer maxNum = 2;
        Integer num = categroyAndMaxNumMap.get(similar3CategoryId);
        if (num != null) {
            maxNum = num;
        }

        Integer otherListCate2Num = otherSpu2CategroyMap.get(secondCategoryId);
        if (otherListCate2Num != null && otherListCate2Num > 6) {
            return false;
        }

        Integer otherListCate3Num = otherSpu3CategroyMap.get(similar3CategoryId);
        if (otherListCate3Num != null && otherListCate3Num >= maxNum) {
            return false;
        }
        return true;
    }

    /**
     * 根据前x后y判断能否岔开
     * @param x 需要与前x个商品进行后台相似3级类目岔开
     * @param y 需要与后y个商品进行后台相似3级类目岔开
     * @param xList 前x个商品集合
     * @param yList 后y个商品集合
     * @param currentProductId 后y个商品集合
     * @return
     */
    public boolean isFitXyPartition(int x, int y, List<TotalTemplateInfo> xList, List<TotalTemplateInfo> yList, String currentProductId) {
        boolean isFit = true;

        //1、获取当前商品后台相似三级类目
        ProductInfo productInfo = productDetailCache.getProductInfo(Long.parseLong(currentProductId));
        if (productInfo == null) {
            return false;
        }
        Long curSimilar3CategoryId = similarCategory3IdCache.getSimilarCate3Id(productInfo.getThirdCategoryId());
        //2、判断当前商品后台相似三级类目与xList yList中商品是否相同
        if(CollectionUtils.isNotEmpty(xList)){
            for (TotalTemplateInfo info: xList) {
                ProductInfo spu = productDetailCache.getProductInfo(Long.parseLong(info.getId()));
                if(spu == null){
                    continue;
                }
                Long xSimilar3CategoryId = similarCategory3IdCache.getSimilarCate3Id(spu.getThirdCategoryId());
                if(xSimilar3CategoryId != null && xSimilar3CategoryId.toString().equals(curSimilar3CategoryId.toString())){
                    isFit = false;
                }
            }
        }
        if(CollectionUtils.isNotEmpty(yList)){
            for (TotalTemplateInfo info: yList) {
                ProductInfo spu = productDetailCache.getProductInfo(Long.parseLong(info.getId()));
                if(spu == null){
                    continue;
                }
                Long ySimilar3CategoryId = similarCategory3IdCache.getSimilarCate3Id(spu.getThirdCategoryId());
                if(ySimilar3CategoryId != null && ySimilar3CategoryId.toString().equals(curSimilar3CategoryId.toString())){
                    isFit = false;
                }
            }
        }
        return isFit;
    }

    /**
     * 对实验配置中的插入位置“1 3 5 7 9”进行双排feeds流当前行数随机，即1随机数为 1或者2，3随机数为3 或者 4，5随机为5 或者6
     * @param configLocationList
     * @return
     */
    public List<Integer> shuffleLocation(List<Integer> configLocationList) {
        if (CollectionUtils.isEmpty(configLocationList)) {
            return configLocationList;
        }

        List<Integer> shuffleList = new ArrayList<>();
        for (Integer location : configLocationList) {

            if(location % 2 ==0){
                //如果配置的是偶数，则将其转化为奇数
                location--;
            }
            //随机一个10以内数字
            int number = random.nextInt(10) + 1;
            if (number < 6) {
                //奇数 1 3 5 7 9，加1
                shuffleList.add(location + 1);
            }else{
                shuffleList.add(location);
            }
        }
//        return configLocationList;
        return shuffleList;
    }

    /**
     * 获取rtList当前位置i前面的x个商品信息
     * @param rtList
     * @param i reList当前的位置
     * @param preProducts
     * @param x  前x个商品
     * @return
     */
    private  List<TotalTemplateInfo> acquireBeforeXProductInfo(List<TotalTemplateInfo> rtList, int i,
                                                               List<TotalTemplateInfo> preProducts, int x){
        List<TotalTemplateInfo> result = new ArrayList<>();
        if(x < i){
            for(int j = i - 1 - x; j < i - 1; j++){
                TotalTemplateInfo totalTemplateInfo = rtList.get(j);
                if(totalTemplateInfo != null){
                    result.add(totalTemplateInfo);
                }
            }
        }else{
            //需要从preProducts截取的个数
            int tmpNum = x - (i - 1);
            int preSize = preProducts.size();
            if(preSize > tmpNum){
                result.addAll(preProducts.subList(preSize - tmpNum, preSize));
            }else{
                result.addAll(preProducts);
            }
            for(int j = 0; j < i - 1; j++){
                TotalTemplateInfo totalTemplateInfo = rtList.get(j);
                if(totalTemplateInfo != null){
                    result.add(totalTemplateInfo);
                }
            }
        }
        return result;
    }

    /**
     * 默认概率获取x或y
     * @param number1
     * @return
     */
    private int acquireRandomXYDefaultProbability(int number1){
        int x;
        if (number1 <= 10) {
            x = 1;
        } else if (number1 > 10 && number1 <= 20) {
            x = 2;
        } else if (number1 > 20 && number1 <= 40) {
            x = 3;
        } else {
            x = 4;
        }
        return x;
    }

    /**
     * 解析概率配置信息
     * @param probability 格式：1,1-10|2,10-20|3,20-40|4,40-100
     * @return
     */
    private List<Map<String, Integer>> parseProbability(String probability){
        List<Map<String, Integer>> result = new ArrayList<>();
        if(StringUtils.isBlank(probability) || ExpFlagsConstants.VALUE_DEFAULT.equals(probability)){
            return result;
        }
        String[] strArray1 = probability.trim().split("\\|");
        for(String str : strArray1){
            try {
                if (StringUtils.isBlank(str)) {
                    continue;
                }
                String[] strArray2 = str.split(",");
                if (strArray2.length != 2) {
                    continue;
                }
                int value = Integer.valueOf(strArray2[0]);
                String[] strArray3 = strArray2[1].split("-");
                if(strArray3.length != 2){
                    continue;
                }
                int min = Integer.valueOf(strArray3[0]);
                int max = Integer.valueOf(strArray3[1]);
                Map<String, Integer> map = new HashMap<>();
                map.put("min", min);
                map.put("max", max);
                map.put("value", value);
                result.add(map);
            }catch (Exception e){
                log.error("[严重异常][实验配置]解析x或y出现异常, 概率配置信息 {} ", probability, e);
            }
        }
        return result;
    }
    /**
     * 随机获取前x或后y
     * @param probabilityList
     * @return
     */
    private int acquireRandomXY(List<Map<String, Integer>> probabilityList){
        int number = random.nextInt(100) + 1;
        if(CollectionUtils.isEmpty(probabilityList)){
            return acquireRandomXYDefaultProbability(number);
        }
        //默认值为4
        int result = 4;
        for(Map<String, Integer> map : probabilityList){
            if(map == null || !map.containsKey("min")
            || !map.containsKey("max") || !map.containsKey("value")){
                continue;
            }
            if(number <= map.get("max") && number > map.get("min")){
                result = map.get("value");
                break;
            }
        }
        return result;
    }
    /**
     * 根据最大相似三级类目限制、最大二级类目限制、前x岔开规则进行插入
     *
     * @param insertPositionList 随机后的插入位置集合
     * @param rtList  用于放置插入后的商品，大小为10
     * @param waitInsertProducts 待插入商品集合
     * @param preProducts        之前的商品集合（最多19个）
     * @param userCategoryMap    用户类目相关数据
     * @return
     */
    public List<TotalTemplateInfo> insertByMaxCateAndXyPartition(List<Integer> insertPositionList, List<TotalTemplateInfo> rtList,List<TotalTemplateInfo> waitInsertProducts,
                                                        List<TotalTemplateInfo> preProducts, Map<Long, Integer> userCategoryMap, BaseRequest2 baseRequest2) {
        //如果待插入商品集合或者待插入的位置集合为空，则不插入商品
        if(CollectionUtils.isEmpty(insertPositionList) || CollectionUtils.isEmpty(waitInsertProducts)){
            return rtList;
        }
        HashMap<String, String> flags = baseRequest2.getFlags();
        List<Map<String, Integer>> beforeXProbabilityList = null;
        if(flags != null){
            String beforeXProbability = flags.get(ExpFlagsConstants.FLAG_BEFOREX_PROBABILITY);
            beforeXProbabilityList = parseProbability(beforeXProbability);
        }
        //遍历每一个位置，进行插入
        for (Integer i : insertPositionList) {
            //如果已经有数据，说明该位置已经插入了活动广告，此时该位置不能插入商品
            if(rtList.get(i -1) != null){
                continue;
            }
            //获取x
            int x = acquireRandomXY(beforeXProbabilityList);
            //获取插入位置的前X个商品信息
            List<TotalTemplateInfo> xList = acquireBeforeXProductInfo(rtList, i, preProducts, x);
            //获取插入位置的前19个商品信息
            List<TotalTemplateInfo> productBeforeInsertPos = acquireBeforeXProductInfo(rtList, i, preProducts, BEFORE_CURRENT_PRODUCT_NUM);

            //遍历循环，找到待插入集合中满足条件的元素的索引
            int fitAllIndex = 0;
            boolean fitAllFlag = false;
            int fitMaxCategoryProductNumIndex = 0;
            boolean fitMaxCategoryProductNumFlag = false;
            int fitXyPartitionIndex = 0;
            boolean fitXyPartitionFlag = false;
            int index = 0;
            for(TotalTemplateInfo info : waitInsertProducts){
                //判断是否符合最大类目限制规则
                boolean isFitMaxCategoryProductNum = isFitMaxCategoryProductNum(productBeforeInsertPos, info.getId(),
                        userCategoryMap);

                //获取当前位置前x个商品赋值到xList中
                //判断是否符合前x岔开
                boolean isFitXyPartition = isFitXyPartition(x, 0, xList, null, info.getId());

                if (isFitMaxCategoryProductNum && isFitXyPartition) {
                    fitAllFlag = true;
                    fitAllIndex = index;
                    break;
                }
                else if (isFitMaxCategoryProductNum && !fitMaxCategoryProductNumFlag){
                    fitMaxCategoryProductNumFlag = true;
                    fitMaxCategoryProductNumIndex = index;
                }
                else if (isFitXyPartition && !fitXyPartitionFlag){
                    fitXyPartitionFlag = true;
                    fitXyPartitionIndex = index;
                }
                index++;
            }
            //如果没有符合两个规则都满足的商品，优先插入满足isFitMaxCategoryProductNum的商品
            //如果没有符合两个规则都满足的商品，也没有满足isFitMaxCategoryProductNum的商品，插入满足isFitXyPartition的商品
            int fitIndex = 0;
            if(fitAllFlag){
                fitIndex = fitAllIndex;
            }else if(fitMaxCategoryProductNumFlag){
                fitIndex = fitMaxCategoryProductNumIndex;
            }else if(fitXyPartitionFlag){
                fitIndex = fitXyPartitionIndex;
            }
            rtList.set(i - 1,waitInsertProducts.get(fitIndex));
            waitInsertProducts.remove(fitIndex);
            //如果待插入的商品集合为空，则直接返回
            if (CollectionUtils.isEmpty(waitInsertProducts)) {
                return rtList;
            }
        }
        return rtList;
    }

    /**
     * 根据最大类目限制规则、前x后y规则进行填充
     * @param insertNewList 10个待填充的槽（槽中可能存在已插入商品）
     * @param notShowPidList 为展示的候选集商品
     * @param preProducts 之前的商品（最多20个）
     * @param hasShowedPidSet 已经展示的商品（用于去重）
     */
    public void  paddingProductByMaxCateAndXyPartition(List<TotalTemplateInfo> insertNewList,List<TotalTemplateInfo> notShowPidList,List<TotalTemplateInfo> preProducts
            ,Set<String>hasShowedPidSet,Map<Long, Integer> userCategoryMap, BaseRequest2 baseRequest2){

        int insertSize = insertNewList.size();
        HashMap<String, String> flags = baseRequest2.getFlags();
        List<Map<String, Integer>> beforeXProbabilityList = null;
        List<Map<String, Integer>> afterYProbabilityList = null;
        if(flags != null){
            String beforeXProbability = flags.get(ExpFlagsConstants.FLAG_BEFOREX_PROBABILITY);
            beforeXProbabilityList = parseProbability(beforeXProbability);
            String afterYProbability = flags.get(ExpFlagsConstants.FLAG_AFTERY_PROBABILITY);
            afterYProbabilityList = parseProbability(afterYProbability);
        }
        //展示的候选商品集中过滤掉已展示的商品
        Iterator<TotalTemplateInfo> iterator = notShowPidList.iterator();
        while (iterator.hasNext()){
            TotalTemplateInfo templateInfo = iterator.next();
            if(templateInfo == null || templateInfo.getId() == null
                || hasShowedPidSet.contains(templateInfo.getId())){
                iterator.remove();
            }
        }
        for (int i = 0; i < insertSize; i++) {
            TotalTemplateInfo info = insertNewList.get(i);
            //又插入的商品 跳过
            if(info!=null){
                continue;
            }

            //随机获取x y
            int x = acquireRandomXY(beforeXProbabilityList);
            int y = acquireRandomXY(afterYProbabilityList);

            //获取插入位置的前X个商品信息
            List<TotalTemplateInfo> xList = acquireBeforeXProductInfo(insertNewList, i+1, preProducts, x);
            //获取插入位置的前19个商品信息
            List<TotalTemplateInfo> productBeforeInsertPos = acquireBeforeXProductInfo(insertNewList, i+1, preProducts, BEFORE_CURRENT_PRODUCT_NUM);
            //再加上后9个
            for(int j = i+1; j < insertSize; j++){
                TotalTemplateInfo tmp = insertNewList.get(j);
                if(tmp != null) {
                    productBeforeInsertPos.add(tmp);
                }
            }

            List<TotalTemplateInfo> yList = new ArrayList<>();
            for(int j = i + 1; j < Math.min(i + 1 + y, insertSize); j++){
                TotalTemplateInfo tmp = insertNewList.get(j);
                if(tmp != null) {
                    yList.add(tmp);
                }
            }

//            log.error("前x商品如下： i {}", i);
//            for(TotalTemplateInfo templateInfo : xList){
//                log.error("pid {}, cateId {}", templateInfo.getId(), getSimilar3CategoryIdByPid(templateInfo));
//            }
//
//            log.error("后y商品如下： i {}", i);
//            for(TotalTemplateInfo templateInfo : yList){
//                log.error("pid {}, cateId {}", templateInfo.getId(), getSimilar3CategoryIdByPid(templateInfo));
//            }
//
//            log.error("前19个+后9个商品如下： i {}", i);
//            for(TotalTemplateInfo templateInfo : preProducts){
//                log.error("pid {}, cateId {}", templateInfo.getId(), getSimilar3CategoryIdByPid(templateInfo));
//            }


            //遍历循环，找到待插入集合中满足条件的元素的索引
            int fitAllIndex = 0;
            boolean fitAllFlag = false;
            int fitMaxCategoryProductNumIndex = 0;
            boolean fitMaxCategoryProductNumFlag = false;
            int fitXyPartitionIndex = 0;
            boolean fitXyPartitionFlag = false;
            int index = 0;
            for (TotalTemplateInfo totalInfo : notShowPidList) {
                //判断是否符合最大类目限制规则
                boolean isFitMaxCategoryProductNum = isFitMaxCategoryProductNum(productBeforeInsertPos, totalInfo.getId(),
                        userCategoryMap);
                //判断是否符合前x岔开
                boolean isFitXyPartition = isFitXyPartition(x, y, xList, yList, totalInfo.getId());

                if (isFitMaxCategoryProductNum && isFitXyPartition) {
                    fitAllFlag = true;
                    fitAllIndex = index;
                    break;
                }
                else if (isFitMaxCategoryProductNum && !fitMaxCategoryProductNumFlag){
                    fitMaxCategoryProductNumFlag = true;
                    fitMaxCategoryProductNumIndex = index;
                }
                else if (isFitXyPartition && !fitXyPartitionFlag){
                    fitXyPartitionFlag = true;
                    fitXyPartitionIndex = index;
                }
                index++;
            }
            //如果没有符合两个规则都满足的商品，优先插入满足isFitMaxCategoryProductNum的商品
            //如果没有符合两个规则都满足的商品，也没有满足isFitMaxCategoryProductNum的商品，插入满足isFitXyPartition的商品
            int fitIndex = 0;
            if(fitAllFlag){
                fitIndex = fitAllIndex;
            }else if(fitMaxCategoryProductNumFlag){
                fitIndex = fitMaxCategoryProductNumIndex;
            }else if(fitXyPartitionFlag){
                fitIndex = fitXyPartitionIndex;
            }
            TotalTemplateInfo fitInfo = notShowPidList.get(fitIndex);
//            log.error("fitInfo： i {}, pid {}, cate3Id {}", i, fitInfo.getId(), getSimilar3CategoryIdByPid(fitInfo));
            insertNewList.set(i,fitInfo);
            hasShowedPidSet.add(fitInfo.getId());
            notShowPidList.remove(fitIndex);

            //如果未展示的商品集合为空，则直接返回
            if (CollectionUtils.isEmpty(notShowPidList)) {
                return;
            }
        }
    }

    /**
     * 过滤掉非用户季节商品，过滤规则如下：
     * 如果用户季节为空 则不过滤
     * 如果用户季节不为空，但商品季节为空，则过滤
     * 如果用户季节春（1），商品适用季节包含春或秋或四季，则不过滤
     * 如果用户季节秋（4），商品适用季节包含春或秋或四季，则不过滤
     * 如果用户季节夏（2），商品适用季节包含夏或四季，则不过滤
     * 如果用户季节冬（8），商品适用季节包含冬或四季，则不过滤
     * @param productSeasonValue 商品季节对应的值
     * @param userSeasonValue 用户季节对应的值
     * @return ture 表示不满足用户季节，false 表示满足用户季节
     */
    public  boolean isFilterByUserSeason(int productSeasonValue, int userSeasonValue){
        //用户季节为空，则不需要过滤
        if (userSeasonValue == 0) {
            return false;
        }
        //如果用户季节不为空，但商品季节为空，则需要过滤
        if(productSeasonValue == 0)
        {
            return true;
        }
        //用户季节和商品季节无交集，则需要过滤
        if ((productSeasonValue & userSeasonValue) == 0) {
            return true;
        }
        return false;
    }

    public  int convertSeason2int(String season){
        int seasonValue = 0;
        if(StringUtils.isBlank(season)){
            return seasonValue;
        }
        //转化成season对应的id，如果季节为春或秋时，对应的季节ID为春ID+秋ID
        if (SeasonEnum.SPRING.getName().equals(season)) {
            seasonValue = SeasonEnum.SPRING.getId() + SeasonEnum.AUTUMN.getId();
        } else if (SeasonEnum.SUMMER.getName().equals(season)) {
            seasonValue = SeasonEnum.SUMMER.getId();
        } else if (SeasonEnum.AUTUMN.getName().equals(season)) {
            seasonValue = SeasonEnum.AUTUMN.getId() + SeasonEnum.SPRING.getId();
        } else if (SeasonEnum.WINTER.getName().equals(season)) {
            seasonValue = SeasonEnum.WINTER.getId();
        }
        return seasonValue;
    }

    /**
     * 获取用户个性化推荐设置开关状态
     * @param uid
     * @return
     */
    public boolean getPersonalizedRecommendSwitch(String uid) {
        if("0".equals(uid)){
            return true;
        }
        boolean result = true;
        try {
            result = pushTokenService.getPersonalizedRecommendSwitch(Long.valueOf(uid), "moses.biyao.com");
        }catch (Exception e){
            log.error("[严重异常]获取用户个性化推荐设置开关状态异常， uid {}, e ",uid, e);
        }
        return result;
    }



    /**
     * 获取用户类型
     * @param uuid
     * @return
     */
    public Integer getUpcUserType(String uuid,String uid) {
        Integer result = UPCUserTypeConstants.NEW_VISITOR; // 默认新访客
        if (StringUtils.isBlank(uuid)){
            return result;
        }
        try {
            VisitorInfoParam vi = new VisitorInfoParam();
            vi.setCallSysName("moses.biyao.com");

            if(StringUtils.isNotBlank(uid) && Long.parseLong(uid) > 0){
                vi.setCustomerId(Long.parseLong(uid));
            }else {
                vi.setCustomerId(null);
            }
            vi.setUuid(uuid);
            IBusinessTocDubboService businessTocDubboService = ApplicationContextProvider.getBean(IBusinessTocDubboService.class);
            com.biyao.bsrd.common.client.model.result.Result<VisitorInfoDTO> visitorInfo =
                    businessTocDubboService.getVisitorInfo(vi);
            if(visitorInfo != null && visitorInfo.getObj() != null ){
                if (!visitorInfo.getObj().isMatch()){ // 老客
                    result = UPCUserTypeConstants.CUSTOMER;
                }else if (visitorInfo.getObj().getVisitorType() == 1){ // 老访客
                    result = UPCUserTypeConstants.OLD_VISITOR;
                }
            }
        } catch (Exception e) {
            log.error("调用upc接口查询用户身份出错",e);
        }

        return result;
    }
}
