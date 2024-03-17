package com.biyao.search.ui.remote.common;

import com.biyao.search.common.enums.PlatformEnum;
import com.biyao.search.common.model.CommonRequestParam;
import com.biyao.search.ui.constant.ColorCodeConsts;
import com.biyao.search.ui.constant.CommonConstant;
import com.biyao.search.ui.remote.request.UISearchPageRequest;
import com.biyao.search.ui.remote.request.UISearchRequest;
import com.biyao.search.ui.remote.response.*;
import com.biyao.search.ui.util.IdCalculateUtil;
import com.biyao.search.ui.util.RedisUtil;
import com.biyao.search.ui.util.RouterUtil;
import com.biyao.search.ui.util.TrackParamUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

@Service
public class PageAndCacheService {

    @Autowired
    private RedisUtil redisUtil;
    private static final Integer firstPageSize = 10; // 每个区块第一页的模板个数
    private static final Integer normalPageSize = 20; // 每个区块普通页的模板个数
    private static final int SEARCH_CACHE_TIME = 5 * 60; // 30分钟
    private static final String BLOCK_CACHE_PREFIX = "search_block_";

    /**
     * 将商品组装成前端需要的格式之后，进行分页缓存，并返回第一页的数据
     *
     * @param blockId
     * @param request
     * @param products
     * @param commonParam
     * @param templateType
     * @param curQuery
     * @return
     * @author: luozhuo
     * @date: 2018年7月26日 上午11:29:12
     * 20190720 zhaiweixi 搜索优化V1.4需求：如果完全匹配的结果大于等于20，搜索词的建议词个数大于等于4个，则在第12个商品后增加试试搜。
     * 如果搜索词的建议词个数大于等于8个，则在第20个商品后增加试试搜。试试搜为根据搜索词从建议词中获取到的前8位
     */
    public BlockData cacheAndGetFirstPageProducts(String blockId, UISearchRequest request, List<SearchProduct> products,
                                                  CommonRequestParam commonParam, String templateType, String curQuery) {
        List<SearchProduct> allItems = products;

        if (allItems.size() == 0) {
            BlockData resultWhenEmpty = new BlockData();
            resultWhenEmpty.setBlockId(blockId);
            resultWhenEmpty.setPageIndex(1);
            resultWhenEmpty.setHasMore(0);
            return resultWhenEmpty;
        }

        boolean addSuggestionWords = false;
        List<Template> textLinkTemplateList = new ArrayList<>();
        int curPosition = 0;
        int stepSize = CommonConstant.TemplateType.SINGLE_PRODCUT.equals(templateType)
                || CommonConstant.TemplateType.GROUP_BUY_SINGLE_PRODCUT.equals(templateType)
                || CommonConstant.TemplateType.LADDER_GROUP_PRODUCT.equals(templateType) ? 1 : 2;

        /*
         * 处理第一页数据
         */
        BlockData firstPage = new BlockData();
        firstPage.setBlockId(blockId);
        firstPage.setPageIndex(1);
        firstPage.setTemplates(convertSearchItem2Template(request, curPosition, templateType,
                allItems.size() > firstPageSize * stepSize ? allItems.subList(0, firstPageSize * stepSize) : allItems));
        firstPage.setHasMore(allItems.size() > firstPageSize * stepSize ? 1 : 0);
        // 强插试试搜模块，双排商品模板强插在第12个商品之后，第20个商品之后。即第一页的第6个位置及最后。
        if (addSuggestionWords && textLinkTemplateList.size() > 0 && templateType.equals(CommonConstant.TemplateType.DOUBLE_PRODUCT)) {
            firstPage.getTemplates().add(6, textLinkTemplateList.get(0));
            if (textLinkTemplateList.size() == 2) {
                firstPage.getTemplates().add(textLinkTemplateList.get(1));
            }
        }
        redisUtil.hset(BLOCK_CACHE_PREFIX + commonParam.getSid() + "_" + blockId, "1", firstPage, SEARCH_CACHE_TIME);
        curPosition = curPosition + firstPageSize * stepSize;

        /*
         * 处理剩下分页数据
         */
        if (firstPage.getHasMore() == 1) {

            List<SearchProduct> restSearchItems = allItems.subList(firstPageSize * stepSize, allItems.size());
            int restPageCount = restSearchItems.size() % (normalPageSize * stepSize) == 0
                    ? restSearchItems.size() / (normalPageSize * stepSize)
                    : restSearchItems.size() / (normalPageSize * stepSize) + 1;
            int curPageIndex = 2;

            for (int i = 0; i < restPageCount; i++) {
                BlockData block = new BlockData();
                block.setBlockId(blockId);
                block.setPageIndex(curPageIndex);
                block.setTemplates(convertSearchItem2Template(request, curPosition, templateType,
                        restSearchItems.size() > (i + 1) * normalPageSize * stepSize
                                ? restSearchItems.subList(i * normalPageSize * stepSize,
                                (i + 1) * normalPageSize * stepSize)
                                : restSearchItems.subList(i * normalPageSize * stepSize, restSearchItems.size())));
                block.setHasMore(i < restPageCount - 1 ? 1 : 0);
                // 搜索V1.4 计算逻辑提到前面
                curPosition = curPosition + block.getTemplates().size() * stepSize;

                // 强插试试搜，单排商品模板强插在第二页的第2个商品之后和第10个商品之后
                if (addSuggestionWords && textLinkTemplateList.size() > 0
                        && templateType.equals(CommonConstant.TemplateType.SINGLE_PRODCUT)
                        && curPageIndex == 2) {
                    if (block.getTemplates().size() > 2) {
                        block.getTemplates().add(2, textLinkTemplateList.get(0));
                        if (textLinkTemplateList.size() == 2 && block.getTemplates().size() >= 11) {
                            block.getTemplates().add(11, textLinkTemplateList.get(1));
                        }
                    }
                }

                // 将区块分页数据缓存至redis
                redisUtil.hset(BLOCK_CACHE_PREFIX + commonParam.getSid() + "_" + blockId, curPageIndex + "", block,
                        SEARCH_CACHE_TIME);

                // curPosition = curPosition + block.getTemplates().size() * stepSize;
                curPageIndex++;
            }
        }

        return firstPage;
    }

