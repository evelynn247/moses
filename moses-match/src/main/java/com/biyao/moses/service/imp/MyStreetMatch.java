package com.biyao.moses.service.imp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.by.profiler.annotation.BProfiler;
import com.by.profiler.annotation.MonitorType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.biyao.moses.common.constant.CommonConstants;
import com.biyao.moses.model.exp.MatchDataSourceTypeConf;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;
import com.biyao.moses.service.RecommendMatch;
import com.biyao.moses.util.RedisUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * 我的街match
 * 
 * @Description
 * @author zyj
 * @Date 2018年10月10日
 */
@Slf4j
@Component("MyStreetMatch")
public class MyStreetMatch implements RecommendMatch {

	@Autowired
	RedisUtil redisUtil;
	// 每个足迹展示商品共现数
	private static final int FOOT_SUBLIT_NUM = 5;
	// 需要展示的商品共现总数
	private static final int PRODUCT_NUM = 75;

	private static final int FOOT_PRINT_NUM = 15;

	private static final String myStreetTopicKeyPrefix = CommonConstants.DEFAULT_PREFIX
			+ CommonConstants.MYSTREET_TOPICID + "_" + CommonConstants.UDM_MATCH_NAME + "_"
			+ CommonConstants.MYSTREET_TOPICID_EXPNUM + "_";

	@BProfiler(key = "com.biyao.moses.service.imp.MyStreetMatch.executeRecommendMatch",
			monitorType = {MonitorType.TP, MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR})
	@Override
	public Map<String, List<TotalTemplateInfo>> executeRecommendMatch(String dataKey, MatchDataSourceTypeConf mdst,
			String uuId) {
		Map<String, List<TotalTemplateInfo>> resultMap = new HashMap<>();
		List<TotalTemplateInfo> totalList = new ArrayList<>();
		Map<String, String> totalMap = new HashMap<>();
		try {
			String myStreetKey = CommonConstants.MOSES_MY_STREET + uuId;
			String myRefreshKey = CommonConstants.MY_STREET_REFRESH_KEY + uuId;
			String matchKey = CommonConstants.MOSES_FOOT_PRINT + uuId;
			// 根据pid查询reids中商品共现
			List<String> occProducts = new ArrayList<>();
			String refreshKey = redisUtil.getString(myRefreshKey);
			if (StringUtils.isEmpty(refreshKey) || !"1".equals(refreshKey)) {
				// 数据过期，需要重新刷新
				Set<String> zrevrange = redisUtil.zrevrange(matchKey, 0, -1);
				if (zrevrange.size() > CommonConstants.PRODUCT_SPLIT_NUM) {
					// 我的街数据过期了，重新生成
					int number = 0;
					List<String> pidList = new ArrayList<>();
					for (String productId : zrevrange) {
						if (number > FOOT_PRINT_NUM) {
							break;
						}
						pidList.add(productId);
						number++;
					}
					// Collections.reverse(pidList);
					for (int i = 0; i < pidList.size(); i++) {
						getGoodsOccurrence(totalMap, occProducts, pidList.get(i));
					}
				}
				if (occProducts.size() > 0) {
					lpushSize(occProducts, myStreetKey, uuId);
					recordMystreetScore(occProducts, myStreetKey, uuId);
				}
			}

			List<String> lrange = redisUtil.lrange(myStreetKey, 0, -1);
			for (String pid : lrange) {
				TotalTemplateInfo temp = new TotalTemplateInfo();
				temp.setId(pid);
				totalList.add(temp);
			}
			resultMap.put(dataKey, totalList);

		} catch (Exception e) {
			log.error("查询我的街match异常:", e);
		}
		return resultMap;
	}

	// 查询商品共现数据
	private void getGoodsOccurrence(Map<String, String> totalMap, List<String> occProducts, String pid) {

		String matchDataStr = redisUtil.hgetStr(CommonConstants.MOSES_GOD_OCCURRENCE, pid);
		if (StringUtils.isEmpty(matchDataStr)) {
			return;
		}
		String[] matchData = matchDataStr.split(",");
		int breakNum = 0;
		for (String productId : matchData) {
			if (breakNum >= FOOT_SUBLIT_NUM) {
				break;
			} else if (totalMap.containsKey(productId)) {
				breakNum++;
				continue;
			} else {
				occProducts.add(productId);
				totalMap.put(productId, productId);
				breakNum++;
			}
		}
	}

	// 存储商品共现数据
	private void lpushSize(List<String> occProducts, String myStreetKey, String uuid) {
		String[] occproductStr = new String[occProducts.size()];
		List<String> list = new ArrayList<>();
		list.addAll(occProducts);
		// 列表倒序，因为列表list 执行命令 LPUSH list a b c ，列表的值将是 c b a
		Collections.reverse(list);
		list.toArray(occproductStr);
		redisUtil.lpush(myStreetKey, occproductStr);
		// 截取75个商品
		redisUtil.ltrim(myStreetKey, 0, PRODUCT_NUM);
		redisUtil.expire(myStreetKey, CommonConstants.MY_STREET_EXEPIRE_TIME);
		redisUtil.setString(CommonConstants.MY_STREET_REFRESH_KEY + uuid, "1", CommonConstants.MY_STREET_REFRESH_TIME);

	}

	// 存储我的街，并设置分值
	private void recordMystreetScore(List<String> occProducts, String myStreetKey, String uuid) {

		try {
			String myProductStr = redisUtil.getString(myStreetTopicKeyPrefix + uuid);
			StringBuffer sbf = new StringBuffer();
			List<String> products = new ArrayList<>();
			for (int i = 0; i < occProducts.size(); i++) {
				products.add(occProducts.get(i));
			}
			if (!StringUtils.isEmpty(myProductStr)) {
				Map<String, String> map = new HashMap<>();
				String[] split = myProductStr.split(",");
				for (int i = split.length - 1; i >= 0; i--) {
					if (products.size() > PRODUCT_NUM) {
						break;
					}
					String[] str = split[i].split(":");
					if (!map.containsKey(str[0])) {
						products.add(str[0]);
						map.put(str[0], str[0]);
					}
				}
			}
			double baseScore = 1.0;
			for (int i = 0; i < products.size(); i++) {
				double score = baseScore / (i == 0 ? 1 : (i + 1));
				sbf.append(products.get(i) + ":" + score + ",");
			}
			String productsValueStr = sbf.toString();
			productsValueStr = productsValueStr.substring(0, productsValueStr.length() - 1);
			redisUtil.setString(myStreetTopicKeyPrefix + uuid, productsValueStr, -1);

		} catch (Exception e) {
			log.error("存储我的街，并设置分值异常！", e);
		}

	}

}
