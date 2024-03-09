package com.biyao.moses.controller;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSON;
import com.biyao.moses.common.constant.ErrorCode;
import com.biyao.moses.common.constant.RedisKeyConstant;
import com.biyao.moses.common.enums.OpenSourceEnum;
import com.biyao.moses.params.ApiResult;
import com.biyao.moses.util.RedisUtil;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;

/**
 * @Description 页面配置管理
 * @author zyj
 * @Date 2018年10月30日
 */
@RestController
@Api("moses页面配置")
@RequestMapping(value = "/moses/pageConf")
@Slf4j
public class MosesPageConfController {
	@Autowired
	private RedisUtil redisUtil;

	private Logger logger_backup = LoggerFactory.getLogger("moses.backup");

	/**
	 * 添加版本控制 一级key：moses:source 二级key：IOS，ANDROID,MWEB,MINIAPP
	 * value：minAvn-maxAvn|source,minAvn-maxAvn|source
	 * 需要新增渠道和来源下拉选择框时，追加allowableValues参数值即可
	 * hfc 首页前台类目  afc 分类页前台类目
	 * @param request
	 * @return
	 */
	@ApiOperation(value = "1-添加版本控制")
	@PostMapping(value = "/versionAdd")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "source", value = "S1，个人中心;HR 首页推荐，HP 首页，NHP新人首页,类目,新手专享页面1,轮播图,主搜预制页和分类页,DDLB-订单列表;SPSC-商品收藏;DPGZ-店铺关注;YE-余额;PTFW-平台服务;XSZXYD-新手专享数据源1单排,XSZXYS-新手专享数据源1双排;TYTQJ-通用特权金单排;YXGJ-营销工具;GGK-刮刮卡;NYXGJ-营销工具1.3;MTGY-媒体共赢链;MTGY2-媒体共赢链老客首页feed流数据;M2F1-买二返一频道页热门;GXQSP-感兴趣商品聚合页", dataType = "String", paramType = "query", allowableValues = "S1,TQJS1,HR,HP,NHP,CATEGORY,XSZX1,HFC,AFC,LBT,ZSYZY,PRICATEGORY,DDLB,SPSC,DPGZ,YE,PTFW,FLBT,ZSYZYOLD,XSZXYD,XSZXYS,TYTQJ,YXGJ,GGK,NYXGJ,MTGY,MTGY2,M2F1,GXQSP", required = true),
			@ApiImplicitParam(name = "platform", value = "平台，IOS，ANDROID,MWEB,MINIAPP", dataType = "String", paramType = "query", allowableValues = "IOS,ANDROID,MWEB,MINIAPP", required = true),
			@ApiImplicitParam(name = "verNum", value = "对应版本号 1-9999,10000-20000 逗号分隔", dataType = "String", paramType = "query", required = true),
			@ApiImplicitParam(name = "version", value = "控制版本HP,HR,HPTOGETHER 逗号分隔", dataType = "String", paramType = "query", required = true)})
	public ApiResult<String> versionAdd(HttpServletRequest request) {
		ApiResult<String> apiResult = new ApiResult<String>();
		try {
			String source = request.getParameter("source").toUpperCase().trim();
			String pf = request.getParameter("platform").toUpperCase().trim();
			String verNum = request.getParameter("verNum").toUpperCase().trim();
			String version=request.getParameter("version").toUpperCase().trim();
			
			StringBuffer sbfStr = new StringBuffer();
			String[] verSplit = verNum.split(",");
			
			String[] versionSplit=version.split(",");
			if(verSplit.length!=versionSplit.length) {
				apiResult.setError("数据格式错误,版本和对应的版本号数量不一致!");
				apiResult.setSuccess(ErrorCode.PARAM_ERROR_CODE);
				return apiResult;
			}
			for (int i = 0; i < verSplit.length; i++) {
				sbfStr.append(verSplit[i] + "|" + versionSplit[i]);
				if ((i + 1) != verSplit.length) {
					sbfStr.append(",");
				}
			}
			Long num = redisUtil.hset(RedisKeyConstant.MOSES_SOURCE_PREFIX + source.trim(), pf.trim(), sbfStr.toString());
			logger_backup.info("confType:version | "+"redis.Key:"+RedisKeyConstant.MOSES_SOURCE_PREFIX + source.trim()+" | redis.Field:"+pf.trim() +" | redis.Value:" + sbfStr.toString());
			apiResult.setData(String.valueOf(num));
			apiResult.setError("添加成功");
		} catch (Exception e) {
			log.error("添加版本失败", e);
			apiResult.setError("数据格式错误!");
			apiResult.setSuccess(ErrorCode.PARAM_ERROR_CODE);
			return apiResult;
		}

		return apiResult;
	}

	/**
	 * 添加页面开关设置 一级key固定：mosesswt:avn ；二级key：version value：开关|pageId，topicId
	 * 
	 * @param request
	 * @return
	 */
	@ApiOperation(value = "2-添加页面开关")
	@PostMapping(value = "/switchOnOffAdd")
	@ApiImplicitParams({
			// @ApiImplicitParam(name = "source", value = "s1，个人中心推荐页;HR 首页推荐，HP 首页",
			// dataType = "String", paramType = "query", required = true),
			@ApiImplicitParam(name = "version", value = "对应控制版本", dataType = "String", paramType = "query", required = true),
			@ApiImplicitParam(name = "onoff", value = "开关设置", dataType = "boolean", paramType = "query", required = true),
			@ApiImplicitParam(name = "pageId", value = "对应页面id;moses:pid_3", dataType = "String", paramType = "query", required = true),
			@ApiImplicitParam(name = "topicId", value = "数据源id", dataType = "String", paramType = "query") })
	public ApiResult<String> switchOnOffAdd(HttpServletRequest request) {
		ApiResult<String> apiResult = new ApiResult<String>();

		try {
			String version = request.getParameter("version").toUpperCase().trim();
			String onoff = request.getParameter("onoff");
			String pageId = request.getParameter("pageId").trim();
			String topicId = request.getParameter("topicId");
			if (OpenSourceEnum.getOpenPage(version)) {
				onoff = "true";
			}
			String value = "";
			if (StringUtils.isEmpty(topicId)) {
				value = onoff + "|" + pageId;
			} else {
				value = onoff + "|" + pageId + "," + topicId;
			}
//			Map<String, String> hgetAll = redisUtil.hgetAll(RedisKeyConstant.MOSES_SOURCE_PREFIX + version);
//			if (hgetAll == null || hgetAll.isEmpty()) {
//				apiResult.setError("没有对应的版本version!");
//				apiResult.setSuccess(ErrorCode.PARAM_ERROR_CODE);
//				return apiResult;
//			}
			Long num = redisUtil.hset(RedisKeyConstant.MOSES_SWITCH_KEY, version, value);
			logger_backup.info("confType:switch | "+"redis.Key:"+RedisKeyConstant.MOSES_SWITCH_KEY+" | redis.Field:"+version +" | redis.Value:" + value);
			apiResult.setData(String.valueOf(num));
			apiResult.setError("添加成功");
		} catch (Exception e) {
			log.error("添加版本失败", e);
			apiResult.setError("数据格式错误!");
			apiResult.setSuccess(ErrorCode.PARAM_ERROR_CODE);
			return apiResult;
		}

		return apiResult;
	}

	@ApiOperation(value = "查询页面开关设置详情")
	@PostMapping(value = "/queryPageSwitch")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "redisKey", value = "查询页面开关 mosesswt:avn; 页面版本控制 mosessou:HR", dataType = "String", paramType = "query", required = true) })
	public ApiResult<String> queryPageSwitch(HttpServletRequest request) {
		ApiResult<String> apiResult = new ApiResult<String>();
		try {

			Map<String, String> hgetAll = redisUtil.hgetAll(request.getParameter("redisKey").trim());
			apiResult.setData(JSON.toJSONString(hgetAll));

		} catch (Exception e) {
			log.error("查询页面开关设置详情失败", e);
			apiResult.setError("数据格式错误!");
			apiResult.setSuccess(ErrorCode.PARAM_ERROR_CODE);
			return apiResult;
		}

		return apiResult;
	}

}
