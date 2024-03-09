package com.biyao.moses.drools;

import com.biyao.moses.context.ByUser;
import com.biyao.moses.model.drools.RuleFact;
import com.biyao.moses.model.match2.MatchItem2;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;
import com.biyao.moses.params.BaseRequest2;
import com.biyao.moses.params.rank2.RankRequest2;
import com.biyao.moses.params.rank2.RankResponse2;
import com.biyao.moses.rules.RuleContext;
import com.biyao.moses.util.MyBeanUtil;
import lombok.extern.slf4j.Slf4j;
import org.drools.core.base.RuleNameEndsWithAgendaFilter;
import org.drools.core.base.RuleNameStartsWithAgendaFilter;
import org.kie.api.runtime.KieSession;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @program: moses-parent
 * @description: Drools过滤服务
 * @author: changxiaowei
 * @create: 2021-03-29 16:15
 **/
@Service
@Slf4j
public class DroolsService {
    /**
     * drools 过滤规则
     * @param kieSession
     * @param ruleFact
     * @param matchItem2List
     * @param
     */
      public List<MatchItem2>  filter(KieSession kieSession, RuleFact ruleFact, List<MatchItem2> matchItem2List, String uid,String uuid){
          try {
              // 构建过滤参数 filterContext
              FilterContext filterContext = FilterContext.builder().
                      expectMaxNum(ruleFact.getExpectNumMax()).
                      matchItem2List(matchItem2List).
                      uid(Integer.valueOf(uid)).
                      uuid(uuid).
                      build();
              // 设置全局变量  用户drl文件和java程序交互
              kieSession.setGlobal("filterContext",filterContext);
              // 按照配置的顺序执行过滤规则
              for(String filterParam:ruleFact.getMatchParamList()){
                  kieSession.getAgenda().getAgendaGroup(filterParam).setFocus();
                  kieSession.fireAllRules(new RuleNameStartsWithAgendaFilter(filterParam));
              }
              // 取出全局变量
              FilterContext result = (FilterContext)kieSession.getGlobal("filterContext");
              return result.getMatchItem2List();
          }catch (Exception e){
              log.error("[严重异常]执行规则引擎过滤逻辑异常，uuid：{}，异常信息：",uuid,e);
          }
          // 异常情况  返回空
         return null;
      }


    /**
     * drools rank 规则
     * @param kieSession
     * @return
     */
    public  RankResponse2  rank(KieSession kieSession,RankRequest2 rankRequest2 ){
        try {
            // 初始化结果集
            RankResponse2 rankResponse2 = new RankResponse2();
            // 设置全局变量
            kieSession.setGlobal("rankRequest2",rankRequest2);
            kieSession.setGlobal("rankResponse2",rankResponse2);
            kieSession.getAgenda().getAgendaGroup("rank").setFocus();
            kieSession.fireAllRules(new RuleNameEndsWithAgendaFilter("rank"));
            // 若排序异常 返回结果为空 则取match返回结果的顺序
            return (RankResponse2)kieSession.getGlobal("rankResponse2");
        }catch (Exception e){
            log.error("[严重异常]执行规则引擎过滤逻辑异常，uuid：{}，异常信息：",rankRequest2.getUuid(),e);
        }
         return  null;
      }

    /**
     *类目隔断机制处理
     * 参数待定
     */
    public  List<TotalTemplateInfo>  dealCategory(KieSession kieSession,RuleContext ruleContext){
        try {
            // 2 初始化结果集
            List<TotalTemplateInfo> totalTemplateInfoList = new ArrayList<>();
            //  设置全局变量
            kieSession.setGlobal("ruleContext",ruleContext);
            kieSession.setGlobal("totalTemplateInfoList",totalTemplateInfoList);
            // 执行规则
            kieSession.getAgenda().getAgendaGroup("category").setFocus();
            kieSession.fireAllRules(new RuleNameStartsWithAgendaFilter("category"));

            // 获取全局变量
            totalTemplateInfoList= (List<TotalTemplateInfo>) kieSession.getGlobal("totalTemplateInfoList");
            if(CollectionUtils.isEmpty(totalTemplateInfoList)){
                return ruleContext.getAllProductList();
            }
            return totalTemplateInfoList;
        }catch (Exception e){
            log.error("[严重异常]执行规则引擎过滤逻辑异常，uuid：{}，异常信息：",ruleContext.getUuid(),e);
        }
        return  ruleContext.getAllProductList();
    }

}
