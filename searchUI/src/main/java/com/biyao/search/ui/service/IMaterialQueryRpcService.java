package com.biyao.search.ui.service;


import com.biyao.cms.client.material.dto.MaterialElementBaseDTO;
import com.biyao.cms.client.material.dto.MaterialQueryParamDTO;
import java.util.Map;

/**
 * @ClassName: IMaterialQueryRpcService
 * @Description: cms相关素材配置查询接口类
 * @author yangy
 * @date 16:36 2019/4/4
 */
public interface IMaterialQueryRpcService {

    /**
     * @Description: 根据素材id查询对应素材信息
     * @author yangy
     * @Date 16:36 2019/4/4
     * @param materialQueryParamDTO 素材id
     */
    Map<Long, MaterialElementBaseDTO> queryMaterial(MaterialQueryParamDTO materialQueryParamDTO) throws Exception;
    
}
