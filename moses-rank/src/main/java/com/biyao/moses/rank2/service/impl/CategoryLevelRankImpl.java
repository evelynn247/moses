package com.biyao.moses.rank2.service.impl;

import com.biyao.moses.cache.ProductDetailCache;
import com.biyao.moses.cache.ProductSeasonCache;
import com.biyao.moses.common.constant.*;
import com.biyao.moses.model.match2.MatchItem2;
import com.biyao.moses.model.rank2.RankItem2;
import com.biyao.moses.params.ProductInfo;
import com.biyao.moses.params.ProductScoreInfo;
import com.biyao.moses.params.rank2.RankRequest2;
import com.biyao.moses.rank2.service.Rank2;
import com.biyao.moses.rpc.UcRpcService;
import com.biyao.moses.util.*;
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
 * 类目页分层排序
 */
@Slf4j
@Component(value = RankNameConstants.CATEGORY_LEVEL_RANK)
public class CategoryLevelRankImpl implements Rank2 {

    @Autowired
    private ProductDetailCache productDetailCache;
    @Autowired
    private UcRpcService ucRpcService;
    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private MatchRedisUtil matchRedisUtil;
    @Autowired
    private CacheRedisUtil cacheRedisUtil;
    @Autowired
    private ProductSeasonCache productSeasonCache;
    // 60*60*24*1000*3 3天
    private final int days3ByMs = 259200000;

    @Override
    public List<RankItem2> rank(RankRequest2 rankRequest2) {
        List<RankItem2> resultList = new ArrayList<>();
        List<MatchItem2> matchItemList = rankRequest2.getMatchItemList();

        try {
            //一、按商品7日销量倒排
            Collections.sort(matchItemList, new Comparator<MatchItem2>() {
                @Override
                public int compare(MatchItem2 o1, MatchItem2 o2) {
                    return productDetailCache.getProductInfo(o2.getProductId()).getSalesVolume7()
                            .compareTo(productDetailCache.getProductInfo(o1.getProductId()).getSalesVolume7());
                }
            });

            //二、数据准备
            Integer upcUserType = rankRequest2.getUpcUserType();
            String uuid = rankRequest2.getUuid();
            Integer uid = rankRequest2.getUid();
            Integer userSex = rankRequest2.getUserSex();
            String ucUid = null;
            if (uid != null && uid > 0) {
                ucUid = uid.toString();
            }
            List<String> fields = new ArrayList<>();
            fields.add(UserFieldConstants.LEVEL3HOBBY);
            fields.add(UserFieldConstants.SEASON);
            User ucUser = ucRpcService.getData(uuid, ucUid, fields, "mosesrank");
            //用户上次登录时间（天）
            String lastLoginTime = redisUtil.getString(RedisKeyConstant.LAST_LOGIN_TIME + uid);
            long currentTime = System.currentTimeMillis();
            //获取ibcf数据
            String rbcfRedisKey = MatchRedisKeyConstant.MOSES_IBCF_PREFIX + uuid;
            List<ProductScoreInfo> ibcfProducts = getIbcfProducts(uuid, rbcfRedisKey, 200);
            //获取rtcbr数据
            String rtcbrRedisKey = MatchRedisKeyConstant.MOSES_RTCBR_PREFIX + uuid;
            List<ProductScoreInfo> rtcbrProducts = getIbcfProducts(uuid, rtcbrRedisKey, 100);
            //获取用户曝光数据
            //获取曝光商品 格式pid:time
            List<String> expPids = cacheRedisUtil.lrange(CacheRedisKeyConstant.NEW_HOME_FEED_EXPOSURE + uuid, 0, -1);
            //曝光次数集合 key:pid val:曝光次数
            Map<Long, Integer> expMap = new HashMap<>();
            dealWithExpNum(expPids, expMap);

            LinkedList<ProductInfo> level0List = new LinkedList<>(); //新品层
            LinkedList<ProductInfo> level1List = new LinkedList<>(); //ibcf rtcbr层
            LinkedList<ProductInfo> level2List = new LinkedList<>(); //用户三级类目偏好层
            LinkedList<ProductInfo> level3List = new LinkedList<>(); //符合用户性别、商品季节层
            LinkedList<ProductInfo> level4List = new LinkedList<>(); //其他层

            //三、数据分层
            divertLevelByProducts(matchItemList, upcUserType, userSex, ucUser, lastLoginTime,
                    currentTime, ibcfProducts, rtcbrProducts, level0List, level1List, level2List, level3List, level4List);

            //四、去除空层
            List<LinkedList<ProductInfo>> allLevelList = new ArrayList<>();
            dealWithEmptyLevelList(level0List, level1List, level2List, level3List, level4List, allLevelList);

            //五、处理曝光
            dealAllLevelListByExpNum(expMap, allLevelList);

            //六、组装数据
            dealWithResult(resultList, allLevelList);
        } catch (Exception e) {
            log.error("[严重异常]clr排序异常  {}", e);
            //组装结果返回
            List<RankItem2> rankItem2List = RankUtils.convert(matchItemList);
            return rankItem2List;
        }

        return resultList;
    }

