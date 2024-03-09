package com.biyao.moses.service.imp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.biyao.moses.common.constant.CommonConstants;
import com.biyao.moses.util.RedisUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * 存储用户足迹共现商品
 * 
 * @Description
 * @author zyj
 * @Date 2018年11月13日
 */
@Component
@Slf4j
public class FootPrintExecutor implements Runnable {

	private static final String myStreetTopicKeyPrefix = CommonConstants.DEFAULT_PREFIX
			+ CommonConstants.MYSTREET_TOPICID + "_" + CommonConstants.UDM_MATCH_NAME + "_"
			+ CommonConstants.MYSTREET_TOPICID_EXPNUM + "_";

	private String uuid;
	private String pid;
	private RedisUtil redisUtil;

	public FootPrintExecutor() {
	}

	public FootPrintExecutor(String uuid, String pid, RedisUtil redisUtil) {
		this.uuid = uuid;
		this.pid = pid;
		this.redisUtil = redisUtil;
	}

	// 每个足迹展示商品共现数
	private static final int FOOT_SUBLIT_NUM = 5;

	private static final int FOOT_PRINT_NUM = 15;
	// 需要展示的商品共现总数
	private static final int PRODUCT_NUM = 75;

	@Override
	public void run() {
		try {

			String matchKey = CommonConstants.MOSES_FOOT_PRINT + uuid;
			Set<String> zrevrange = redisUtil.zrange(matchKey, 0, -1);
			// 足迹前3个图片不获取商品共现，等于3个的时候再获取。超过3个累加获取,最大15个足迹图片（每个足迹取5个商品共现，最大75个共现商品）
			if (zrevrange.size() == CommonConstants.PRODUCT_SPLIT_NUM) {
				for (String pid : zrevrange) {
					this.pid = pid;
					pushPrint(matchKey);
				}
			} else if (zrevrange.size() > CommonConstants.PRODUCT_SPLIT_NUM) {
				pushPrint(matchKey);
			}
		} catch (Exception e) {
			log.error("存储用户足迹失败！" + uuid, e);
		}
	}

	private void pushPrint(String matchKey) {
		Map<String, String> totalMap = new HashMap<>();
		String myStreetKey = CommonConstants.MOSES_MY_STREET + uuid;

		String myRefreshKey = CommonConstants.MY_STREET_REFRESH_KEY + uuid;

		String refreshKey = redisUtil.getString(myRefreshKey);

		// 根据pid查询reids中商品共现
		List<String> occProducts = new ArrayList<>();
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

		} else {
			// 查询我的街内容
			List<String> lrange = redisUtil.lrange(myStreetKey, 0, -1);
			if (lrange != null && lrange.size() > 0) {
				// 我的街内容去重复
				for (String productId : lrange) {
					totalMap.put(productId, productId);
				}
				// 追加我的街数据内容
				if (!StringUtils.isEmpty(pid)) {
					getGoodsOccurrence(totalMap, occProducts, pid);
				}
			} else {
				getGoodsOccurrence(totalMap, occProducts, pid);
			}
		}

		if (occProducts.size() > 0) {
			lpushSize(occProducts, myStreetKey, uuid);
			recordMystreetScore(occProducts, myStreetKey, uuid);

		}

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
			Map<String, String> map = new HashMap<>();
			String myProductStr = redisUtil.getString(myStreetTopicKeyPrefix + uuid);
			StringBuffer sbf = new StringBuffer();

			List<String> products = new ArrayList<>();
			for (int i = 0; i < occProducts.size(); i++) {
				map.put(occProducts.get(i), occProducts.get(i));
				products.add(occProducts.get(i));
			}
			if (!StringUtils.isEmpty(myProductStr)) {
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
