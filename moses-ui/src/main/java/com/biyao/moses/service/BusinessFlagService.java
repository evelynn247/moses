package com.biyao.moses.service;

import java.util.List;

/**
 * 判断业务标识接口
 */
public interface BusinessFlagService {

    /**
     * 获取是否展示上新红点标识
     *
     * @param category
     * @param scmIds
     * @return
     */
     Boolean hasNewsByCategroys(String category, String scmIds, String uuid, String frontendCategoryId);

}
