package com.biyao.moses.po;

import lombok.Data;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * @program: moses-parent-online
 * @description:
 * @author: changxiaowei
 * @Date: 2022-03-07 18:09
 **/
@Data
public class Video {

    private final static int selected =1;
    private final static int platform =2;
    private final static int slide =3;

    public Video() {

    }
    private Integer vid;
    private Float score;
    private Integer expo;
    private String publish_time;
    //   1:商家视频、2:用户评价、3:平台视频、4：详情页视频
    public Integer type;
    public List<Byte> channel;

    public static boolean isValid(Video video){
        return video != null &&
                video.getVid() != null &&
                !CollectionUtils.isEmpty(video.channel);
    }
}
