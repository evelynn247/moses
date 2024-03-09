package com.biyao.moses.service.imp;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.biyao.moses.common.constant.ErrorCode;
import com.biyao.moses.common.constant.MosesTypeContans;
import com.biyao.moses.common.enums.TemplateTypeEnum;
import com.biyao.moses.model.template.Page;
import com.biyao.moses.model.template.Template;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;
import com.biyao.moses.params.ApiResult;
import com.biyao.moses.service.MosesConfService;
import com.biyao.moses.util.RedisUtil;

/**
 * 配置实现类
 * 
 * @Description
 * @author zyj
 * @Date 2018年9月27日
 */
@Service
public class MosesConfServiceImpl implements MosesConfService {

	private Logger logger = LoggerFactory.getLogger(this.getClass());
	private Logger logger_backup = LoggerFactory.getLogger("moses.backup");

	@Autowired
	private RedisUtil redisUtil;

	private Long templateErrorNum = 0L;

	@Override
	public ApiResult<String> templateAdd(JSONObject json) {
		ApiResult<String> apiResult = new ApiResult<String>();
		try {
			// 获取json数据，以k，v的形式存储
			Integer templateType = json.getInteger(MosesTypeContans.TEMPLATE_TYPE_NAME);
			String templateValue = TemplateTypeEnum.getTemplateNameById(templateType);
			Boolean dynamic = json.getBoolean(MosesTypeContans.DYNAMIC);
			String templateName = json.getString(MosesTypeContans.TEMPLATE_NAME);
			String dataSourceType = json.getString(MosesTypeContans.TEMPLATE_SOURCE_TYPE);

			if (!checkTemplateParam(templateName, templateValue, dynamic, dataSourceType)) {
				logger.error("参数异常,json=" + json);
				apiResult.setSuccess(ErrorCode.PARAM_ERROR_CODE);
				apiResult.setError("参数异常!");
				return apiResult;
			}

			String templateId = MosesTypeContans.MOSES_TEMPLATE_ID
					+ redisUtil.incr(MosesTypeContans.MOSES_TEMPLATE_ID_INCR);
			json.put(MosesTypeContans.TEMPLATE_ID, templateId);
			json.put(MosesTypeContans.TEMPLATE_SOURCE_TYPE, dataSourceType);

			boolean isOk = redisUtil.setString(templateId, json.toString(), -1);
			if (!isOk) {
				logger.error("redisUtil.setString 存储template错误，templateId:" + templateId);
				apiResult.setSuccess(ErrorCode.SYSTEM_ERROR_CODE);
				apiResult.setError("添加失败!");
			} else {
				apiResult.setData(templateId);
				logger.info("添加template成功，templateId:" + templateId);
				logger_backup.info("confType:template | "+"redis.key:"+templateId+" | redis.value:"+json.toString());
			}
		} catch (Exception e) {
			logger.error("添加template失败！", e);
			apiResult.setSuccess(ErrorCode.SYSTEM_ERROR_CODE);
			apiResult.setError("添加失败!");
		}
		return apiResult;
	}

