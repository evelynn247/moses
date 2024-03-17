package com.biyao.search.ui.remote.response;

import lombok.*;

import java.io.Serializable;

/**
 * 图片筛选项
 * @author zhaiweixi@idstaff.com
 * @date 2019/9/5
 **/
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class FilterItem implements Serializable {
    private static final long serialVersionUID = -6467419236822483534L;
    /**
     * 图片url
     */
    private String image;
    /**
     * 文字
     */
    private String text;
    /**
     * 是否选中
     * 0:未选中  1:选中
     */
    private Integer selected = 0;
    /**
     * 筛选项参数
     * 选中时，会被设置到
     * @see com.biyao.search.ui.remote.request.UISearchRequest#imageFilterParam
     */
    private String param;
}