    /**
     * 组装最后返回结果
     *
     * @param resultList
     * @param allLevelList
     */
    private void dealWithResult(List<RankItem2> resultList, List<LinkedList<ProductInfo>> allLevelList) {
        for (LinkedList<ProductInfo> list : allLevelList) {
            Iterator<ProductInfo> iterator = list.iterator();
            while (iterator.hasNext()) {
                ProductInfo next = iterator.next();
                RankItem2 item2 = new RankItem2();
                item2.setProductId(next.getProductId());
                resultList.add(item2);
            }
        }
    }

    /**
     * 对商品进行曝光惩罚处理
     *
     * @param expMap
     * @param allLevelList
     */
    private void dealAllLevelListByExpNum(Map<Long, Integer> expMap, List<LinkedList<ProductInfo>> allLevelList) {
        //需要进行后置添加的集合
        Map<Long,Integer> addLastExpProductsMap = new LinkedHashMap<>();

        for (int i = 0; i < allLevelList.size(); i++) {
            LinkedList<ProductInfo> productList = allLevelList.get(i);
            Iterator<ProductInfo> iterator = productList.iterator();
            while (iterator.hasNext()) {
                ProductInfo product = iterator.next();
                Integer expNum = expMap.getOrDefault(product.getProductId(), 0);
                //无曝光
                if (expNum == 0) {
                    continue;
                } else {
                    //计算惩罚到哪一层 惩罚层数 = 当前层数+曝光次数-1
                    int levelNum = ((i + 1) + expNum - 1) > allLevelList.size() ? allLevelList.size() : (i + 1) + expNum - 1;
                    //把商品加入对应的惩罚层的最后一个，并在当前层删除该商品
                    iterator.remove();
//                    allLevelList.get(levelNum - 1).addLast(product);
                    addLastExpProductsMap.put(product.getProductId(),levelNum - 1);
                }
            }
        }

        Iterator<Map.Entry<Long, Integer>> iterator = addLastExpProductsMap.entrySet().iterator();
        while (iterator.hasNext()){
            Map.Entry<Long, Integer> next = iterator.next();
            allLevelList.get(next.getValue()).addLast(productDetailCache.getProductInfo(next.getKey()));
        }
    }

    /**
     * 计算曝光池中商品曝光次数
     *
     * @param expPids
     * @param expMap
     * @return
     */
    private Map<Long, Integer> dealWithExpNum(List<String> expPids, Map<Long, Integer> expMap) {
        if (CollectionUtils.isEmpty(expPids)) {
            return expMap;
        }

        for (String str : expPids) {
            if (StringUtils.isBlank(str)) {
                continue;
            }
            try {
                String[] pidTimeStr = str.split(":");
                long pid = Long.parseLong(pidTimeStr[0]);
                Integer expNum = expMap.get(pid);
                if (expNum == null) {
                    expMap.put(pid, 1);
                } else {
                    expNum = expNum + 1;
                    expMap.put(pid, expNum);
                }
            } catch (Exception e) {
                log.error("[严重异常]使用UC商品曝光数据统计各个时段商品曝光数量出现数字异常  {}", e);
                continue;
            }
        }
        return expMap;
    }

    /**
     * 对商品集合分层
     *
     * @param matchItemList
     * @param upcUserType
     * @param userSex
     * @param ucUser
     * @param lastLoginTime
     * @param currentTime
     * @param ibcfProducts
     * @param rtcbrProducts
     * @param level0List
     * @param level1List
     * @param level2List
     * @param level3List
     * @param level4List
     */
    private void divertLevelByProducts(List<MatchItem2> matchItemList, Integer upcUserType, Integer userSex,
                                       User ucUser, String lastLoginTime, long currentTime,
                                       List<ProductScoreInfo> ibcfProducts, List<ProductScoreInfo> rtcbrProducts,
                                       LinkedList<ProductInfo> level0List, LinkedList<ProductInfo> level1List,
                                       LinkedList<ProductInfo> level2List, LinkedList<ProductInfo> level3List,
                                       LinkedList<ProductInfo> level4List) {
        for (MatchItem2 item2 : matchItemList) {
            ProductInfo productInfo = productDetailCache.getProductInfo(item2.getProductId());
            //用户为老客时加入level0
            //条件：1用户为老客、2商品为新品、3商品上新时间>用户上次登录时间
            try {
                if (hasNewProduct(upcUserType, lastLoginTime, currentTime, productInfo)) {
                    level0List.add(productInfo);
                    continue;
                }

                //商品为ibcf、rtcbr时加入level1
                if (hasIbcfOrRtcbr(ibcfProducts, rtcbrProducts, item2)) {
                    level1List.add(productInfo);
                    continue;
                }

                //商品为用户偏好类目时加入level2
                if (ucUser != null && ucUser.getLevel3Hobby() != null) {
                    Map<String, BigDecimal> level3Hobby = ucUser.getLevel3Hobby();
                    if (hasHobbyCategoryProduct(level3Hobby, productInfo)) {
                        level2List.add(productInfo);
                        continue;
                    }
                }

                boolean sessonFlag = true;
                if (ucUser != null ) {
                    int seasonNum = RankUtil.convertSeason2int(ucUser.getSeason());
                    if(RankUtil.isFilterByUserSeason(productSeasonCache.getProductSeasonValue
                            (productInfo.getProductId().toString()), seasonNum)){
                        sessonFlag = false;
                    }
                }
                boolean sexFlag = true;
                if (RankUtil.isFilterByUserSex(productInfo, userSex)) {
                    sexFlag = false;
                }
                //商品为适用用户性别和用户当前感知季节类目下商品 加入level3
                if (sessonFlag && sexFlag) {
                    level3List.add(productInfo);
                    continue;
                }
            } catch (Exception e) {
                log.error("[严重异常]clr排序对商品分层时出现异常 {}", e);
            }
            //其他情况 加入level4
            level4List.add(productInfo);
        }
        if(level0List.size()>0){
            level0List.sort(Comparator.comparing(ProductInfo::getFirstOnshelfTime).reversed());
        }

    }