	@Override
	public ApiResult<String> blockAdd(JSONObject json) {
		ApiResult<String> apiResult = new ApiResult<String>();
		try {
			JSONArray templateList = new JSONArray();
			int templateError = 0;
			String blockId = MosesTypeContans.MOSES_BLOCKE_ID + redisUtil.incr(MosesTypeContans.MOSES_BLOCKE_ID_INCR);
			Boolean dynamic = json.getBoolean(MosesTypeContans.DYNAMIC);
			Boolean isFeed = json.getBoolean(MosesTypeContans.IS_FEED);

			if (!checkBlockParams(dynamic, isFeed)) {
				apiResult.setSuccess(ErrorCode.PARAM_ERROR_CODE);
				apiResult.setError("参数为空");
				return apiResult;
			}
			// 循环添加block下的template，返回template的json集合
			JSONArray jsonArray = json.getJSONArray(MosesTypeContans.BLOCK_NAME);
			for (int i = 0; i < jsonArray.size(); i++) {
				JSONObject templateJson = jsonArray.getJSONObject(i);
				// 标题模板，不算在模板总数中
				templateError = editTemplate(templateError, templateList, templateJson);
			}
			json.clear();
			json.put(MosesTypeContans.BLOCKE_ID, blockId);
			json.put(MosesTypeContans.BLOCK_NAME, templateList);
			json.put(MosesTypeContans.DYNAMIC, dynamic);
			json.put(MosesTypeContans.IS_FEED, isFeed);

			boolean isOk = redisUtil.setString(blockId, json.toString(), -1);
			if (!isOk) {
				logger.error("redisUtil.setString 存储block错误，blockID:" + blockId);
				apiResult.setSuccess(ErrorCode.SYSTEM_ERROR_CODE);
				apiResult.setError("添加失败!");
			} else {
				logger.info("添加block成功，blockID:" + blockId);
				logger_backup.info("confType:block | "+"redis.key:"+blockId+" | redis.value:"+json.toString());
				apiResult.setData(blockId);
				apiResult.setError("添加block完成，总计template数量：" + jsonArray.size() + " ,成功:"
						+ (jsonArray.size() - templateError) + ",失败:" + templateError);
				templateErrorNum += templateError;
			}
		} catch (Exception e) {
			logger.error("添加block失败！", e);
			apiResult.setSuccess(ErrorCode.SYSTEM_ERROR_CODE);
			apiResult.setError("添加失败!");
		}
		return apiResult;
	}

	/**
	 * 添加block下template
	 * 
	 * @param templateError
	 * @param block
	 * @param templateJson
	 * @return
	 */
	private int editTemplate(int templateError, JSONArray templateList, JSONObject templateJson) {
		String templateKey = templateJson.getString(MosesTypeContans.TEMPLATE_ID);
		// tid已经存在，直接使用tid模板内容。不存在新增模板
		if (!StringUtils.isEmpty(templateKey)) {
			logger.info("tid:" + templateKey + " 在redis中已经存在，关联模板关系。");
			String template = redisUtil.getString(templateKey);
			if (!StringUtils.isEmpty(template)) {
				templateList.add(JSONObject.parseObject(template));
			}
		} else {
			if (templateAdd(templateJson).getSuccess() != 0) {
				logger.error("添加template失败，模板异常!");
				templateError++;
				return templateError;
			}
			templateList.add(templateJson);
		}
		return templateError;
	}

	@Override
	public ApiResult<String> pageAdd(JSONObject json, String... expId) {
		ApiResult<String> apiResult = new ApiResult<String>();
		templateErrorNum = 0L;
		try {
			int blockError = 0;
			JSONArray blockList = new JSONArray();
			String pageId = MosesTypeContans.MOSES_PAGE_ID + redisUtil.incr(MosesTypeContans.MOSES_PAGE_ID_INCR);
			String pageName = json.getString(MosesTypeContans.PAGE_NAME);
			// bid，仅供批量生成feed流页面使用
			Map<String, String> bidMap = new HashMap<>();
			// 循环添加page下的block，返回bid的集合
			JSONArray jsonArray = json.getJSONArray(MosesTypeContans.BLOCK_LIST_NAME);
			for (int i = 0; i < jsonArray.size(); i++) {
				JSONObject blockJson = jsonArray.getJSONObject(i);
				blockError = editBlock(blockError, blockList, blockJson, pageId, bidMap);
			}
			json.clear();
			json.put(MosesTypeContans.BLOCK_LIST_NAME, blockList);
			json.put(MosesTypeContans.PAGE_ID, pageId);
			json.put(MosesTypeContans.PAGE_NAME, pageName);
			boolean isOk = redisUtil.setString(pageId, json.toString(), -1);
			if (!isOk) {
				logger.error("redisUtil.setString 存储page错误，pageID:" + pageId);
				apiResult.setSuccess(ErrorCode.SYSTEM_ERROR_CODE);
				apiResult.setError("添加失败!");
			} else {
				logger.info("添加page成功，pageID:" + pageId);
				logger_backup.info("confType:page | "+"redis.key:"+pageId+" | redis.value:"+json.toString());

				// 建立pid -> bid 关系
				if (!buildPidRelationBid(pageId, blockList)) {
					logger.error("建立pid:" + pageId + "关系失败！");
				}
				if (expId.length > 0) {
					apiResult.setData(pageId + "|" + bidMap.get("bid") + "_" + expId[0]);
				} else {
					apiResult.setData(pageId);
				}
				apiResult.setError("添加page完成，总计block数量：" + jsonArray.size() + " ,成功:" + (jsonArray.size() - blockError)
						+ ",失败:" + blockError + " ,异常template数量:" + templateErrorNum);

				logger.info("添加page完成，总计block数量：" + jsonArray.size() + ",block失败数量:" + blockError + " ,异常template数量:"
						+ templateErrorNum);
			}
		} catch (Exception e) {
			logger.error("添加page失败！", e);
			apiResult.setSuccess(ErrorCode.SYSTEM_ERROR_CODE);
			apiResult.setError("添加失败!");
		}
		return apiResult;
	}

