package com.biyao.moses.params;

import com.biyao.moses.common.constant.CommonConstants;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import static com.biyao.moses.common.constant.CommonConstants.VIDEO_SCENDIDS;

/**
 * @ClassName RecommendAllRequest
 * @Description 放在url中的参数
 * @Author xiaojiankai
 * @Date 2020/3/30 14:01
 * @Version 1.0
 **/
@Getter
@Setter
@Slf4j
public class RecommendAllRequest {
    /**
     * 用户uuid，必传，非空
     */
    private String uuid;
    /**
     * 用户uid，必传。若没有，则传空字符串
     */
    private String uid;
    /**
     * 业务名称，必传，非空。加价购业务则传入"jjg"
     */
    private String biz;
    /**
     * 调用者，必传，非空。
     */
    private String caller;
    /**
     * 提前预留，防止其他业务也需要接入推荐获取全量商品信息，
     * 此时可以通过传入召回源及其权重信息来获取。
     * 格式为：source,weight|...|source,weight
     */
    private String sourceAndWeight;
    /**
     * 要求商品数量上限 不传则用默认
     */
    private String expNum;
    /**
     * 请求端编号 从 1，2，3，7，9中取值，其中：1 M站 ；2 miniapp ；3 PC ；7 IOS； 9 Android。
     */
    private String siteId;

    /**
     * 必要朋友V2.0项目新增
     * 好友已购业务表示：好友uid列表
     * 格式为：id1，id2，id3
     */
    private String responseMapKeys;
    /**
     * 场景id
     */
    private String sceneId;
    /**
     * 服务端生成的唯一id
     */
    private  String sid;
    /**
     * 机型
     */
    private String device;
    /**
     * 特权金下发页用户持有面额
     */
    private  String tqjMaxLimit;
    // 日志追踪
    private boolean debug =false;

    /**
     * 个性化推荐设置开关。
     * true表示开关打开，可以使用用户个性化数据做推荐
     * false表示开关关闭，不可以使用用户个性化数据做推荐
     */
    private boolean personalizedRecommendSwitch = true;
    /**
     * 是否需要过滤必要造物商品  0 =false 1 = true
     */
    private String isFilterByZw = "0";
    // 后台三级类目   多个以逗号分隔
    private String thirdCateGoryIdList;
    // 标签id   多个以逗号分隔
    private String tagIdList;
    // 前台类目
    private String frontendCategoryId;
    // 页面id
    private String pageId;
    //视频流落地页入口视频id
    private String entryVideoId;
    // 视频流落地页主商品id
    private Long entryProductId;
    // 页面编号 用于分页
    private Integer pageIndex;
    /**
     * 渠道  必要商城 1  必要分销 2 鸿源分销 3
     */
    private Integer channelType;
    /**
     * 校验参数
     * @return
     */
    public boolean isValid(){
        if(StringUtils.isBlank(uuid) || (StringUtils.isBlank(biz) && StringUtils.isBlank(sceneId)) || StringUtils.isBlank(caller)){
            return false;
        }
        // 如果是视频流落地页推荐
        if(VIDEO_SCENDIDS.contains(sceneId)){
            if(pageIndex == null || StringUtils.isEmpty(pageId)  || channelType == null ||  StringUtils.isBlank(entryVideoId)){
                return false ;
            }
        }
        if(StringUtils.isNotBlank(siteId) && !CommonConstants.SITEID.contains(siteId.trim())){
            log.error("[严重异常]siteId格式错误，uuid:{},biz:{},caller:{},siteId:{}",uuid,biz,caller,siteId);
            this.siteId = null;
        }
        if(StringUtils.isBlank(uid)){
            this.uid="0";
        }
        return true;
    }

    /**
     * 类目下商品召回参数校验
     * @return
     */
    public boolean isValidForCateGory(){
        // 前台类目必传
        if(StringUtils.isBlank(frontendCategoryId)){
            return false;
        }
        // 后台三级类目 和  tag 不能全为空
        if(StringUtils.isEmpty(thirdCateGoryIdList) && StringUtils.isEmpty(tagIdList)){
           return false;
        }
        return true;
    }
}
