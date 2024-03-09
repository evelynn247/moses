package com.biyao.moses.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSONObject;
import com.biyao.moses.common.constant.ErrorCode;
import com.biyao.moses.common.constant.RedisKeyConstant;
import com.biyao.moses.constants.EXPLayerType;
import com.biyao.moses.dto.DomainDetailInfo;
import com.biyao.moses.model.exp.AlgorithmConf;
import com.biyao.moses.model.exp.Expirement;
import com.biyao.moses.model.exp.ExpirementDomain;
import com.biyao.moses.model.exp.ExpirementLayer;
import com.biyao.moses.params.ApiResult;
import com.biyao.moses.params.ExpirementParam;
import com.biyao.moses.util.JodaDateUtils;
import com.biyao.moses.util.RedisUtil;
import sun.rmi.runtime.Log;

/**
 * @Description 实验配置管理
 * @author houkun
 * @Date 2018年10月30日
 */
@RestController
@Api("moses实验配置")
@RequestMapping(value = "/moses/exp")
@Slf4j
public class MosesExpController {

	@Autowired
	private RedisUtil redisUtil;

	private Logger logger_backup = LoggerFactory.getLogger("moses.backup");

	@ApiOperation(value = "获取实验预制模板")
	@GetMapping(value = "/expTemplate")
	public ExpirementDomain expTemplate() {

		ExpirementDomain expirementDomain = new ExpirementDomain();

		List<ExpirementLayer> layers = new ArrayList<ExpirementLayer>();
		expirementDomain.setLayers(layers);

		// 配置实验需要时间，使用时间不会有重复
		String domainId = JodaDateUtils.getNowTime(JodaDateUtils.DATE_KEY_STR);

		expirementDomain.setDomaimId("do" + domainId);
		expirementDomain.setDomainDes("实验域描述");

		ExpirementLayer eLayer = new ExpirementLayer();

		String randomLayerId = "la" + new Random().nextInt(100);

		eLayer.setLayerId(randomLayerId);
		eLayer.setLayerName(EXPLayerType.MATCH.getName());
		eLayer.setDivison("uuid");
		eLayer.setEndDate("结束日期");
		eLayer.setStartDate("开始日期");

		List<Expirement> expirements = new ArrayList<Expirement>();

		List<AlgorithmConf> confList = new ArrayList<AlgorithmConf>();
		confList.add(AlgorithmConf.builder().id("算法ID").name("matchName").expNum("数据编号").weight("match权重")
				.algorithmDes("算法描述").build());
//		confList.add(AlgorithmConf.builder().id("算法ID2").name("matchName2").expNum("数据编号2").weight("match权重2")
//				.algorithmDes("算法描述").build());

		Expirement exp = Expirement.builder().expId("实验ID(唯一)").expName("实验名").expType(0).flow("左闭右开  [0,50)")
				.whiteList("白名单").confList(confList).expDes("实验描述").build();

		Expirement exp2 = Expirement.builder().expId("实验ID(唯一)").expName("实验名").expType(0).flow("左闭右开  [0,50)")
				.whiteList("白名单").confList(confList).expDes("实验描述").build();

		expirements.add(exp);
		expirements.add(exp2);

		eLayer.setExpirements(expirements);
		layers.add(eLayer);

		return expirementDomain;
	}
	
