package com.biyao.moses.service.imp;

import java.util.HashMap;
import java.util.Map;

import com.alibaba.fastjson.TypeReference;
import com.biyao.moses.common.enums.MosesConfTypeEnum;
import com.biyao.moses.common.enums.RedisKeyTypeEnum;
import com.biyao.moses.exp.util.MosesConfUtil;
import com.biyao.moses.util.MatchRedisUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.biyao.moses.common.constant.ErrorCode;
import com.biyao.moses.model.template.entity.RecommendPage;
import com.biyao.moses.params.ApiResult;
import com.biyao.moses.util.RedisUtil;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class HomePageCacheServiceImpl {

	@Autowired
	RedisUtil redisUtil;

	@Autowired
	MosesConfUtil mosesConfUtil;

	@Autowired
	MatchRedisUtil matchRedisUtil;

	private static final String REDIS_AVN_KEY = "mosesswt:avn";

	private static final String MOSES_SOURCE_PREFIX = "mosessou:";

	// mosesswt:avn 二级key：HP， value=true|moses:pid_32,10300057
	public static Map<String, String> HOME_AVN_MAP = new HashMap<>();

	// mosessou：HP 二级key：IOS； value： 1-9999|HP,10000-20000|HP
	public static Map<String, Map<String, String>> MOSES_SOURCE = new HashMap<>();

	public void initpageIdCache() {
		Boolean[] isRedisExceptionArray = new Boolean[]{false};
		//实验配置迁移后的 临时方案，先从match集群中查询，再从原集群查询
		Map<String, String> mosesAvn = matchRedisUtil.hgetAll(REDIS_AVN_KEY, isRedisExceptionArray);
		try {
			if (mosesAvn != null && !mosesAvn.isEmpty()) {
				HOME_AVN_MAP.putAll(mosesAvn);
				mosesConfUtil.refreshConfCacheAndDB(MosesConfTypeEnum.SwitchConf, REDIS_AVN_KEY, null, JSON.toJSONString(HOME_AVN_MAP), RedisKeyTypeEnum.HASH.getId());
			}else{
				mosesAvn = redisUtil.hgetAll(REDIS_AVN_KEY, isRedisExceptionArray);
				if (mosesAvn != null && !mosesAvn.isEmpty()) {
					log.error("[严重异常]redis key {} 未迁移到match集群", REDIS_AVN_KEY);
					HOME_AVN_MAP.putAll(mosesAvn);
					mosesConfUtil.refreshConfCacheAndDB(MosesConfTypeEnum.SwitchConf, REDIS_AVN_KEY, null, JSON.toJSONString(HOME_AVN_MAP), RedisKeyTypeEnum.HASH.getId());
				}else {
					Boolean isRedisException = isRedisExceptionArray[0];
					String value = mosesConfUtil.dealOnRedisValueNullOrException(MosesConfTypeEnum.SwitchConf, REDIS_AVN_KEY, null, RedisKeyTypeEnum.HASH.getId(), isRedisException);
					if (StringUtils.isNotBlank(value)) {
						Map<String, String> map = JSON.parseObject(value, new TypeReference<Map<String, String>>() {
						});
						HOME_AVN_MAP.putAll(map);
					}
				}
			}
			for (Map.Entry<String, String> m : HOME_AVN_MAP.entrySet()) {
				Boolean[] isExceptionArray = new Boolean[] {false};
				String key = MOSES_SOURCE_PREFIX + m.getKey();
				//实验配置迁移后的 临时方案，先从match集群中查询，再从原集群查询
				Map<String, String> mosesswt = matchRedisUtil.hgetAll(key, isExceptionArray);
				if (mosesswt != null && !mosesswt.isEmpty()) {
					Map<String, String> map = new HashMap<>();
					map.putAll(mosesswt);
					MOSES_SOURCE.put(key, map);
					mosesConfUtil.refreshConfCacheAndDB(MosesConfTypeEnum.VersionConf, key, null, JSON.toJSONString(map), RedisKeyTypeEnum.HASH.getId());
				}else{
					mosesswt = redisUtil.hgetAll(key, isExceptionArray);
					if (mosesswt != null && !mosesswt.isEmpty()) {
						log.error("[严重异常]redis key {} 未迁移到match集群", key);
						Map<String, String> map = new HashMap<>();
						map.putAll(mosesswt);
						MOSES_SOURCE.put(key, map);
						mosesConfUtil.refreshConfCacheAndDB(MosesConfTypeEnum.VersionConf, key, null, JSON.toJSONString(map), RedisKeyTypeEnum.HASH.getId());
					}else {
						Boolean isException = isExceptionArray[0];
						String value = mosesConfUtil.dealOnRedisValueNullOrException(MosesConfTypeEnum.VersionConf, key, null, RedisKeyTypeEnum.HASH.getId(), isException);
						if (StringUtils.isNotBlank(value)) {
							Map<String, String> map = JSON.parseObject(value, new TypeReference<Map<String, String>>() {
							});
							MOSES_SOURCE.put(key, map);
						}
					}
				}
			}
		} catch (Exception e) {
			log.error("pageId缓存异常", e);
		}
		log.info("获取pageId页面缓存MOSES_SOURCE：" + JSON.toJSONString(MOSES_SOURCE));
		log.info("获取pageId页面缓存HOME_AVN_MAP：" + JSON.toJSONString(HOME_AVN_MAP));
	
	}

	/**
	 * 查询pageId缓存
	 * 
	 * @param source
	 * @param avn
	 * @param pf
	 * @return
	 */
	public ApiResult<RecommendPage> getPageIdCache(String source, String avn, String pf) {
		ApiResult<RecommendPage> apiResult = new ApiResult<RecommendPage>();
		RecommendPage rp = new RecommendPage();
		try {
			Map<String, String> map = MOSES_SOURCE.get(MOSES_SOURCE_PREFIX + source.toUpperCase());
			if (map == null || map.isEmpty()) {
				apiResult.setSuccess(ErrorCode.SYSTEM_ERROR_CODE);
				apiResult.setError("查询缓存中pageId无数据");
				return apiResult;
			}
			String resultStr = map.get(pf.toUpperCase());
			if (!StringUtils.isEmpty(resultStr)) {
				String[] ver = resultStr.split(",");
				for (String avnStr : ver) {
					if (!StringUtils.isEmpty(avnStr)) {
						apiResult = queryPageAndTopicId(rp, avn, avnStr);
					}
				}
			}
		} catch (Exception e) {
			log.error("[严重异常]查询pageId缓存异常！", e);
			apiResult.setSuccess(ErrorCode.SYSTEM_ERROR_CODE);
			apiResult.setError("查询缓存中pageId错误!");
			return apiResult;
		}
		return apiResult;
	}

	/**
	 * 
	 * @param rp
	 * @param avn
	 *            当前参数中的版本 1
	 * @param avnStr
	 *            缓存中的版本 1-9999|HP
	 * @return
	 */
	private ApiResult<RecommendPage> queryPageAndTopicId(RecommendPage rp, String avn, String avnStr) throws Exception {
		ApiResult<RecommendPage> apiResult = new ApiResult<RecommendPage>();
		String[] mv = avnStr.split("\\|");
		String[] minMax = mv[0].split("-");
		String version = mv[1];
		String minAvn = minMax[0];
		String maxAvn = minMax[1];
		if (Double.valueOf(minAvn) <= Double.valueOf(avn) && Double.valueOf(avn) <= Double.valueOf(maxAvn)) {
			// 查询开关和pageId，topicId
			// 二级key：HP， value=true|moses:pid_32,10300057
			String resultPage = HOME_AVN_MAP.get(version.toUpperCase());
			String[] switchOnOff = resultPage.split("\\|");
			String onoff = switchOnOff[0];
			String[] pageTopic = switchOnOff[1].split(",");
			if (pageTopic.length > 0 && pageTopic.length <= 1) {
				rp.setPid(pageTopic[0]);
				rp.setTopicId("");
			} else if (pageTopic.length > 0 && pageTopic.length > 1) {
				rp.setPid(pageTopic[0]);
				rp.setTopicId(pageTopic[1]);
			}
			if(!Boolean.parseBoolean(onoff)) {
				rp.setPid("");
				rp.setTopicId("");
			}
			rp.setOnoff(Boolean.parseBoolean(onoff));
		}
		apiResult.setData(rp);
		return apiResult;
	}
}
