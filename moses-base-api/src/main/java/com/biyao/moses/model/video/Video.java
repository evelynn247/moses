package com.biyao.moses.model.video;

import lombok.Data;
import org.apache.commons.collections.CollectionUtils;

import java.util.List;

/**
 * @program: moses-parent
 * @description: VideoInfo
 * @author: changxiaowei
 * @Date: 2022-02-18 17:52
 **/
@Data
public class Video {

    private final static int selected =1;
    private final static int platform =2;
    private final static int slide =3;

    public Video(Integer vid, Float score, Integer expo, String publish_time, Integer type) {
        this.vid = vid;
        this.score = score;
        this.expo = expo;
        this.publish_time = publish_time;
        this.type = type;
    }
    public Video() {

    }
    private Integer vid;
    private Float score;
    private Integer expo;
    private String publish_time;
    //   1:商家视频、2:用户评价、3:平台视频、4：详情页视频
    public Integer type;
    public List<Integer> channel;

    public static boolean isValid(Video video,int type){
        return video != null &&
                video.getVid() != null &&
                video.getScore() != null &&
                !CollectionUtils.isEmpty(video.channel) &&
                video.channel.contains(type);
    }

    public static boolean isSlide(Video video){
        return slide == video.getType();
    }

    public static boolean isValid(Video video){
        return video != null &&
                video.getVid() != null &&
                video.getScore() != null ;
    }
}
