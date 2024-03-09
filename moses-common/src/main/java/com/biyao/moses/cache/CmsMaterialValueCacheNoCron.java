package com.biyao.moses.cache;

import com.alibaba.fastjson.JSON;
import com.biyao.cms.client.common.bean.Result;
import com.biyao.cms.client.material.dto.MaterialElementBaseDTO;
import com.biyao.cms.client.material.dto.MaterialQueryParamDTO;
import com.biyao.cms.client.material.service.IMaterialQueryDubboService;
import com.biyao.moses.common.enums.CmsMaterialEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class CmsMaterialValueCacheNoCron {

    private Map<Long, MaterialElementBaseDTO> cache = new HashMap<>();

    @Resource
    IMaterialQueryDubboService materialQueryDubboService;



    protected void init() {
        refresh();
    }

    /**
     * 刷新缓存内容
     *
     */
    protected void refresh(){
        List<Long> materialIdList = new ArrayList<>();
        for(CmsMaterialEnum cmsMaterialEnum : CmsMaterialEnum.values()){
            if(cmsMaterialEnum == null || cmsMaterialEnum.getId() == null){
                continue;
            }
            materialIdList.add(cmsMaterialEnum.getId());
        }
        if(CollectionUtils.isEmpty(materialIdList)){
            log.error("[严重异常][CMS素材]素材ID为空");
            return;
        }
        MaterialQueryParamDTO materialQueryParamDTO = new MaterialQueryParamDTO();
        materialQueryParamDTO.setMaterialIdIn(materialIdList);
        materialQueryParamDTO.setCaller("moses");
        Map<Long, MaterialElementBaseDTO> data = null;
        try {
            Result<Map<Long, MaterialElementBaseDTO>> result = materialQueryDubboService.queryMaterial(materialQueryParamDTO);
            if(result == null){
                log.error("[严重异常][CMS素材]获取CMS素材值出现错误，返回值为null");
            }else{
                if(result.isSuccess() && result.getData() != null){
                    data = result.getData();
                }else{
                    log.error("[严重异常][CMS素材]获取CMS素材值出现错误，request {}， result {}",JSON.toJSONString(materialQueryParamDTO), JSON.toJSONString(result));
                }
            }

        }catch (Exception e){
            log.error("[严重异常][CMS素材]获取CMS素材值出现异常，request {}，", JSON.toJSONString(materialQueryParamDTO), e);
        }
        cache = data;
    }

    public MaterialElementBaseDTO getValue(Long materialId) {
        if(cache == null || materialId == null){
            return null;
        }
        return cache.get(materialId);
    }
}
