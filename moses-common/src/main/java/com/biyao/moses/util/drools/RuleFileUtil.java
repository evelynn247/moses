package com.biyao.moses.util.drools;

import com.biyao.moses.common.constant.RuleConstant;
import com.biyao.moses.model.drools.RuleFact;
import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.biyao.moses.util.FileUtil.getRemoteFile;

/**
 * @program: moses-parent
 * @description: 规则配置文件处理
 * @author: changxiaowei
 * @create: 2021-03-26 17:32
 **/
@Slf4j
public class RuleFileUtil {

    private static final int linesNum = 18;

    private static HashFunction hf = Hashing.md5();
    public static List<RuleFact> loadFile(String fileUrl){

        List<RuleFact> ruleFacts=new ArrayList<>();
        try {
            //1 加载网络文件
            List<String> lines =getRemoteFile(fileUrl);
            // 2 过滤无效的配置行
            List<String> validLines = filterLines(lines);
            // 3 按照每条规则的起始 ruleId 划分规则
            List<List<String>> ruleList = transformToRuleBlock(validLines);
            // 4 过滤不符合格式的规则
            ruleFacts = transformToRuleFactUnit(ruleList);
        }catch (Exception e){
            log.error("[严重异常]加载规则配置文件异常，异常信息：",e);
        }
        return ruleFacts;
    }

    private static List<List<String>> transformToRuleBlock(List<String> lines) {
        List<List<String>> ruleBlocks = Lists.newArrayList();

        for (String line : lines) {
            // 按 ruleId 分块
            if (line.startsWith("ruleId:")) {
                ruleBlocks.add(Lists.newArrayList());
            }
            try {
                ruleBlocks.get(ruleBlocks.size() - 1).add(line);
            }catch (Exception e){
                log.error("[严重异常]规则配置文件中起始配置项必须为ruleId");
            }

        }
        return ruleBlocks;
    }


    /**
     * 过滤掉不符合要求的配置行
     * @param lines
     * @return
     */
    private static List<String> filterLines(List<String> lines) {
        List<String> resultList = new ArrayList<>();
        lines.forEach(l->{
            l=l.trim();
            if(l.startsWith("ruleId:")||l.startsWith("condition:")||l.startsWith("execute:")){
                resultList.add(l);
            }
        });
        return resultList;
    }

