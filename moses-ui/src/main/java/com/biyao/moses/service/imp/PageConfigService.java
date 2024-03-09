package com.biyao.moses.service.imp;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.biyao.moses.common.enums.MosesConfTypeEnum;
import com.biyao.moses.common.enums.PageVersionEnum;
import com.biyao.moses.common.enums.RedisKeyTypeEnum;
import com.biyao.moses.common.enums.TemplateTypeEnum;
import com.biyao.moses.constants.CommonConstants;
import com.biyao.moses.context.ByUser;
import com.biyao.moses.exp.ExpirementSpace;
import com.biyao.moses.exp.util.MosesConfUtil;
import com.biyao.moses.model.exp.ExpRequest;
import com.biyao.moses.model.exp.Expirement;
import com.biyao.moses.model.template.Block;
import com.biyao.moses.model.template.Page;
import com.biyao.moses.model.template.Template;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;
import com.biyao.moses.util.MatchRedisUtil;
import com.biyao.moses.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * @ClassName PageConfigService
 * @Description 页面配置相关方法
 * @Author xiaojiankai
 * @Date 2020/6/1 13:39
 * @Version 1.0
 **/
@Component
@Slf4j
public class PageConfigService {

    @Autowired
    private ExpirementSpace expirementSpace;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private MatchRedisUtil matchRedisUtil;

    @Autowired
    private MosesConfUtil mosesConfUtil;

    public Page<TotalTemplateInfo> queryPageById(String pid, ByUser user) {
        Page<TotalTemplateInfo> page = null;
        try {
            String pageObject;
            String key;
            Boolean[] isRedisExceptionArray = new Boolean[]{false};
            Expirement expirement = expirementSpace.getExpirement(
                    ExpRequest.builder().tid(pid).uuid(user.getUuid()).layerName(CommonConstants.LAYER_NAME_UI).build());
            if (expirement != null && expirement.getConfList() != null && expirement.getConfList().size() > 0) {
                String expNum = expirement.getConfList().get(0).getExpNum();
                // 直接使用expNum 配置的pid
                key = expNum;
            } else {
                key = pid;
            }
            //实验配置迁移后的 临时方案，先从match集群中查询，再从原集群查询
            pageObject = matchRedisUtil.getString(key,isRedisExceptionArray);
            if(StringUtils.isBlank(pageObject)){
                pageObject = redisUtil.getString(key,isRedisExceptionArray);
                if(StringUtils.isBlank(pageObject)) {
                    Boolean isRedisException = isRedisExceptionArray[0];
                    pageObject = mosesConfUtil.dealOnRedisValueNullOrException(MosesConfTypeEnum.PageConf, key, null, RedisKeyTypeEnum.STRING.getId(), isRedisException);
                }else{
                    log.error("[严重异常]redis key {} 未迁移到match集群", key);
                    mosesConfUtil.refreshConfCacheAndDB(MosesConfTypeEnum.PageConf, key, null, pageObject, RedisKeyTypeEnum.STRING.getId());
                }
            }else{
                mosesConfUtil.refreshConfCacheAndDB(MosesConfTypeEnum.PageConf, key, null, pageObject, RedisKeyTypeEnum.STRING.getId());
            }
            page = JSONObject.parseObject(pageObject, new TypeReference<Page<TotalTemplateInfo>>() {
            });

            //对页面配置进行修正
            dealByPageVersion(page, user);

        } catch (Exception e) {
            log.error("[严重异常]页面信息获取失败", e);
        }
        return page;
    }

    /**
     * 根据入参修改模板类型数据
     */
    private void dealByPageVersion(Page<TotalTemplateInfo> page, ByUser user){
        try {
            String pageVersion = user.getPageVersion();
            //如果广告活动对应的页面版本，则不修改模板
            if (!PageVersionEnum.PAGE_VERSION_ADVERT.getVersion().equals(pageVersion) || page == null) {
                return;
            }

            List<Block<TotalTemplateInfo>> blockList = page.getBlockList();
            if (CollectionUtils.isEmpty(blockList)) {
                return;
            }

            //遍历模板，将原模板修改为新模板
            for (Block<TotalTemplateInfo> block : blockList) {
                if (block == null) {
                    continue;
                }
                List<Template<TotalTemplateInfo>> templateList = block.getBlock();
                if (CollectionUtils.isEmpty(templateList)) {
                    continue;
                }
                for (Template<TotalTemplateInfo> template : templateList) {
                    if (template == null) {
                        continue;
                    }
                    if (TemplateTypeEnum.PRI_DOUBLE_ROWLISST.getTemplateType().equals(template.getTemplateName())
                            || TemplateTypeEnum.FEED_DOUBLE.getTemplateType().equals(template.getTemplateName())) {
                        template.setTemplateName(TemplateTypeEnum.PRI_DOUBLE_ROWLISST_FOR_ADVERT.getTemplateType());
                        template.setTemplateType(TemplateTypeEnum.PRI_DOUBLE_ROWLISST_FOR_ADVERT.getTemplateId());
                    } else if (TemplateTypeEnum.TEMPLATE_NEWUSER.getTemplateType().equals(template.getTemplateName())) {
                        template.setTemplateName(TemplateTypeEnum.TEMPLATE_NEWUSER_FOR_ADVERT.getTemplateType());
                        template.setTemplateType(TemplateTypeEnum.TEMPLATE_NEWUSER_FOR_ADVERT.getTemplateId());
                    }
                }
            }
        }catch (Exception e){
            log.error("[严重异常]替换页面配置时，发生异常， e ");
        }
    }
}