	/**
	 * 添加blockList下的block
	 * 
	 * @param blockError
	 * @param blockList
	 * @param blockJson
	 * @return
	 */
	private int editBlock(int blockError, JSONArray blockList, JSONObject blockJson, String pid,
			Map<String, String> bidMap) {
		String blockKey = blockJson.getString(MosesTypeContans.BLOCKE_ID);
		// bid已经存在，直接使用bid数据，不存在则新增bid
		if (!StringUtils.isEmpty(blockKey)) {
			logger.info("bid:" + blockKey + " 在redis中已经存在，关联区块关系。");
			String blockMap = redisUtil.getString(blockKey);
			if (!StringUtils.isEmpty(blockMap)) {
				blockList.add(JSON.parseObject(blockMap));
			}
		} else {
			if (blockAdd(blockJson).getSuccess() != 0) {
				logger.error("添加block失败，异常数据:", blockJson);
				blockError++;
				return blockError;
			}
			blockList.add(blockJson);
			blockKey = blockJson.getString(MosesTypeContans.BLOCKE_ID);
		}
		bidMap.put("bid", blockKey);

		// 建立 bid -> pid 关系
		if (!buildBidRelationPid(blockKey, pid)) {
			logger.error("建立bid:" + blockKey + "关系失败!");
		}

		return blockError;
	}

