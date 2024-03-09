package com.biyao.moses.service.imp;

import com.biyao.moses.context.ByUser;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;
import com.biyao.moses.params.BaseRequest2;
import com.biyao.moses.params.UIBaseRequest;
import com.biyao.moses.service.MatchAndRankService2;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.Future;

/**
 * @ClassName MatchAndRank2AnsyService
 * @Description 异步获取match2、rank2
 * @Author xiaojiankai
 * @Date 2019/12/14 16:08
 * @Version 1.0
 **/
@Slf4j
@Service
public class MatchAndRank2AnsyService {

    @Autowired
    private MatchAndRankService2 matchAndRankService2;

    @Async
    public Future<List<TotalTemplateInfo>> matchAndRank(UIBaseRequest uibaseRequest, ByUser user, BaseRequest2 baseRequest2, String rankName, String bizName){
        List<TotalTemplateInfo> result = matchAndRankService2.matchAndRank(uibaseRequest, user, baseRequest2,null,  rankName, bizName);
        return new AsyncResult<>(result);
    }
}
