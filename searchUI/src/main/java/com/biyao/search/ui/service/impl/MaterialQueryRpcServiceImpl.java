package com.biyao.search.ui.service.impl;

import com.alibaba.fastjson.JSON;
import com.biyao.cms.client.common.bean.Result;
import com.biyao.cms.client.material.dto.MaterialElementBaseDTO;
import com.biyao.cms.client.material.dto.MaterialQueryParamDTO;
import com.biyao.cms.client.material.service.IMaterialQueryDubboService;
import com.biyao.search.ui.service.IMaterialQueryRpcService;
import com.biyao.search.ui.util.ConstantUtil;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author Administrator
 * @ClassName: ${CLASS}
 * @Description: **
 * @date 2019/4/4 16:37
 */
@Service
public class MaterialQueryRpcServiceImpl implements IMaterialQueryRpcService {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private IMaterialQueryDubboService materialQueryDubboService;

	@Override
	public Map<Long, MaterialElementBaseDTO> queryMaterial(MaterialQueryParamDTO materialQueryParamDTO) {

		Result<Map<Long, MaterialElementBaseDTO>> result = null;
		try {
			materialQueryParamDTO.setCaller(ConstantUtil.SERVICE_NAME);
			result = materialQueryDubboService.queryMaterial(materialQueryParamDTO);
		} catch (Exception e) {
			logger.error("[严重异常]调用cms服务materialQueryDubboService.queryMaterial查询素材信息异常,参数param: "
					+ JSON.toJSONString(materialQueryParamDTO), e);
		}
		if (result == null) {
			logger.error("[严重异常]调用cms服务materialQueryDubboService.queryMaterial查询素材信息异常，返回result为null,参数param: "
					+ JSON.toJSONString(materialQueryParamDTO));
		} else if (!result.isSuccess()) {
			logger.error("[严重异常]调用cms服务materialQueryDubboService.queryMaterial查询素材信息 业务异常,参数param: "
					+ JSON.toJSONString(materialQueryParamDTO) + " ;结果result：" + JSON.toJSONString(result));
		}
		return result.getData();
	}
}
