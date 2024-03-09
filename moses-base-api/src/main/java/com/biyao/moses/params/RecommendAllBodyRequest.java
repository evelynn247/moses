package com.biyao.moses.params;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * @ClassName RecommendAllBodyRequest
 * @Description 放在请求体body中的参数
 * @Author xiaojiankai
 * @Date 2020/3/30 14:01
 * @Version 1.0
 **/
@Getter
@Setter
public class RecommendAllBodyRequest {
    /**
     * 用户设备id
     */
    private String uuid;
    /**
     * 用户id
     */
    private String uid;
    /**
     * 视频间隔
     */
    private Integer videoInterval;
    /**
     * 最后一个被替换的位置
     */
    private Integer  lastReplacePosition;
    /**
     * 当前页码
     */
    private Integer pageIndex;
    /**
     * 页码尺寸
     */
    private Integer pageSize;

    private String caller;
    /**
     * 需要查找相似商品的pid集合信息，非必传。
     */
    private List<String> pids;
    /**
     * 必要朋友V2.0项目新增
     * 好友已购业务表示：好友uid列表
     * 格式为：id1，id2，id3
     */
    private String responseMapKeys;
    /**
     * cms配置的插入的视频信息
     */
    private List<AdvertParam> videoInfo;
    /**
     * 查询视频分的视频id
     */
    private List<Long> videoIds;

    /**
     * 渠道   必要商城 1  必要分销 2 鸿源分销 3
     */
    private Integer channelType;
    public boolean isValidForGetVid(){
        /**
         * 调用者不为空
         */
        if(StringUtils.isEmpty(caller)){
            return false;
        }
        if(videoInterval == null || pageSize ==null || pageIndex ==null || CollectionUtils.isEmpty(pids)){
            return false;
        }
        return true;
    }


    public boolean isValidForGetVidScore(){
        /**
         * 调用者不为空
         */
        if(StringUtils.isEmpty(caller)){
            return false;
        }
        /**
         * 视频ids不能为空
         */
        if(videoIds == null || CollectionUtils.isEmpty(videoIds)){
            return false;
        }
        /**
         * 视频ids最大200条，如果超过200条，则只截取200条
         */
        if(videoIds.size() > 200){
            this.videoIds = videoIds.subList(0,200);
        }
        return true;
    }
}
