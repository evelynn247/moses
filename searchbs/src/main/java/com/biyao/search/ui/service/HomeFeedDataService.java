package com.biyao.search.ui.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.biyao.search.common.model.CommonRequestParam;
import com.biyao.search.ui.cache.RedisDataCache;
import com.biyao.search.ui.cache.SimilarProductCache;
import com.biyao.search.ui.constant.ColorCodeConsts;
import com.biyao.search.ui.constant.CommonConstant;
import com.biyao.search.ui.remote.common.PageAndCacheService;
import com.biyao.search.ui.remote.common.ProductDetailService;
import com.biyao.search.ui.remote.request.UISearchRequest;
import com.biyao.search.ui.remote.response.BlockData;
import com.biyao.search.ui.remote.response.SearchProduct;
import com.biyao.search.ui.remote.response.SearchTitle;
import com.biyao.search.ui.remote.response.TitleText;
import com.biyao.search.ui.util.AppNumVersionUtil;
import com.biyao.search.ui.util.HttpClientUtil;
import com.biyao.search.ui.util.IdCalculateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @author zhaiweixi@idstaff.com
 * @date 2019/7/23
 **/
@Component
@Slf4j
public class HomeFeedDataService {

    @Autowired
    ProductDetailService productDetailService;

    @Autowired
    PageAndCacheService pageAndCacheService;

    @Autowired
    SimilarProductCache similarProductCache;

    @Autowired
    RedisDataCache redisDataCache;

    /**
     * 获取首页feed流模块
     * @param request
     * @param commonParam
     * @param sb
     * @return
     */
    public BlockData getHomeFeedData(UISearchRequest request, CommonRequestParam commonParam, StringBuilder sb,List<Long> searchResultPids) {

        if(AppNumVersionUtil.isAfterVideoVersion(request)){
            //判断当前版本如果是内容策略版本之后，则屏蔽猜你喜欢数据源
            return null;
        }
        BlockData blockData = new BlockData();
        List<SearchProduct> searchProductList = new ArrayList<>();
        try{
            //拼接请求url
            String url = String.format(CommonConstant.MOSES_PRODUCT_FEED_URL,request.getUuid(),request.getUid()==null?"":request.getUid(),"guessYouLike","search",100);
            String resultStr = HttpClientUtil.sendPostJSON(url, null, "", 500);
            JSONObject resultJson = JSONObject.parseObject(resultStr);
            if (resultJson.getInteger("success") == 0){
                JSONObject dataObject = resultJson.getJSONObject("data");
                if(dataObject !=null){
                    JSONArray array = dataObject.getJSONArray("pids");
                    List<Long> feedPids = new ArrayList<>();
                    if (array != null && array.size() > 0){
                        array.stream().forEach(item -> {
                            feedPids.add(Long.valueOf(item.toString()));
                        });
                        //猜你喜欢头部插入找相似商品 20200924
                        searchProductList = insertSimilarProduct(feedPids,request,searchResultPids);
                        String aid = IdCalculateUtil.createAid(request.getAid() + IdCalculateUtil.createBlockId());
                        String blockId = IdCalculateUtil.createBlockId();
                        productDetailService.detailProduct(searchProductList, commonParam, blockId, request, "searchResult_feeds",
                                CommonConstant.TemplateType.DOUBLE_PRODUCT, aid);

                        blockData = pageAndCacheService.cacheAndGetFirstPageProducts(blockId, request,
                                searchProductList, commonParam, CommonConstant.TemplateType.DOUBLE_PRODUCT, "");

                        List<SearchTitle> title = new ArrayList<>();

                        SearchTitle searchTitle = new SearchTitle();

                        List<TitleText> oneLine = new ArrayList<>();
                        oneLine.add(new TitleText("猜你喜欢", ColorCodeConsts.TITLE_COLOR_BLACK_333333, 1));

                        searchTitle.setContents(oneLine);
                        searchTitle.setPicType(1);
                        title.add(searchTitle);
                        blockData.setTitle(title);

                        JSONArray productsLog = new JSONArray();
                        int position = 0;
                        for (SearchProduct product : searchProductList) {
                            JSONObject item = new JSONObject();
                            item.put("pos", position++);
                            item.put("suid", product.getSuId());
                            productsLog.add(item);
                        }

                        JSONObject dcLogBody = new JSONObject();
                        dcLogBody.put("blockId", blockId);
                        dcLogBody.put("aid", aid);
                        dcLogBody.put("result", productsLog);
                        sb.append("\ttopic0=").append(JSON.toJSONString(dcLogBody));
                    }
                }
            }
        }catch (Exception e){
            log.error("[严重异常]从mosesmatch获取feed流商品失败：", e);
        }
        return blockData;
    }

    /**
     * 猜你喜欢头部插入找相似商品
     * @param feedPids
     * @param request
     * @param searchResultPids
     * @return
     */
    private List<SearchProduct> insertSimilarProduct(List<Long> feedPids, UISearchRequest request, List<Long> searchResultPids) {
        List<Integer> resultPids = new ArrayList<>();

        //开关控制
        try{
            if(redisDataCache.getSimilarProductFlag()){
                List<String> temp = similarProductCache.getSimilarProduct(request.getQuery());
                if(temp !=null && temp.size() > 0){
                    for (String pid : temp) {
                        //数量控制，最多100个商品
                        if(resultPids.size() > 100){
                            break;
                        }
                        //重复过滤
                        if(feedPids!= null){
                            if(feedPids.contains(Long.valueOf(pid))){
                                continue;
                            }
                        }
                        if(searchResultPids!= null){
                            if(searchResultPids.contains(Long.valueOf(pid))){
                                continue;
                            }
                        }
                        resultPids.add(Integer.valueOf(pid));
                    }
                }
            }
        }catch(Exception e){
            log.error("[严重异常]猜你喜欢模块头部插入找相似商品异常：", e);
        }

        if (feedPids != null) {
            feedPids.forEach(item->{
                resultPids.add(item.intValue());
            });
        }
        return setSearchProduct(resultPids);
    }

    /**
     * 组装返回结果
     * @param resultPids
     * @return
     */
    private List<SearchProduct> setSearchProduct(List<Integer> resultPids) {
        List<SearchProduct> result = new ArrayList<>();
        for (Integer pid:resultPids) {
            SearchProduct searchProduct = new SearchProduct();
            searchProduct.setProductId(pid);
            searchProduct.setSemStr("cnxh");
            result.add(searchProduct);
        }
        return result;
    }
}
