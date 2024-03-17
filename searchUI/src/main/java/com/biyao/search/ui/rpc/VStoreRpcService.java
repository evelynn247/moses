package com.biyao.search.ui.rpc;

import com.alibaba.fastjson.JSONObject;
import com.biyao.mag.dubbo.client.common.PageInfo;
import com.biyao.mag.dubbo.client.common.Result;
import com.biyao.mag.dubbo.client.tob.IDreamWorksShopCommonService;
import com.biyao.mag.dubbo.client.tob.dto.WorksShopDto;
import com.biyao.mag.dubbo.client.tob.dto.WorksShopParamDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author zj
 * @version 1.0
 * @date 2020/6/30 18:01
 * @description
 */
@Service
@Slf4j
public class VStoreRpcService {

    private static final int PAGE_SIZE = 200;

    @Resource
    IDreamWorksShopCommonService dreamWorksShopCommonService;

    public Map<Long, WorksShopDto> getAllWorksShopMap(){

        Map<Long,WorksShopDto> resultMap = new HashMap<>();

        Integer totalPage = 0;
        Integer pageIndex = 1;

        //构造查询参数
        //店铺状态：0:禁用；1:启用
        //审核状态：1:待审核；2:通过；3:驳回
        WorksShopParamDto param = new WorksShopParamDto();
        param.setStatus((byte)1);
        param.setAuditStatus((byte)2);
        param.setPageSize(PAGE_SIZE);

        //分页循环获取店铺数据
        Result<PageInfo<WorksShopDto>> result = null;
        do {
            try {
                //调用rpc接口获取当前页结果
                param.setPageIndex(pageIndex);
                result = dreamWorksShopCommonService.queryShopListByPage(param);
                log.info("获取全量梦工厂店铺数据,当前页码：{}，调用参数：{}",pageIndex, JSONObject.toJSONString(param));
            } catch (Exception e) {
                log.error("[严重异常]获取全量梦工厂店铺数据-系统错误,调用参数：{}，错误信息：{}", JSONObject.toJSONString(param),e);
                pageIndex++;
                continue;
            }
            if (result == null) {
                log.error("[严重异常]获取全量梦工厂店铺数据-获取失败,调用参数：{}，返回结果：null", JSONObject.toJSONString(param));
                pageIndex++;
                continue;
            }
            if (!result.success) {
                log.error("[严重异常]获取全量梦工厂店铺数据-获取失败,调用参数：{}，返回结果：{}", JSONObject.toJSONString(param),JSONObject.toJSONString(result));
                pageIndex++;
                continue;
            }

            PageInfo<WorksShopDto> pageInfoResult = result.getData();
            if (pageInfoResult == null) {
                log.error("[严重异常]获取全量梦工厂店铺数据,当前页码:{},暂无数据,调用参数：{}，返回结果：{}", pageIndex,JSONObject.toJSONString(param),JSONObject.toJSONString(result));
                pageIndex++;
                continue;
            }

            totalPage = pageInfoResult.getTotalPage();
            List<WorksShopDto> dataResult = pageInfoResult.getList();
            if (dataResult == null || dataResult.size() == 0) {
                log.error("[严重异常]获取全量梦工厂店铺数据,当前页码:{},暂无数据,调用参数：{}，返回结果：{}", pageIndex,JSONObject.toJSONString(param),JSONObject.toJSONString(result));
                pageIndex++;
                continue;
            }
            log.info("获取全量梦工厂店铺数据：" + totalPage + ",当前页数:" + pageIndex + ",本页数量:" + dataResult.size());

            dataResult.forEach(item->{
                resultMap.put(item.getRelationUserId(),item);
            });

            pageIndex++;

        } while (pageIndex <= totalPage);

        return resultMap;

    }
}
