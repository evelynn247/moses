package com.biyao.moses.service.match;

import com.biyao.moses.match.MatchItem2;
import com.biyao.moses.match.MatchParam;
import com.biyao.moses.util.ApplicationContextProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import static com.biyao.moses.constant.MatchStrategyConst.*;

/**
 * @program: moses-parent-online
 * @description: 在线召回
 * @author: changxiaowei
 * @create: 2021-10-11 09:45
 **/
@Slf4j
@Service
public class AsyncMatchOnlineService {

    @Async
    public Future<List<MatchItem2>> executeMatch2(MatchParam matchParam, String matchBeanName) {
        List<MatchItem2> result = new ArrayList<>();
        try {
            IMatchOnline match ;
            // fm 和icf  统一为个性化召回
            if(PERSONAL_FM.equals(matchBeanName) || PERSONAL_ICF.equals(matchBeanName) ){
                match = ApplicationContextProvider.getBean(PERSONAL, IMatchOnline.class);
            }else {
                match = ApplicationContextProvider.getBean(matchBeanName, IMatchOnline.class);
            }
            result = match.match(matchParam);
        }catch (Exception e){
            log.error("[严重异常]获取召回源{}的数据时发生异常，", matchBeanName, e);
        }
        return new AsyncResult<>(result);
    }
}
