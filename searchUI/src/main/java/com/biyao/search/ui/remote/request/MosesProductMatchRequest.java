package com.biyao.search.ui.remote.request;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * @author zhaiweixi@idstaff.com
 * @date 2019/7/23
 **/
@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MosesProductMatchRequest {
    /**
     * uuid
     */
    private String uuid;
    /**
     * uid
     */
    private String uid;
    /**
     * app version
     */
    private String av;
    /**
     * app version number
     */
    private String avn;
    /**
     * 设备
     */
    private String device;
    /**
     * 站点 1:M  2:miniapp  7:ios  9:android
     */
    private Integer siteId = 1;
    /**
     * 数据源
     */
    private String dataSourceType;
    /**
     * 期望召回商品数
     */
    private Integer exceptNum = 500;
    /**
     * 经度
     */
    private String lng;
    /**
     * 纬度
     */
    private String lat;
    /**
     * upc用户类型  1:老客  2:新访客  3:老访客
     */
    private Integer upcUserType = 2;
    /**
     * 后台类目ID集合
     */
    private List<String> categoryIds = new ArrayList<>();
    /**
     * 请求ID
     */
    private String pvid;
}
