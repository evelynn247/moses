package com.biyao.moses.bo;

import lombok.Data;

/**
 * @program: moses-parent-online
 * @description: SourceWeight Business Object
 * @author: changxiaowei
 * @Date: 2021-12-20 16:39
 **/
@Data
public class SourceWeightBo {
    /**
     * 数量权限控制 实例说明：
     * per,1,1,|hot,0.2,0.5
     * 说明：
     * per为个性化召回
     * 1 控制召回数量 expNum*1
     * 1 召回分权重  召回分乘以1
     */
   private double numWeight = 1D;
    /**
     * 召回分权限控制
     */
   private double scoreWeight = 1D;

}
