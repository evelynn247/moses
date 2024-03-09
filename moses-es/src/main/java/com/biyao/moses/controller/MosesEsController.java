package com.biyao.moses.controller;

import com.biyao.moses.es.EsRequest;
import com.biyao.moses.match.ApiResult;
import com.biyao.moses.service.ProductEsServiceImpl;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

import static com.biyao.moses.common.ErrorCode.PARAM_ERROR_CODE;

/**
 * @program: moses-parent-online
 * @description:
 * @author: changxiaowei
 * @create: 2021-09-17 18:02
 **/
@Slf4j
@RestController
@RequestMapping(value = "/recommend/es")
public class MosesEsController {
    @Autowired
    ProductEsServiceImpl productEsService;
    @PostMapping ("/updateIndex")
    public ApiResult<Object> updateIndex(@ApiParam EsRequest esRequest){
        ApiResult<Object> result = new ApiResult<>();
        if(!esRequest.isValid()){
            result.setSuccess(PARAM_ERROR_CODE);
            result.setError("参数错误");
            return result;
        }
        if(!StringUtils.isEmpty(esRequest.getPids())){
            String[] split = esRequest.pids.split(",");
            List<Long> pids = new ArrayList<>();
            for (int i = 0; i < split.length; i++) {
                try {
                    pids.add(Long.valueOf(split[i]));
                }catch (Exception e){
                    log.error("[严重异常]传参异常,",e);
                }
            }
            productEsService.updateIndexByPids(pids);
        }else {
            productEsService.updateIndexByTime(esRequest.getTime());
        }
        return result;
    }
}
