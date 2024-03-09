package com.biyao.moses.service.match.impl;

import com.alibaba.fastjson.JSONObject;
import com.biyao.moses.match.MatchItem2;
import com.biyao.moses.match.MatchParam;
import com.biyao.moses.service.match.IMatchOnline;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import static com.biyao.moses.constant.MatchStrategyConst.CATEGORY;

/**
 * @program: moses-parent-online
 * @description: 处理类目召回
 * @author: changxiaowei
 * @Date: 2022-01-27 14:08
 **/
@Service(value = CATEGORY)
@Slf4j
public class CategoryImpl implements IMatchOnline {
        @Autowired
        CommonService commonService;
        @Autowired
        RestHighLevelClient restHighLevelClient;

        @Autowired
        HotMatchOnlineImpl hotMatchOnline;
        @Override
        public List<MatchItem2> match(MatchParam matchParam) {
            if(CollectionUtils.isEmpty(matchParam.getThirdCateGoryId()) && CollectionUtils.isEmpty(matchParam.getTagId()) ){
                log.error("[严重异常]类目召回时后台三级类目id和tagId均为空，入参：{}", JSONObject.toJSONString(matchParam));
                return new ArrayList<>();
            }
           return hotMatchOnline.match(matchParam);
        }
    }