    private static List<RuleFact> transformToRuleFactUnit(List<List<String>> ruleList){

        List<RuleFact> result=new ArrayList<>();

        for (List<String> validLines : ruleList){
            int temp=0;
            // 初始化fact对象
            RuleFact ruleFact = new RuleFact();
            List<String> matchParamList=new ArrayList<>();
            List<String> rankParamList=new ArrayList<>();
            List<String> ruleParamList=new ArrayList<>();
            ruleFact.setMatchParamList(matchParamList);
            ruleFact.setRankParamList(rankParamList);
            ruleFact.setRuleParamList(ruleParamList);

            // 用于判断该配置项是否已经出现过
            List<String> addList=new ArrayList<>();

            for(String line :validLines){

                try {
                    // 若出现重复的配置项 则规则无效
                    if(line.startsWith("ruleId:")){
                        if(addList.contains("ruleId")){
                            log.error("[严重异常]重复配置项{}，跳过该规则","ruleId");
                            break;
                        }
                        ruleFact.setRuleId(line.split(":")[1]);
                        temp++;
                        addList.add("ruleId");
                        continue;
                    }
                    String[]  arrLine =line.split(":");
                    // 条件
                    if(arrLine[0].equals("condition")){
                        String[] split = arrLine[1].split("==");
                        if(addList.contains(split[0])){
                            log.error("[严重异常]重复配置项{}，跳过该规则",split[0]);
                            break;
                        }
                        if(split[0].equals("scene")){
                            ruleFact.setScene(split[1]);
                            addList.add("scene");
                            temp++;
                            continue;
                        }
                        if(split[0].equals("utype")){
                            ruleFact.setUtype(split[1]);
                            addList.add("utype");
                            temp++;
                            continue;
                        }
                        if(split[0].equals("siteid")){
                            ruleFact.setSiteId(split[1]);
                            addList.add("siteid");
                            temp++;
                            continue;
                        }
                        if(split[0].equals("flow")){
                            String[] bucketSplit = split[1].split("~");
                            ruleFact.setBucketMaxValue(Integer.valueOf(bucketSplit[1]));
                            ruleFact.setBucketMinValue(Integer.valueOf(bucketSplit[0]));
                            addList.add("flow");
                            temp++;
                            continue;
                        }
                        if(split[0].equals("is_personal")){
                            ruleFact.setIsPersonal("1".equals(split[1]));
                            addList.add("is_personal");
                            temp++;
                            continue;
                        }
                        // 白名单
                        if(split[0].equals("white_list")){
                            ruleFact.setWhiteList(Arrays.asList(split[1].split(",")));
                            addList.add("white_list");
                            temp++;
                            continue;
                        }
                    }
                    // 执行体
                    if(arrLine[0].equals("execute")){
                        String[] split = arrLine[1].split("=");
                        if(addList.contains(split[0])){
                            log.error("[严重异常]重复配置项{}，跳过该规则",split[0]);
                            break;
                        }
                        if(RuleConstant.matchParamSet.contains(split[0])){
                            matchParamList.add(split[0]);
                        }
                        if(RuleConstant.rankParamSet.contains(split[0])){
                            rankParamList.add(split[0]);
                        }
                        if(RuleConstant.ruleParamSet.contains(split[0])){
                            ruleParamList.add(split[0]);
                        }
                        if(split[0].equals("source_and_weight")){
                            ruleFact.setSourceAndWeight(split[1]);
                            addList.add("source_and_weight");
                            temp++;
                            continue;
                        }
                        if(split[0].equals("source_data_strategy")){
                            ruleFact.setSourceDataStrategy(split[1]);
                            addList.add("source_data_strategy");
                            temp++;
                            continue;
                        }
                        if(split[0].equals("expect_num_max")){
                            ruleFact.setExpectNumMax(Integer.valueOf(split[1].trim()));
                            addList.add("expect_num_max");
                            temp++;
                            continue;
                        }
                        if(split[0].equals("sex_filter")){
                            ruleFact.setSexFilter(split[1].trim());
                            addList.add("sex_filter");
                            temp++;
                            continue;
                        }
                        if(split[0].equals("season_filter")){
                            ruleFact.setSeasonFilter(split[1].trim());
                            addList.add("season_filter");
                            temp++;
                            continue;
                        }
                        if(split[0].equals("recall_points")){
                            ruleFact.setRecallPoints(split[1].trim());
                            addList.add("recall_points");
                            temp++;
                            continue;
                        }
                        if(split[0].equals("punish_factor")){
                            if( Arrays.asList(split[1].split(",")).contains("0")){
                                ruleFact.setPunishFactor("0");
                            }else {
                                ruleFact.setPunishFactor(split[1]);
                            }
                            addList.add("punish_factor");
                            temp++;
                            continue;
                        }
                        if(split[0].equals("price_factor")){
                            ruleFact.setPriceFactor(split[1].trim());
                            addList.add("price_factor");
                            temp++;
                            continue;
                        }
                        if(split[0].equals("repurchase_filter")){
                            ruleFact.setRepurchaseFilter(split[1].trim());
                            addList.add("repurchase_filter");
                            temp++;
                            continue;
                        }
                        if(split[0].equals("category_partition")){
                            ruleFact.setCategoryPartition(split[1].trim());
                            addList.add("category_partition");
                            temp++;
                        }
                        // 召回方式
                        if(split[0].equals("match_type")){
                            ruleFact.setMatchType(Byte.valueOf(split[1].trim()));
                            addList.add("match_type");
                            temp++;
                        }
                    }
                }catch (Exception e){
                    log.error("[严重异常]数据格式错误，该条规则无效，错误信息：",e);
                    break;
                }
            }
            if(temp!=linesNum){
                log.error("[严重异常]需要配置{}条配置项,实际配置{}条配置项)",linesNum,temp);
                continue;
            }
            result.add(ruleFact);
        }
        return result;
    }

    public static Integer hash(String s1, String s2) {

        HashCode hc = hf.newHasher().putString(s1, Charsets.UTF_8).putString(s2, Charsets.UTF_8).hash();

        return Math.abs(hc.asInt()) % 100;
    }
}
