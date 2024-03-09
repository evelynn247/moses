package com.biyao.moses.service.imp;

import com.biyao.moses.cache.CategorySortCache;
import com.biyao.moses.common.constant.CommonConstants;
import com.biyao.moses.compare.DesComparator;
import com.biyao.moses.context.ByUser;
import com.biyao.moses.exp.ExpirementSpace;
import com.biyao.moses.model.exp.AlgorithmConf;
import com.biyao.moses.model.exp.ExpRequest;
import com.biyao.moses.model.exp.Expirement;
import com.biyao.moses.model.template.FrontendCategory;
import com.biyao.moses.model.template.FrontendCategoryForAct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 
 * @Description 
 * 无法接入现有match rank
 * 通过实验配置数据号及置顶的类目
 * @author Alpaca
 * @Time 2019年5月23日 下午3:27:03
 */
@Slf4j
@Service
public class CategorySortService {
	
	@Autowired
	ExpirementSpace expirementSpace;

	@Autowired
	CategorySortCache categorySortCache;
	
	
	private static final String CATEGORY_SORT = "categorySort";

	/**
	 * 活动类目排序
	 * @param categoryList
	 * @param user
	 */
	public void sortCategoryForAct(List<FrontendCategoryForAct> categoryList, ByUser user){
		if (CollectionUtils.isEmpty(categoryList)) {

			return;
		}
		//获取排序分集合
		Map<Integer, Double> catScoreMap = categorySortCache.getCategorySortMap(user.getSex());
		categoryList.sort(((o1, o2) -> {
			Double categoryScoreO1 = catScoreMap.getOrDefault(o1.getCategoryId(),0d);
			Double categoryScoreO2 = catScoreMap.getOrDefault(o2.getCategoryId(),0d);
			return categoryScoreO2.compareTo(categoryScoreO1);
		}));
		// 对子级类目进行排序
		for (FrontendCategoryForAct frontendCategoryForAct : categoryList) {
			if(CollectionUtils.isNotEmpty(frontendCategoryForAct.getSubFrontendCategoryList())){
				sortCategoryForAct(frontendCategoryForAct.getSubFrontendCategoryList(),user);
			}
		}
	}

	/**
	 * @Description:
	 * 置顶类目不参与排序
	 * @param categoryList 待排序类目 
	 * @param user 用户
	 * @author: zhangjian01
	 * @time:2019年9月11日 上午10:40:10
	 */
	public void sortCategory(List<FrontendCategory> categoryList, ByUser user){
		
		try {
			String uuid = user.getUuid();

			//如果个性化推荐设置开关关闭，则对类目进行排序，即使用cms配置的的顺序
			if(!user.isPersonalizedRecommendSwitch()){
				return;
			}

			// 要排序类目数据空，则返回
			if (CollectionUtils.isEmpty(categoryList)||StringUtils.isEmpty(uuid)) {
				return;
			}
			
			// 获取实验配置
			ExpRequest expRequest = ExpRequest.builder().tid(CATEGORY_SORT).uuid(uuid)
									.layerName(CommonConstants.LAYER_NAME_MATCH).build();
			
			// 使用 AlgorithmConf 中的 algorithmDes描述作为置顶类目配置字段
			Expirement expirement = expirementSpace.getExpirement(expRequest);

			// 如果没有配置实验则不进行排序
			if (expirement==null) {
				return;
			}
			
			// 获取redis key数据
			// 类目级别
			Integer categoryLevel = 1;
//			Integer categoryLevel = categoryList.get(0).getCategoryLevel();
			
			AlgorithmConf algorithmConf = expirement.getConfList().get(0);
			// 获取试验号
//			String expNum = algorithmConf.getExpNum();
			String expNum = "0001";
			// 获取置顶类目
			String[] topCatetory = null;
			// 整一个set,当置顶的类目集，之后判断设置高分值
			HashSet<String> highScoreSet = new HashSet<String>();
			String algorithmDes = algorithmConf.getAlgorithmDes();
			if (StringUtils.isNotBlank(algorithmDes)) {
				topCatetory = algorithmDes.split(",");
				for (String topId : topCatetory) {
					highScoreSet.add(topId);
				}
			}
			//性别
			String sex = user.getSex();
			//获取排序分集合
			Map<Integer, Double> catScoreMap =categorySortCache.getCategorySortMap(sex);
			if (catScoreMap==null||catScoreMap.isEmpty()) {
				return;
			}
			
			// 设置分值
			for (int i = 0; i < categoryList.size(); i++) {
				FrontendCategory frontendCategory = categoryList.get(i);
				Integer catId = frontendCategory.getCategoryId();
				Double score = catScoreMap.getOrDefault(catId,0d);
				if (highScoreSet.contains(String.valueOf(catId))) {
					//包含在置顶set中
					frontendCategory.setScore(999d);
				}else{
					frontendCategory.setScore(score);
				}

			}
			// 进行排序
			Collections.sort(categoryList, new DesComparator());
			
		} catch (Exception e) {
			log.error("[严重异常][类目排序]处理类目排序时出现异常",e);
		}
	}

	/**
	 * 通过入参决定是否需要过滤掉定制类目, 默认需要过滤定制类目
	 */
	public List<FrontendCategory> filterCustomCate(List<FrontendCategory> frontendCategoryList, String showCustomCate){
        if ("1".equals(showCustomCate) || CollectionUtils.isEmpty(frontendCategoryList)) {
            return frontendCategoryList;
        }

	    List<FrontendCategory> result = new ArrayList<>();
		try {
			for(FrontendCategory frontendCategory : frontendCategoryList){
			    if(frontendCategory == null){
			        continue;
                }
                Integer categoryType = frontendCategory.getCategoryType();
                if (categoryType != null && "1".equals(categoryType.toString())) {
                    continue;
                }
			    result.add(frontendCategory);
            }
		}catch (Exception e){
			log.error("[严重异常]过滤定制类目异常", e);
            result = frontendCategoryList;
		}
		return result;
	}
}