    /**
     * 去除空层
     *
     * @param level0List
     * @param level1List
     * @param level2List
     * @param level3List
     * @param level4List
     * @param allLevelList
     */
    private void dealWithEmptyLevelList(LinkedList<ProductInfo> level0List, LinkedList<ProductInfo> level1List, LinkedList<ProductInfo> level2List, LinkedList<ProductInfo> level3List, LinkedList<ProductInfo> level4List, List<LinkedList<ProductInfo>> allLevelList) {
        if (!level0List.isEmpty()) {
            allLevelList.add(level0List);
        }
        if (!level1List.isEmpty()) {
            allLevelList.add(level1List);
        }
        if (!level2List.isEmpty()) {
            allLevelList.add(level2List);
        }
        if (!level3List.isEmpty()) {
            allLevelList.add(level3List);
        }
        if (!level4List.isEmpty()) {
            allLevelList.add(level4List);
        }
    }

    /**
     * 判断商品是否为用户偏好类目商品
     *
     * @param level3Hobby
     * @param productInfo
     * @return
     */
    private boolean hasHobbyCategoryProduct(Map<String, BigDecimal> level3Hobby, ProductInfo productInfo) {
        Iterator<Map.Entry<String, BigDecimal>> iterator = level3Hobby.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, BigDecimal> map = iterator.next();
            String thirdCategory = map.getKey();
            if(StringUtils.isBlank(thirdCategory)){
                continue;
            }
            if (thirdCategory.equals(productInfo.getThirdCategoryId().toString())) {
                return true;
            }
        }

        return false;
    }

    /**
     * 判断是否为ibcf或rtcbr商品
     *
     * @param ibcfProducts
     * @param rtcbrProducts
     * @param item2
     * @return
     */
    private boolean hasIbcfOrRtcbr(List<ProductScoreInfo> ibcfProducts, List<ProductScoreInfo> rtcbrProducts
            , MatchItem2 item2) {
        for (ProductScoreInfo ibcfProduct : ibcfProducts) {
            if (ibcfProduct.getProductId().equals(item2.getProductId())) {
                return true;
            }
        }
        for (ProductScoreInfo rtcbrProduct : rtcbrProducts) {
            if (rtcbrProduct.getProductId().equals(item2.getProductId())) {
                return true;
            }
        }

        return false;
    }

    /**
     * 获取ibcf数据
     *
     * @param uuid
     * @return
     */
    private List<ProductScoreInfo> getIbcfProducts(String uuid, String key, int MaxNum) {
        String str = matchRedisUtil.getString(key);
        List<ProductScoreInfo> products = new ArrayList<>();
        if (StringUtils.isNotBlank(str)) {
            products = StringUtil.parseProductStr(str, MaxNum, "mosesrank");
        }
        return products;
    }

    /**
     * 判断是否加入新品层
     *
     * @param upcUserType
     * @param lastLoginTime
     * @param currentTime
     * @param productInfo
     * @return
     */
    private boolean hasNewProduct(Integer upcUserType, String lastLoginTime, long currentTime, ProductInfo productInfo) {
        return upcUserType != null && upcUserType == UPCUserTypeConstants.CUSTOMER &&
                currentTime - productInfo.getFirstOnshelfTime().getTime() <= days3ByMs &&
                StringUtils.isNotBlank(lastLoginTime) &&
                productInfo.getFirstOnshelfTime().getTime() > Long.parseLong(lastLoginTime);
    }
}
