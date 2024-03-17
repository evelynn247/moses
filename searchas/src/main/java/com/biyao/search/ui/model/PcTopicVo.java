package com.biyao.search.ui.model;

import com.biyao.search.ui.home.model.pc.PcTopic;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 *
 * @author maping
 * @date 2018/8/30 16:52
 * To change this template use File | Settings | File and Code Templates.
 */
public class PcTopicVo {
    /**
     * floorId
     */
    private Integer floorId;
    /**
     * 楼层主标题
     */
    private String floorTitle;
    /**
     * 楼层副标题
     */
    private String floorViceTitle;
    /**
     * 楼层类型
     */
    private Integer floorType;
    /**
     * topic主标题
     */
    private String topicTitle;
    /**
     * topic副标题
     */
    private String topicViceTitle;
    /**
     * 小图
     */
    private List<String> littleImages;
    /**
     * 大图
     */
    private List<String> largeImages;
    /**
     * 商家Id
     */
    private Integer supplierId;

    public Integer getFloorId() {
        return floorId;
    }

    public void setFloorId(Integer floorId) {
        this.floorId = floorId;
    }

    public String getFloorTitle() {
        return floorTitle;
    }

    public void setFloorTitle(String floorTitle) {
        this.floorTitle = floorTitle;
    }

    public String getFloorViceTitle() {
        return floorViceTitle;
    }

    public void setFloorViceTitle(String floorViceTitle) {
        this.floorViceTitle = floorViceTitle;
    }

    public Integer getFloorType() {
        return floorType;
    }

    public void setFloorType(Integer floorType) {
        this.floorType = floorType;
    }

    public String getTopicTitle() {
        return topicTitle;
    }

    public void setTopicTitle(String topicTitle) {
        this.topicTitle = topicTitle;
    }

    public String getTopicViceTitle() {
        return topicViceTitle;
    }

    public void setTopicViceTitle(String topicViceTitle) {
        this.topicViceTitle = topicViceTitle;
    }

    public List<String> getLittleImages() {
        return littleImages;
    }

    public void setLittleImages(List<String> littleImages) {
        this.littleImages = littleImages;
    }

    public List<String> getLargeImages() {
        return largeImages;
    }

    public void setLargeImages(List<String> largeImages) {
        this.largeImages = largeImages;
    }

    public Integer getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Integer supplierId) {
        this.supplierId = supplierId;
    }

    @Override
    public String toString() {
        return "PcTopicVo{" +
                "floorId=" + floorId +
                ", floorTitle='" + floorTitle + '\'' +
                ", floorViceTitle='" + floorViceTitle + '\'' +
                ", floorType=" + floorType +
                ", topicTitle='" + topicTitle + '\'' +
                ", topicViceTitle='" + topicViceTitle + '\'' +
                ", littleImages=" + littleImages +
                ", largeImages=" + largeImages +
                ", supplierId=" + supplierId +
                '}';
    }
}
