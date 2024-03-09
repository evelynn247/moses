package com.biyao.moses.service;

import com.biyao.moses.model.template.entity.TotalTemplateInfo;
import com.biyao.moses.params.BaseRequest2;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @ClassName NoFirstPageResult
 * @Description
 * @Author xiaojiankai
 * @Date 2019/12/27 16:33
 * @Version 1.0
 **/
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NoFirstPageResult {
    /**
     * 实验分流结果
     */
    BaseRequest2 baseRequest2;
    /**
     * 待插入的商品集合
     */
    List<TotalTemplateInfo> waitInsertPidInfoList;
}