	@Override
	public ApiResult<List<Template<TotalTemplateInfo>>> queryBlockByExpId(String blockId, String expId) {
		ApiResult<List<Template<TotalTemplateInfo>>> apiResult = new ApiResult<List<Template<TotalTemplateInfo>>>();
		try {
			blockId = blockId + MosesTypeContans.EXP_ID_SUF;
			Object object = redisUtil.hgetStr(blockId, expId);
			if (StringUtils.isEmpty(object)) {
				logger.info("根据bid和expId查询Template列表无数据，参数{},{}", blockId, expId);
				apiResult.setSuccess(ErrorCode.SYSTEM_ERROR_CODE);
				apiResult.setError("无数据!");
				return apiResult;
			}
			List<Template<TotalTemplateInfo>> templateList = JSONObject.parseObject(object.toString(),
					new TypeReference<List<Template<TotalTemplateInfo>>>() {
					});
			apiResult.setData(templateList);
		} catch (Exception e) {
			logger.error("查询Template列表异常！", e);
			apiResult.setSuccess(ErrorCode.SYSTEM_ERROR_CODE);
			apiResult.setError("查询异常!");
		}
		return apiResult;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public ApiResult<Page> queryPageById(String pid) {
		ApiResult<Page> apiResult = new ApiResult<Page>();
		try {
			String pageObject = redisUtil.getString(pid);
			Page page = JSONObject.parseObject(pageObject, new TypeReference<Page<TotalTemplateInfo>>() {
			});
			apiResult.setData(page);
		} catch (Exception e) {
			logger.error("查询page页面错误！", e);
			apiResult.setSuccess(ErrorCode.SYSTEM_ERROR_CODE);
			apiResult.setError("查询异常!");
		}
		return apiResult;

	}

	@Override
	public ApiResult<String> updateBlock(JSONObject json) {
		ApiResult<String> apiResult = new ApiResult<String>();
		try {
			String blockId = json.getString(MosesTypeContans.BLOCKE_ID);
			JSONArray jsonArray = json.getJSONArray(MosesTypeContans.BLOCK_NAME);
			Boolean dynamic = json.getBoolean(MosesTypeContans.DYNAMIC);
			Boolean isFeed = json.getBoolean(MosesTypeContans.IS_FEED);

			if (!checkBlockParams(dynamic, isFeed)) {
				apiResult.setSuccess(ErrorCode.PARAM_ERROR_CODE);
				apiResult.setError("参数为空");
				return apiResult;
			}

			int templateError = 0;
			JSONArray templateList = new JSONArray();
			for (int i = 0; i < jsonArray.size(); i++) {
				JSONObject templateJson = jsonArray.getJSONObject(i);
				// 标题模板，不算在模板总数中
				templateError = this.editTemplate(templateError, templateList, templateJson);
			}
			json.clear();
			json.put(MosesTypeContans.BLOCKE_ID, blockId);
			json.put(MosesTypeContans.BLOCK_NAME, templateList);
			json.put(MosesTypeContans.DYNAMIC, dynamic);
			json.put(MosesTypeContans.IS_FEED, isFeed);

			boolean isOk = redisUtil.setString(blockId, json.toString(), -1);
			if (!isOk) {
				logger.error("修改block失败，redisUtil.setString失败，blockId={}", blockId);
				apiResult.setSuccess(ErrorCode.SYSTEM_ERROR_CODE);
				apiResult.setError("修改失败!");
			} else {
				// 查询bid对应的pid关系。根据pid-> bid关系重新更新数据
				if (!updatePidRelationBid(blockId)) {
					logger.error("修改blockId:" + blockId + " 对应关系失败");
				}
				apiResult.setData(blockId);
				apiResult.setError("修改block完成，总计template数量：" + jsonArray.size() + " ,成功:"
						+ (jsonArray.size() - templateError) + ",失败:" + templateError);
				logger.info("修改block成功,blockId=" + blockId + "，总计template数量：" + jsonArray.size() + " ,成功:"
						+ (jsonArray.size() - templateError) + ",失败:" + templateError);
				logger_backup.info("confType:block | "+"redis.key:"+blockId+" | redis.value:"+json.toString());
			}
		} catch (Exception e) {
			logger.error("修改block失败", e);
			apiResult.setSuccess(ErrorCode.SYSTEM_ERROR_CODE);
			apiResult.setError("修改失败");
		}
		return apiResult;

	}

	/**
	 * 校验block参数
	 * 
	 * @param isExp
	 * @param dynamic
	 * @param isFeed
	 * @param expId
	 * @return
	 */
	private boolean checkBlockParams(Boolean dynamic, Boolean isFeed) {

		if (StringUtils.isEmpty(dynamic) || StringUtils.isEmpty(isFeed)) {
			return false;
		}
		return true;

	}

	/**
	 * 校验template参数
	 * 
	 * @param templateName
	 * @param templateValue
	 * @param dynamic
	 * @return
	 */
	private boolean checkTemplateParam(String templateName, String templateValue, Boolean dynamic,
			String dataSourceType) {

		boolean falg = true;
		if (StringUtils.isEmpty(templateValue)) {
			logger.error("未识别template类型," + templateValue);
			falg = false;
		}
		if (!templateValue.equals(templateName)) {
			logger.error("template类型与template名称不匹配," + templateValue);
			falg = false;
		}
		if (StringUtils.isEmpty(templateName) || StringUtils.isEmpty(dynamic)) {
			falg = false;
		}
		if (StringUtils.isEmpty(dataSourceType)) {
			// 空白模板和分割线模板不需要数据源
			if ("blockline".equals(templateName) || "separateLine".equals(templateName)
					|| "switchTab".equals(templateName)) {
				falg = true;
			} else {
				falg = false;
			}
		}
		return falg;
	}

	@Override
	public ApiResult<String> matchAdd(JSONObject json) {
		ApiResult<String> apiResult = new ApiResult<String>();
		JSONArray matchObject = json.getJSONArray("data");
		String matchId = json.getString("matchId");
		boolean isOk = redisUtil.setString(matchId, matchObject.toString(), -1);
		apiResult.setError(matchId);
		if (!isOk) {
			apiResult.setSuccess(ErrorCode.SYSTEM_ERROR_CODE);
			apiResult.setError("添加match失败");

		}
		return apiResult;
	}

	/**
	 * 建立pid -> bid 关系
	 * 
	 * @param pid
	 * @param blockList
	 * @return
	 */
	private boolean buildPidRelationBid(String pid, JSONArray blockList) {
		// 以hash的形式存储,一级key为pid，2级key为bid
		pid = pid + MosesTypeContans.RELATION;
		// 先删除，再新建
		redisUtil.del(pid);
		for (int i = 0; i < blockList.size(); i++) {
			String bid = blockList.getJSONObject(i).getString(MosesTypeContans.BLOCKE_ID);
			bid = bid + MosesTypeContans.RELATION;
			Map<String, String> blockMap = new HashMap<>();
			String index = i + "";
			blockMap.put(bid, index);
			if (redisUtil.hmset(pid, blockMap)) {
				logger.info("建立pid:{} -> bid:{} 关系完成", pid, bid);
			} else {
				logger.error("建立pid:{} -> bid:{} 关系失败！", pid, bid);
				return false;
			}
		}
		return true;
	}

	/**
	 * 建立bid -> pid关系
	 * 
	 * @param bid
	 * @param pid
	 * @return
	 */
	private boolean buildBidRelationPid(String bid, String pid) {
		pid = pid + MosesTypeContans.RELATION;
		bid = bid + MosesTypeContans.RELATION;
		Object object = redisUtil.hgetStr(bid, pid);
		// 关系不存在，建立关系，已存在则忽略
		if (StringUtils.isEmpty(object)) {
			Map<String, String> pageMap = new HashMap<>();
			pageMap.put(pid, pid);
			if (redisUtil.hmset(bid, pageMap)) {
				logger.info("建立bid:{} -> pid:{} 关系完成", bid, pid);
			} else {
				logger.error("建立bid:{} -> pid:{} 关系失败！", bid, pid);
				return false;
			}
		}
		return true;
	}

	/**
	 * 修改bid对应pid的json数据内容
	 * 
	 * @param bid
	 * @return
	 */
	private boolean updatePidRelationBid(String bid) {
		// 查询bid对应的pid关系
		// 数据结构hash 一级key：bid-relation ，二级key：pid-relation ，二级value：pid-relation
		bid = bid + MosesTypeContans.RELATION;
		Map<String, String> bidMap = redisUtil.hgetAll(bid);
		for (Entry<String, String> bidEntry : bidMap.entrySet()) {
			String pid = bidEntry.getKey();

			// 先查询page下的bid
			JSONArray jsonArray = new JSONArray();
			// 根据pid-> bid关系,重新更新page下的json数据
			// 数据结构hash，一级key：pid-relation，二级key，bid-relation，二级value：此bid在pid中的顺序，从0开始递增
			Map<String, String> pidMap = redisUtil.hgetAll(pid);

			// 查询pid下的json数据内容
			pid = pid.split("-")[0];
			String pageObject = redisUtil.getString(pid);
			JSONObject jsonobject = JSONObject.parseObject(pageObject);

			// 重新组装pid下bid数据内容
			for (Entry<String, String> pidEntry : pidMap.entrySet()) {
				// bid
				String bidRela = pidEntry.getKey();
				// 对应的顺序
				Integer index = Integer.valueOf(pidEntry.getValue());
				String json = redisUtil.getString(bidRela.split("-")[0]);
				jsonArray.set(index, JSONObject.parseObject(json));
			}
			jsonobject.put(MosesTypeContans.BLOCK_LIST_NAME, jsonArray);
			if (redisUtil.setString(pid, jsonobject.toString(), -1)) {
				logger.info("修改pid:{} 页面json数据完成", pid);
			} else {
				logger.error("修改pid:{} 页面json数据失败！", pid);
				return false;
			}

		}
		return true;
	}

	@Override
	public ApiResult<String> updatePage(JSONObject json) {
		ApiResult<String> apiResult = new ApiResult<String>();
		String pid = json.getString(MosesTypeContans.PAGE_ID);
		try {
			String pageName = json.getString(MosesTypeContans.PAGE_NAME);
			int blockError = 0;
			JSONArray blockList = new JSONArray();
			Map<String, String> bidMap = new HashMap<>();
			// 循环添加page下的block，返回bid的集合
			JSONArray jsonArray = json.getJSONArray(MosesTypeContans.BLOCK_LIST_NAME);
			for (int i = 0; i < jsonArray.size(); i++) {
				JSONObject blockJson = jsonArray.getJSONObject(i);
				blockError = editBlock(blockError, blockList, blockJson, pid, bidMap);
			}
			json.clear();
			json.put(MosesTypeContans.BLOCK_LIST_NAME, blockList);
			json.put(MosesTypeContans.PAGE_ID, pid);
			json.put(MosesTypeContans.PAGE_NAME, pageName);
			boolean isOk = redisUtil.setString(pid, json.toString(), -1);
			if (!isOk) {
				logger.error("redisUtil.setString 存储page错误，pageID:" + pid);
				apiResult.setSuccess(ErrorCode.SYSTEM_ERROR_CODE);
				apiResult.setError("修改失败!");
			} else {
				logger.info("修改page成功，pageID:" + pid);
				logger_backup.info("confType:page | "+"redis.key:"+pid+" | redis.value:"+json.toString());
				// 建立pid -> bid 关系
				buildPidRelationBid(pid, blockList);
				apiResult.setData(pid);
				apiResult.setError("修改page完成，总计block数量：" + jsonArray.size() + " ,成功:" + (jsonArray.size() - blockError)
						+ ",失败:" + blockError);

				logger.info("修改page完成，总计block数量：" + jsonArray.size() + ",block失败数量:" + blockError);
			}
		} catch (Exception e) {
			logger.error("修改page:" + pid + " 失败!", e);
			apiResult.setSuccess(ErrorCode.SYSTEM_ERROR_CODE);
			apiResult.setError("修改失败!");
		}
		return apiResult;
	}

	@Override
	public ApiResult<String> buildPageByPageName(String pageName, Integer templateType, String expId) {
		ApiResult<String> apiResult = new ApiResult<String>();
		// 只支持feed流
		if (templateType.equals(14) || templateType.equals(17) || templateType.equals(18)) {
			String templateName = TemplateTypeEnum.getTemplateNameById(templateType);
			// feedType feed流类型，单排双排三排
			String pageJson = "{\"blockList\":[{\"expId\":\"" + expId
					+ "\",\"exp\":false,\"dynamic\":true,\"feed\":true,\"block\":[{\"templateType\":" + templateType
					+ ",\"templateName\":\"" + templateName
					+ "\",\"dynamic\":true,\"data\":[{\"routerType\":7,\"mainTitleColor\":\"#FFFFFF\""
					+ ",\"subtitleColor\":\"#FB4C81\",\"priceColor\":\"#FB4C81\"}]}]}],\"pageName\":\"" + pageName
					+ "\"}";

			JSONObject parseObject = JSONObject.parseObject(pageJson);
			ApiResult<String> pageResult = pageAdd(parseObject, expId);
			if (pageResult.getSuccess() == 0) {
				String resultData = pageResult.getData();
				apiResult.setData(resultData);
			}

		} else {
			apiResult.setSuccess(ErrorCode.PARAM_ERROR_CODE);
			apiResult.setError("参数类型错误!");
		}
		return apiResult;
	}

}
