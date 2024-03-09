package com.biyao.moses.params;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang.StringUtils;

/**
 * @ClassName UICommonRequest
 * @Description 公共请求结构
 * @Author xiaojiankai
 * @Date 2019/8/5 19:52
 * @Version 1.0
 **/
@Setter
@Getter
public class UICommonRequest {
    private String pf;
    private String avn;
    /**
     * uuid,必传
     */
    private String uuid;
    private String uid;
    private String siteId;
    private String device;
    /**
     * 页面唯一标识，页面刷新时重新生成pvid，32位字符串，必传
     */
    private String pvid;
    private String did;
    private String ctp;
    private String stp;

    /**
     * 标签ID，必传
     */
    private String tagId;
    /**
     * 场景ID，必传
     * 1: 一起拼运营聚合页， 2：特权金下发成功页  3任务中心常规页
     */
    private String sceneId;
    /**
     * 需排序的全量商品pid信息，pid之间按“,”分隔。上限为600，超过600返回前600个排序后的结果。
     * 当pageIndex大于1时，pids为空字符串。
     * 当pageIndex=1时，pids为商品pid集合。
     */
    private String pids;
    /**
     * 当前页id，用于分页查询（正整数），必传，返回全量数据时传入空字符串
     */
    private String pageIndex;
    /**
     * 每页大小，用于分页查询（正整数）必传，返回全量数据是时入空字符串
     */
    private String pageSize;

    public boolean validate(){
        boolean result = true;
        //必传参数校验
        if(StringUtils.isBlank(uuid) || StringUtils.isBlank(pvid)
            || StringUtils.isBlank(sceneId) || StringUtils.isBlank(tagId)){
            return false;
        }

        try{
            if(StringUtils.isNotBlank(pageIndex) && StringUtils.isNotBlank(pageSize)){
                //pageIndex和pageSize都有值时，要求：1 其值为不能为负数； 2 pageIndex为1时，pids必须有数据
                int index = Integer.valueOf(pageIndex);
                int size = Integer.valueOf(pageSize);
                if(index < 0 || size <= 0){
                    return false;
                }
                if(index == 1 && StringUtils.isBlank(pids)){
                    return false;
                }
            }else if(StringUtils.isBlank(pageIndex) && StringUtils.isBlank(pageSize)){
                //pageIndex和pageSize都没有值时，要求：1 pids必须有数据
                if(StringUtils.isBlank(pids)){
                    return false;
                }
            }else{
                //pageIndex和pageSize 只有一个有值时，入参不符合要求
                return false;
            }
        }catch (Exception e){
            result = false;
        }

        return result;
    }
}
