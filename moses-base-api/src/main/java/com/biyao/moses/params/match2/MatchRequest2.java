package com.biyao.moses.params.match2;

import com.alibaba.dubbo.common.utils.CollectionUtils;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.biyao.moses.params.BaseRequest2;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author zhaiweixi@idstaff.com
 * @date 2019/10/8
 **/
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MatchRequest2 extends BaseRequest2 {
    /**
     * 业务标识
     */
    private String biz;

    /**
     * match召回源及其权重，格式为：source,weight|...source,weight
     */
    private String sourceAndWeight;

    /**
     * 召回源数据获取的策略
     */
    private String sourceDataStrategy;

    /**
     * 人工配置召回源列表
     */
    private List<String> manualSourceList;
    /**
     * 召回源数据存储的redis
     */
    private String sourceRedis;

    /**
     * ucb召回源的数据号
     */
    private String UcbDataNum;

    /**
     * 期望的商品数量上限
     */
    private Integer expNum;

    /**
     * 日志debug开关，由mosesui传入，只有白名单用户打开日志开关
     * true：日志开关打开，false：日志开关关闭
     */
    private Boolean debug;
    /**
     * pid集合（需要查找相似商品的商品pid集合）
     */
    private List<String> pidList;

    /**
     * 必要朋友V2.0项目新增
     * 好友已购业务表示：好友uid列表
     * 格式为：id1，id2，id3
     */
    private String responseMapKeys;

    /**
     * match需要执行的过滤规则名称
     */
    private String ruleName;
    /**
     * 是否是走规则引擎召回逻辑
     */
    private boolean isDrools = false;
    /**
     * 个性化推荐设置开关。
     * true表示开关打开，可以使用用户个性化数据做推荐
     * false表示开关关闭，不可以使用用户个性化数据做推荐
     */
    private boolean personalizedRecommendSwitch = true;
    /**
     * 是否需要过滤必要造物商品 0=false 不传默认0
     */
    private String isFilterByZw;
    /**
     * 校验request参数
     * @return
     */
    public boolean valid(){
        if (StringUtils.isBlank(this.uuid)){
            return false;
        }

        if (StringUtils.isBlank(this.sid)){
            return false;
        }

        return true;
    }

    /**
     * 校验召回源是否为人工配置的召回源
     * @param source
     * @return
     */
    public boolean isManualSource(String source){
      if(CollectionUtils.isEmpty(manualSourceList)){
          return false;
      }
      return manualSourceList.contains(source);
    }
}
