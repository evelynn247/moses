package com.biyao.moses.rpc;

import com.alibaba.fastjson.JSON;
import com.biyao.cms.client.common.bean.Result;
import com.biyao.cms.client.material.dto.MaterialElementBaseDTO;
import com.biyao.cms.client.material.dto.MaterialElementRadioDTO;
import com.biyao.cms.client.material.dto.MaterialQueryParamDTO;
import com.biyao.cms.client.material.service.IMaterialQueryDubboService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;

/**
 * Created by GuoYue on 2019/5/22.
 */
@Slf4j
@Service
public class CmsRpcService {

    @Resource
    private IMaterialQueryDubboService materialQueryDubboService;

    /**
     * 查询商品编辑器开关状态
     * 返回值
     *  0：无结果，降级操作
     *  1: 一起拼编辑器
     *  2：普通编辑器
     * @return
     */
    public int queryProductEditorSwitch(){

        MaterialQueryParamDTO param = new MaterialQueryParamDTO();
        param.setMaterialIdIn(new ArrayList<>());
        param.getMaterialIdIn().add(10050104L);
        param.setCaller("mosesmatch.biyao.com");

        Result<Map<Long, MaterialElementBaseDTO>> rs = null;

        try {
            rs = this.materialQueryDubboService.queryMaterial(param);
        } catch (Exception e) {
            log.error("[严重异常]从cms获取编号为10050104的素材时发生异常, param:{}", JSON.toJSONString(param));
            return 0;
        }

        if(rs.success == false){
            log.error("[严重异常]从cms获取编号为10050104的素材时失败, param:{}, rs:{}", JSON.toJSONString(param), JSON.toJSONString(rs));
            return 0;
        }
        MaterialElementBaseDTO materialElementBaseDTO = rs.getData().get(10050104L);
        if(materialElementBaseDTO == null){
            return 0;
        }
        if(materialElementBaseDTO instanceof MaterialElementRadioDTO ){
            return Optional.ofNullable( ((MaterialElementRadioDTO)materialElementBaseDTO).getValue()).orElse(0);
        }
        return 0;
    }

}
