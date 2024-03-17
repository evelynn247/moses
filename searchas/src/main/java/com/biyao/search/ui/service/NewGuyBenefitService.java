package com.biyao.search.ui.service;

import com.biyao.search.ui.model.NewGuyAccessStatus;

/**
 * @Auther: sunbaokui
 * @Date: 2019/4/3 11:55
 * @Description: 新手专享服务类
 */
public interface NewGuyBenefitService {


    /**
     * @Description: 获取新手专享访问状态
     * @auther: sunbaokui
     * @date: 2019/5/22 15:34
     * @param:
     * @return:
     */
    NewGuyAccessStatus getNewGuyAccessStatus(Integer uid, String uuid);

   
}