    /**
     * 将商品SearchProduct转换成前端需要的商品模板格式
     *
     * @param request
     * @param curPosition
     * @param templateType
     * @param searchItems
     * @return
     * @author: luozhuo
     * @date: 2018年7月26日 上午11:30:34
     */
    private List<Template> convertSearchItem2Template(UISearchRequest request, int curPosition, String templateType,
                                                      List<SearchProduct> searchItems) {
        if (searchItems == null || searchItems.size() == 0) {
            return new ArrayList<>();
        }

        List<Template> result = new ArrayList<>();

        if (CommonConstant.TemplateType.SINGLE_PRODCUT.equals(templateType)) { // 单商品模板
            for (SearchProduct item : searchItems) {
                Template template = new Template();
                template.setType(CommonConstant.TemplateType.SINGLE_PRODCUT);

                template.getData().add(item);

                result.add(template);
            }
        } else if (CommonConstant.TemplateType.GROUP_BUY_SINGLE_PRODCUT.equals(templateType)) {
            for (SearchProduct item : searchItems) {
                Template template = new Template();
                template.setType(CommonConstant.TemplateType.GROUP_BUY_SINGLE_PRODCUT);

                template.getData().add(item);

                result.add(template);
            }
        } else if (CommonConstant.TemplateType.LADDER_GROUP_PRODUCT.equals(templateType)) {
            for (SearchProduct item : searchItems) {
                Template template = new Template();
                template.setType(CommonConstant.TemplateType.LADDER_GROUP_PRODUCT);
                template.getData().add(item);
                result.add(template);
            }
        } else { // 双商品模板
            for (int i = 0; i < searchItems.size(); i += 2) {
                Template template = new Template();
                template.setType(CommonConstant.TemplateType.DOUBLE_PRODUCT);

                SearchProduct product1 = searchItems.get(i);
                template.getData().add(product1);

                if ((i + 1) < searchItems.size()) {
                    SearchProduct product2 = searchItems.get(i + 1);
                    template.getData().add(product2);
                }

                // 20180628 IOS 特权金版本之后才能使用，否则前端有BUG，在双商品模板中只返回一个商品时会报错
                if (PlatformEnum.IOS.getName().equalsIgnoreCase(request.getPlatform().getName())
                        && (request.getAppVersionNum() == null || request.getAppVersionNum() < 90)
                        && template.getData().size() <= 1) {
                    continue;
                }

                result.add(template);
            }
        }

        return result;
    }

