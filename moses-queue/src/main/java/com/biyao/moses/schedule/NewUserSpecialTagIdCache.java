package com.biyao.moses.schedule;

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
@Slf4j
@EnableScheduling
public class NewUserSpecialTagIdCache {

    /**
     * 新手专享tagId
     */
    private Long newUserSpecialTagId;

    @Autowired
    private MatchRedisUtil matchRedisUtil;

    @Autowired
    private IExclusiveService exclusiveService;

    @Autowired
    private IProductBaseTagService productBaseTagService;

    //过期时间为30天
    private static final int expireTime =2592000; // 30 * 24 * 3600

    @PostConstruct
    protected void init(){

        refreshNewuserSpecialTagId();
    }

    /**
     * 刷新新手专享的tagId，每隔1分钟刷新一次
     */
    @Scheduled(cron = "0 0/1 * * * ?")
    protected void refreshNewuserSpecialTagId() {
        Result<LabelStatusDto> result = null;
        try {
            result = exclusiveService.getLabelStatus();
            if( result == null ||
                    !result.isSuccess() ||
                    result.getData() == null ||
                    result.getData().getTagId() == null){
                return;
            }
            //如果tagId发生了变化，则重新查询新手专享商品信息，然后刷新Redis缓存
            long tagId = result.getData().getTagId();
            if(newUserSpecialTagId == null
                    || newUserSpecialTagId.intValue() != tagId){

                newUserSpecialTagId = tagId;
                List<Long> products = queryProductTagMarked(newUserSpecialTagId);
                if(products != null){
                    StringBuilder value = new StringBuilder();
                    for(Long product : products){
                        value.append(product+",");
                    }
                    //删除最后一个逗号
                    if(value.length()>0) {
                        value.deleteCharAt(value.length() - 1);
                    }
                    matchRedisUtil.setString(MatchRedisKeyConstant.MOSES_NEWUSER_SPECIAL_PRODUCTS, value.toString(),expireTime);
                    log.info("刷新新手专享tag {}, products {}",newUserSpecialTagId, JSON.toJSONString(products));
                }
            }
            //log.error("刷新新手专享tagId：{}",newUserSpecialTagId);
        }catch(Exception ex){
            log.error("查询新手专享的标签异常：", ex);
        }
    }

    public Long getNewUserSpecialTagId() {
        return newUserSpecialTagId;
    }

    /**
     * 通过tagId，获取该tagId下的所有商品
     * @param tagId
     * @return
     */
    public List<Long> queryProductTagMarked(Long tagId){
        MarkedQueryDTO param = new MarkedQueryDTO();
        param.setTagId(tagId);
        com.biyao.tag.dto.Result<List<Long>> result = null;
        result = productBaseTagService.queryProductTagMarked(param);
        if(result != null && result.getSuccess()){
            return result.getObj();
        }
        else{
            log.error("刷新新手专享商品失败：{}",JSON.toJSONString(result));
            return null;
        }
    }

}
