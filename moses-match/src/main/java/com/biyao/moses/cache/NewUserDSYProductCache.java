package com.biyao.moses.cache;

import com.alibaba.fastjson.JSON;
import com.biyao.cms.client.common.bean.Result;
import com.biyao.cms.client.newcustomer.dto.LabelStatusDto;
import com.biyao.cms.client.newcustomer.service.IExclusiveService;
import com.biyao.moses.common.constant.MatchRedisKeyConstant;
import com.biyao.moses.util.MatchRedisUtil;
import com.biyao.tag.client.productTag.IProductBaseTagService;
import com.biyao.tag.dto.MarkedQueryDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;

@Component
@EnableScheduling
@Slf4j
public class NewUserDSYProductCache {

    @Autowired
    private MatchRedisUtil matchRedisUtil;

    @Autowired
    private IExclusiveService exclusiveService;

    @Autowired
    private IProductBaseTagService productBaseTagService;

    //过期时间为30天
    private static final int expireTime = 2592000; //30 * 24 * 3600;

    @PostConstruct
    protected void init(){
        refreshNewUserSpecialProducts();
    }

    /**
     * 每天凌晨3点刷新一次redis缓存，刷新时间与排行系统保持一致
     */
    @Scheduled(cron = "0 0 3 * * ?")
    protected void refreshNewUserSpecialProducts(){
        log.info("[任务进度][新手专享]获取运营配置的新手专享商品开始");
        long start = System.currentTimeMillis();
        Long tagId = queryNewuserSpecialTagId();
        if(tagId == null){
            log.error("[严重异常][新手专享]新手专享标签为空");
            return;
        }
        try {
            List<Long> products = queryProductTagMarked(tagId);
            if (products != null && products.size()>0) {
                StringBuilder value = new StringBuilder();
                for(Long product : products){
                    value.append(product).append(",");
                }
                //删除最后一个逗号
                value.deleteCharAt(value.length()-1);
                matchRedisUtil.setString(MatchRedisKeyConstant.MOSES_NEWUSER_SPECIAL_PRODUCTS, value.toString(), expireTime);
                log.info("[任务进度][新手专享]获取运营配置的新手专享商品结束，耗时{}ms，商品个数{}", System.currentTimeMillis()-start, products.size());
            }
        }catch(Exception e){
            log.error("[严重异常][新手专享]获取运营配置的新手专享商品集合失败，", e);
        }
    }

    /**
     * 查询新手专享的tagId
     * @return
     */
    protected Long queryNewuserSpecialTagId() {
        Result<LabelStatusDto> result = null;
        Long newUserSpecialTagId = null;
        try {
            result = exclusiveService.getLabelStatus();
        }catch(Exception e){
            log.error("[严重异常][新手专享]查询新手专享的标签异常，", e);
        }

        if(result != null && result.isSuccess()){
            newUserSpecialTagId = result.getData().getTagId();
        }else if(result != null && !result.isSuccess()){
            log.error("[严重异常][新手专享]查询新手专享的标签失败，result {}", JSON.toJSONString(result));
        }
        return newUserSpecialTagId;
    }

    /**
     * 通过tagId，获取该tagId下的所有商品
     * @param tagId
     * @return
     */
    private List<Long> queryProductTagMarked(Long tagId){
        MarkedQueryDTO param = new MarkedQueryDTO();
        param.setTagId(tagId);
        com.biyao.tag.dto.Result<List<Long>> result = null;
        try{
            result = productBaseTagService.queryProductTagMarked(param);
        }catch(Exception ex){
            log.error("[严重异常][新手专享]查询SCM新手专享标签下商品集合异常,标签Id {},",tagId, ex);
        }

        if(result != null && !result.getSuccess()){
            log.error("[严重异常][新手专享]查询SCM新手专享标签下商品集合失败,标签Id {},result {},",tagId, JSON.toJSONString(result));
            return null;
        }else if(result != null && result.getSuccess()){
            return result.getObj();
        }else{
            return null;
        }
    }
}
