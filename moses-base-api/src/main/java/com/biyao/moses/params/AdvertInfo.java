package com.biyao.moses.params;

import com.biyao.moses.model.template.entity.TotalTemplateInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @ClassName AdvertInfo
 * @Description 活动广告信息
 * @Author xiaojiankai
 * @Date 2020/3/9 15:11
 * @Version 1.0
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdvertInfo {
    TotalTemplateInfo totalTemplateInfo;

    Integer position;
}
