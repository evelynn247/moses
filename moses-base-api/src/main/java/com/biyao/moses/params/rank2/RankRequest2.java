package com.biyao.moses.params.rank2;

import com.biyao.moses.model.match2.MatchItem2;
import com.biyao.moses.params.BaseRequest2;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * @author zhaiweixi@idstaff.com
 * @date 2019/10/10
 **/
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RankRequest2 extends BaseRequest2 {

    /**
     * 排序名称
     */
    private String rankName;

    /**
     * 算法数据号
     */
    private String dataNum;

    /**
     * 召回结果
     */
    private List<MatchItem2> matchItemList;

    /**
     * 前台类目ID对应的后台三级类目ID集合，多个ID时以逗号分隔
     */
    private String categoryIds;

    /**
     *  前台类目ID
     */
    private String frontendCategoryId;

    /**
     * 业务名称
     */
    private String bizName;

    /**
     * 日志debug开关，由mosesui传入，只有白名单用户打开日志开关
     * true：日志开关打开，false：日志开关关闭
     */
    private Boolean debug;

    /**
     * 是否使用召回分排序
     */
    private Boolean recallPoints;
    /**
     * 是否使用价格因素排序
     */
    private Boolean priceFactor;
    /**
     * 惩罚因子使用顺序
     */
    private String punishFactor;
    // 用户持有特权金最大面额
    private String tqjMaxLimit;

    /**
     * request参数校验
     * @param request
     * @return
     */
    public boolean parameterValidate(RankRequest2 request){
        if(StringUtils.isBlank(request.getUuid())|| CollectionUtils.isEmpty(request.getMatchItemList())
                ||StringUtils.isBlank(request.getSid())){
            return false;
        }
        return true;
    }

}
