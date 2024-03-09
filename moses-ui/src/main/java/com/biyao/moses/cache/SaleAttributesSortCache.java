package com.biyao.moses.cache;

import com.biyao.moses.common.constant.MatchRedisKeyConstant;
import com.biyao.moses.constants.CommonConstants;
import com.biyao.moses.util.MatchRedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@EnableScheduling
public class SaleAttributesSortCache {

    @Autowired
    private MatchRedisUtil matchRedisUtil;

    private Map<String, List<String>> saleAttrsMap = new ConcurrentHashMap<>();

    public List<String> getSaleAttrsByAttrKey(String key) {
        if(StringUtils.isBlank(key)){
            return  null;
        }
        return saleAttrsMap.get(key);
    }

    private static final String colorStr = CommonConstants.STD_SALE_ATTR_KEY_COLOR;
    private static final String sizeStr = CommonConstants.STD_SALE_ATTR_KEY_SIZE;
    private static final String sortColorFinal = "白色,黑色,粉色,褐色,红色,天蓝色,黄色,橘色,酒红色,军绿色,绿色,蓝色,浅黄色,浅灰色,浅绿色,紫色,巧克力色,深灰色,深卡其布色,深蓝色,深紫色,紫罗兰,花色,透明";
    private static final String sortSizeFinal = "3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,33.5,34,34.5,35,35.5,36,36.5,37,37.5,38,38.5,39,39.5,40,40.5,41,41.5,42,42.5,43,43.5,44,44.5,45,45.5,46,47,48,49,50,51,52,53,54,55,56,58,59,60," +
            "65,66,68,70,71,72,73,74,76,77,78,79,80,81,82,83,84,85,86,88,90,92,94,95,96,98,100,102,104,105,106,110,115,120,125,135,145,150,155,160,165,170,175,180,185,190,"+
            "XS,S,M,L,XL,XXL,XXXL,4XL,5XL,"+
            "1A,2A,3A,4A,5A,6A,7A,8A,9A,10A,11A,12A,44A,44B,44C,46A,46B,46C,48A,48B,48C,50A,50B,50C,52A,52B,52C,54A,54B,54C,56A,56B,56C,65A,70A,70B,70C,70D,75A,75B,75C,75D,75E,80A,80B,80C,80D,80E,85A,85B,85C,85D,85E,90A,90B,90C,90D,90E,95A,95B,95C,95D,95E,L/XL,S/M,"+
            "A4,A5,A6,A7,A8,AB4,AB5,AB6,AB7,AB8,BB4,BB5,BB6,BB7,BE4,BE5,BE6,BE7,BE8,Y4,Y5,Y6,Y7,YA4,YA5,YA6,YA7,YA8,"+
            "1.0m床,1.2m床,1.35m床,1.5m床,1.8m床,2.0m床,2.2m床,2.5m床,2.8m床,1.0*2.0m,1.2*2.0m,1.35*2.0m,1.5*2.0m,1.8*2.0m,2.0*2.0m,2.0*2.2m,"+
            "40*40cm,45*60cm,40*60cm,45*75cm,50*80cm,57*87cm,60*90cm,70*140cm,80*150cm,120*170cm,140*200cm,150*200cm,150*210cm,160*230cm,200*230cm,200*290cm,220*240cm,235*240cm,240*250cm,240*260cm,240*270cm,240*340cm,245*250cm,245*270cm,300*400cm,400*500cm,"+
            "1.5L,1.6L,1.7L,1.8L,1.9L,2.0L,14寸,15寸,16寸,17寸,18寸,19寸,20寸,21寸,22寸,24寸,25寸,26寸,27寸,28寸,29寸,30寸,32寸,"+
            "0-3个月,3-6个月,6-9个月,9-12个月,12-18个月,18-24个月,0.5-1岁,1-2岁,3-4岁,4-5岁,6-7岁,8-9岁,10-11岁,12-13岁,14-15岁,"+"200mm,250mm,300mm,305mm,350mm,400mm,"+"均码";

    @PostConstruct
    public  void init(){
        refreshSaleAttributesCache();
    }

    @Scheduled(cron = "0 0 0/1 * * ?")
    public void refreshSaleAttributesCache(){
        log.info("[任务进度][颜色尺码排序]获取颜色尺码排序信息开始");
        String sortColor = matchRedisUtil.getString(MatchRedisKeyConstant.STD_SALE_ATTRS_COLOR);
        String sortSize = matchRedisUtil.getString(MatchRedisKeyConstant.STD_SALE_ATTRS_SIZE);
        List<String> sortColorList = null;
        List<String> sortSizeList = null;
        if(StringUtils.isNotBlank(sortColor)) {
            sortColorList = str2List(sortColor, ",");
        }

        Map<String, List<String>> saleAttrsMapTmp = new ConcurrentHashMap<>();
        if(!CollectionUtils.isEmpty(sortColorList)){
            saleAttrsMapTmp.put(colorStr, sortColorList);
        }else{
            List<String> lastColorList = saleAttrsMap.get(colorStr);
            if(CollectionUtils.isEmpty(lastColorList)){
                //如果redis中没有获取到数据 且当前内存中也没有数据，则填入默认值
                saleAttrsMapTmp.put(colorStr, str2List(sortColorFinal, ","));
            }else{
                //如果redis中没有获取到数据 但当前内存中有数据，则使用当前内存中的数据
                saleAttrsMapTmp.put(colorStr, lastColorList);
            }
        }

        if(StringUtils.isNotBlank(sortSize)) {
            sortSizeList = str2List(sortSize, ",");
        }

        if(!CollectionUtils.isEmpty(sortSizeList)) {
            saleAttrsMapTmp.put(sizeStr, sortSizeList);
        }else{
            List<String> lastSizeList = saleAttrsMap.get(sizeStr);
            if(CollectionUtils.isEmpty(lastSizeList)){
                //如果redis中没有获取到数据 且当前内存中也没有数据，则填入默认值
                saleAttrsMapTmp.put(sizeStr, str2List(sortSizeFinal, ","));
            }else{
                //如果redis中没有获取到数据 但当前内存中有数据，则使用当前内存中的数据
                saleAttrsMapTmp.put(sizeStr, lastSizeList);
            }
        }
        saleAttrsMap = saleAttrsMapTmp;
        log.info("[任务进度][颜色尺码排序]获取刷新颜色尺码排序信息结束，颜色个数{}，尺码个数{}", saleAttrsMapTmp.get(colorStr).size(), saleAttrsMapTmp.get(sizeStr).size());
    }

    private ArrayList<String> str2List(String str, String split){
        ArrayList<String> resultList = new ArrayList<>();

        if (StringUtils.isNotBlank(str)) {
            String[] data = str.split(split);
            if (data!=null) {
                for (int i = 0; i < data.length; i++) {
                    resultList.add(data[i]);
                }
            }
        }

        return resultList;
    }

}