	/**
	 * 添加实验
	 * moses:moses_exp_ids  topicId  domainId
	 * mosesexp:domainId   domainJSON
	 */
	@ApiOperation(value = "添加实验" ,notes = "moses:moses_exp_ids  topicId  domainId  \r\n mosesexp:domainId   domainJSON")
	@PostMapping(value = "/expirementAdd")
	public ApiResult<String> expirementAdd(@RequestBody ExpirementDomain expDomain,
			@ApiParam(value="id使用topicId") ExpirementParam expparam) {

		// 二级key = tid | topicId value = domainId
		Map<String, String> expIdsMap = redisUtil.hgetAll(RedisKeyConstant.MOSES_EXP_IDS);

		String[] split = expparam.getId().split(",");
		for (String id : split) {
			if (expIdsMap == null || expIdsMap.isEmpty()) {
				break;
			}
			boolean containsKey = expIdsMap.containsKey(id);
			if (containsKey) {
				return new ApiResult<>(ErrorCode.PARAM_ERROR_CODE, id + "已经存在", null);
			}
		}
		// 添加分布式锁
		boolean lock = redisUtil.lock(RedisKeyConstant.MOSES_EXP_LOCK, 120);
		try {
			if (lock) {
				String jsonString = JSONObject.toJSONString(expDomain);
				for (String id : split) {
					redisUtil.hset(RedisKeyConstant.MOSES_EXP_IDS, id, expDomain.getDomaimId());
					redisUtil.setString(RedisKeyConstant.MOSES_EXP_PREFIX + expDomain.getDomaimId(), jsonString, -1);
					logger_backup.info("confType:exp | "+"redis.Key:"+RedisKeyConstant.MOSES_EXP_IDS+" | redis.Field:"+id +" | redis.Value:" + expDomain.getDomaimId());
					logger_backup.info("confType:domain | "+"redis.Key:"+RedisKeyConstant.MOSES_EXP_PREFIX + expDomain.getDomaimId()+" | redis.Value:" + jsonString);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			redisUtil.unlock(RedisKeyConstant.MOSES_EXP_LOCK);
		}

		return new ApiResult<String>();
	}

	@ApiOperation(value = "通过domainId查询实验")
	@PostMapping(value = "/expirementSearchByDomainId")
	public ExpirementDomain expirementSearch(@ApiParam(value="id使用domainId") ExpirementParam expparam) {

		String string = redisUtil.getString(RedisKeyConstant.MOSES_EXP_PREFIX + expparam.getId());

		ExpirementDomain expirementDomain = JSONObject.parseObject(string, ExpirementDomain.class);

		return expirementDomain;

	}

	/**
	 * 通过tid或topicId修改当前tid配置的domainId实验域
	 * 相当于批量修改当前domainId配置的所有tid
	 */
	@ApiOperation(value = "通过tid修改实验", notes = "通过tid或topicId修改当前tid配置的domainId实验域,相当于批量修改当前domainId配置的所有tid")
	@PostMapping(value = "/expirementEditByTid")
	public ApiResult<String> expirementEdit(@RequestBody ExpirementDomain expDomain,
			@ApiParam(value="id使用topicId") ExpirementParam expparam) {
		String jsonString = JSONObject.toJSONString(expDomain);
		String[] split = expparam.getId().split(",");

		// 二级key = tid | topicId value = domainId
		Map<String, String> expIdsMap = redisUtil.hgetAll(RedisKeyConstant.MOSES_EXP_IDS);
		for (String id : split) {
			boolean containsKey = expIdsMap.containsKey(id);
			if (!containsKey) {
				return new ApiResult<>(ErrorCode.PARAM_ERROR_CODE, id + "tid没有配置实验", null);
			}
			if (!expIdsMap.get(id).equals(expDomain.getDomaimId())) {
				return new ApiResult<>(ErrorCode.PARAM_ERROR_CODE, expDomain.getDomaimId() + "配置domainId不一致", null);
			}
		}

		boolean lock = redisUtil.lock(RedisKeyConstant.MOSES_EXP_LOCK, 120);
		try {
			if (lock) {
				for (String id : split) {
					String domainId = expIdsMap.get(id);
					redisUtil.setString(RedisKeyConstant.MOSES_EXP_PREFIX + domainId, jsonString, -1);
					logger_backup.info("confType:domain | "+"redis.Key:"+RedisKeyConstant.MOSES_EXP_PREFIX + domainId+" | redis.Value:" + jsonString);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			redisUtil.unlock(RedisKeyConstant.MOSES_EXP_LOCK);
		}

		return new ApiResult<String>();
	}
	
	/**
	 * 删除在moses:moses_exp_ids  topicId  domainId中的二级key
	 * 
	 */
	@ApiOperation(value = "删除实验(仅删除在总实验里的id)",notes = " 删除在moses:moses_exp_ids  topicId  domainId中的二级key")
	@PostMapping(value = "/expirementDelete")
	public ApiResult<String> expirementDelete(@ApiParam(value="id使用topicId") ExpirementParam expparam) {
		String[] split = expparam.getId().split(",");

		boolean lock = redisUtil.lock(RedisKeyConstant.MOSES_EXP_LOCK, 120);
		try {
			if (lock) {
				for (String id : split) {
					redisUtil.hdel(RedisKeyConstant.MOSES_EXP_IDS, id);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			redisUtil.unlock(RedisKeyConstant.MOSES_EXP_LOCK);
		}

		return new ApiResult<String>();
	}

	// 查询当前存在所有实验
	@ApiOperation(value = "查询当前配置所有实验")
	@PostMapping(value = "/getAllExp")
	public List<ExpirementDomain> getAllExp() {

		HashSet<String> hashSet = new HashSet<String>();

		List<ExpirementDomain> resultList = new ArrayList<ExpirementDomain>();

		Map<String, String> expIdsMap = redisUtil.hgetAll(RedisKeyConstant.MOSES_EXP_IDS);

		if (expIdsMap == null || expIdsMap.isEmpty()) {
			return resultList;
		}

		for (Entry<String, String> entry : expIdsMap.entrySet()) {
			try{
				String string = redisUtil.getString(RedisKeyConstant.MOSES_EXP_PREFIX + entry.getValue());
				ExpirementDomain expirementDomain = JSONObject.parseObject(string, ExpirementDomain.class);

				if (hashSet.contains(expirementDomain.getDomaimId())) {
					continue;
				} else {
					hashSet.add(expirementDomain.getDomaimId());
				}
				resultList.add(expirementDomain);
			}
			catch(Exception e){
				log.error(e.getMessage());
				continue;
			}
		}
		return resultList;
	}

	// 查询当前tid配置的domainId
	@ApiOperation(value = "查询当前tid配置的domainId")
	@PostMapping(value = "/getExpMap")
	public Map<String, String> getExpMap() {
		
		Map<String, String> resultMap = new HashMap<String, String>();
		
		Map<String, String> expIdsMap = redisUtil.hgetAll(RedisKeyConstant.MOSES_EXP_IDS);
		for (Entry<String, String> entry : expIdsMap.entrySet()) {
			String string = redisUtil.getString(RedisKeyConstant.MOSES_EXP_PREFIX + entry.getValue());
			ExpirementDomain expirementDomain = JSONObject.parseObject(string, ExpirementDomain.class);
			resultMap.put(entry.getKey(), entry.getValue()+"-"+expirementDomain.getDomainDes());
		}
		
		
		return resultMap;
	}
	
	// 查询当前tid配置的domainId
	@ApiOperation(value = "查询domain详情")
	@PostMapping(value = "/getDomainDetail")
	public Map<String, DomainDetailInfo> getDomainDetail() {
		
		Map<String, DomainDetailInfo> resultMap = new HashMap<String, DomainDetailInfo>();
		//tid  domainid
		Map<String, String> expIdsMap = redisUtil.hgetAll(RedisKeyConstant.MOSES_EXP_IDS);
		
		for (Entry<String, String> entry : expIdsMap.entrySet()) {

			try {
				//			resultMap.put(entry.getKey(), entry.getValue()+"-"+expirementDomain.getDomainDes());
				String domainId = entry.getValue();
				if (resultMap.containsKey(domainId)) {
					DomainDetailInfo domainDetailInfo = resultMap.get(domainId);
					String expKey = domainDetailInfo.getExpKey();
					expKey += ","+entry.getKey();
					domainDetailInfo.setExpKey(expKey);
				}else{
					String expDomain = redisUtil.getString(RedisKeyConstant.MOSES_EXP_PREFIX + entry.getValue());
					ExpirementDomain expirementDomain = JSONObject.parseObject(expDomain, ExpirementDomain.class);

					DomainDetailInfo domainDetailInfo = new DomainDetailInfo();
					domainDetailInfo.setDes(expirementDomain.getDomainDes());
					domainDetailInfo.setExpKey(entry.getKey());

					String expIds = "";
					for (ExpirementLayer layer : expirementDomain.getLayers()) {
						for (Expirement exp : layer.getExpirements()) {
							expIds += ","+exp.getExpId();
						}
					}
					domainDetailInfo.setExpIds(expIds);
					resultMap.put(domainId, domainDetailInfo);
				}

			}catch(Exception e){
				log.error(e.getMessage());
				continue;
			}
		}
		return resultMap;
	}
	
	// domainId上配置的tid
	@ApiOperation(value = "查询domainId上配置的tid")
	@PostMapping(value = "/getTidsMap")
	public Set<String> getTidsMap(@ApiParam ExpirementParam expparam) {
		Set<String> resultSet = new HashSet<>();
		Map<String, String> expIdsMap = redisUtil.hgetAll(RedisKeyConstant.MOSES_EXP_IDS);
		if (expIdsMap == null || expIdsMap.isEmpty()) {
			return null;
		}

		for (Entry<String, String> entry : expIdsMap.entrySet()) {

			if (entry.getValue().equals(expparam.getId())) {
				resultSet.add(entry.getKey());
			}

		}
		return resultSet;
	}

}