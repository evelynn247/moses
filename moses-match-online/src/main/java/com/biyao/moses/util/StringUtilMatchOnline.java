package com.biyao.moses.util;

import com.biyao.moses.common.utils.StringUtilsOnline;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import java.util.List;

/**
 * @program: moses-parent-online
 * @description: matchonline String 工具类
 * @author: changxiaowei
 * @Date: 2022-03-22 14:19
 **/
@Slf4j
public class StringUtilMatchOnline {

    /**
     * str 解析成 qureybuilder
     * 格式（
     * shelfStatus=1&&
     * showStatus=0&&
     * isCreator=0&&
     * supportTexture!=1&&
     * showStatus=0&&
     * isToggroupProduct=1&&
     * newPrivilege=1&&
     * isNewVProduct=1&&
     * ）
     *
     * @param matchRule
     * @return
     */
    public static QueryBuilder stringToQureyBuilder(String matchRule) {
        BoolQueryBuilder mustQueryBuilder;
        if (StringUtils.isBlank(matchRule)) {
            return null;
        }
        mustQueryBuilder = QueryBuilders.boolQuery();
        String[] rules = matchRule.split("&&");
        try {
            for (int i = 0; i < rules.length; i++) {
                String keyValue = rules[i];
                // 先处理括号中的解析 &&(fcategory3Id=644 ||tagsId=230) 类型
                if (keyValue.startsWith("(")) {
                    BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();
                    String shouldRule = keyValue.substring(1, keyValue.length() - 1);
                    String[] shouldRuleArr = shouldRule.split("\\|\\|");
                    for (int i1 = 0; i1 < shouldRuleArr.length; i1++) {
                        if (!StringUtilsOnline.isBlank(shouldRuleArr[i1])) {
                            String[] keyValue2 = shouldRuleArr[i1].split("=");
                            if (keyValue2.length != 2) {
                                continue;
                            }
                            //直接用termsQuery
                            List<Integer> valueList = StringUtilsOnline.stringArrToIntList(keyValue2[1].split(","));
                            if (!CollectionUtils.isEmpty(valueList)) {
                                queryBuilder.should((QueryBuilders.termsQuery(keyValue2[0], valueList)));
                            }
                        }
                    }
                    mustQueryBuilder.must(queryBuilder);
                }// 如果为不等于 则用mustNot
                else if (keyValue.contains("!")) {
                    String[] keyValueArr = keyValue.split("!=");
                    if (keyValueArr.length == 2) {
                        mustQueryBuilder.mustNot((QueryBuilders.termsQuery(keyValueArr[0], StringUtilsOnline.stringArrToIntList(keyValueArr[1].split(",")))));
                    }
                } else {
                    String[] keyValueArr = keyValue.split("=");
                    if (keyValueArr.length == 2) {
                        mustQueryBuilder.must((QueryBuilders.termsQuery(keyValueArr[0], StringUtilsOnline.stringArrToIntList(keyValueArr[1].split(",")))));
                    }
                }
            }
        } catch (Exception e) {
            log.error("[严重异常]字符串转成queryBuilder异常，参数：{}，异常信息：{}", matchRule, e);
        }
        return mustQueryBuilder;
    }
}