    /**
     * 从缓存中获取块分页数据
     *
     * @param request
     * @return
     * @author: luozhuo
     * @date: 2018年7月26日 上午11:37:07
     */
    public BlockData getPageBlockData(UISearchPageRequest request) {
        String cacheKey = BLOCK_CACHE_PREFIX + request.getSid() + "_" + request.getBlockId();
        BlockData blockCache = (BlockData) redisUtil.hget(cacheKey, request.getPageIndex() + "");
        if (blockCache == null) {
            blockCache = new BlockData();
            blockCache.setHasMore(0);
            blockCache.setPageIndex(request.getPageIndex());
            blockCache.setBlockId(request.getBlockId());
        }

        return blockCache;
    }

    /**
     * 构造试试搜的模板
     *
     * @param suggestionWordList
     * @param startIndex         inclusive
     * @param endIndex           exclusive
     * @param request
     * @param blockId
     * @return
     */
    @Deprecated
    private Template buildQuerySuggestionTemplate(List<String> suggestionWordList, int startIndex, int endIndex, UISearchRequest request, String blockId) {
        Template template = new Template();
        template.setType(CommonConstant.TemplateType.TEXT_BUTTON);
        TextLinkTemplateData textLinkTemplateData = new TextLinkTemplateData();
        List<SearchTitle> templateTitle = new ArrayList<>();
        SearchTitle title = new SearchTitle();

        List<TitleText> oneLine = new ArrayList<>();
        oneLine.add(new TitleText("试试搜", ColorCodeConsts.TITLE_COLOR_BLACK_333333, 1));

        title.setContents(oneLine);
        title.setPicType(1);
        templateTitle.add(title);
        textLinkTemplateData.setTitle(templateTitle);

        String aid = IdCalculateUtil.createAid(request.getAid() + IdCalculateUtil.createBlockId());
        for (int index = startIndex; index < suggestionWordList.size() && index < endIndex; index++) {
            String suggestionWord = suggestionWordList.get(index);
            LinkButton button = new LinkButton();
            button.setTitle(suggestionWord);
            TemplateRoute route = new TemplateRoute();
            route.setQuery(suggestionWord);
            route.setTrackParam(URLEncoder.encode(String.format("sid=%s&bkId=%s&pos=%s", request.getSid(),
                    blockId, index)));
//			if (PlatformEnum.IOS.equals(request.getPlatform()) || PlatformEnum.ANDROID.equals(request.getPlatform())) {
//				route.setType(CommonConstant.ROUTE_TYPE_NEW_PAGE);
//			}else{
//				route.setType(CommonConstant.ROUTE_TYPE_SEARCH_PAGE);
//			}
            route.setType(CommonConstant.ROUTE_TYPE_SEARCH_PAGE);
            route.setRouteUrl(RouterUtil.getSearchNewPageRoute(request.getPlatform(), "q=" + suggestionWord,
                    TrackParamUtil.generateSTP(request, "searchResult_trySuggestions", String.valueOf(index), aid,"")));

            button.setRoute(route);
            textLinkTemplateData.getButtons().add(button);
        }
        //
        template.getData().add(textLinkTemplateData);
        return template;
    }
}
