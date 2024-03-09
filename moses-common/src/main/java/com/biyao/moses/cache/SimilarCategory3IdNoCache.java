package com.biyao.moses.cache;

import com.biyao.moses.util.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * @ClassName SimilarCategory3IdNoCache
 * @Description 相似三级类目缓存
 * @Author xiaojiankai
 * @Date 2020/1/17 10:33
 * @Version 1.0
 **/
@Slf4j
public class SimilarCategory3IdNoCache {

    @Value("${similar.cate3Id.urlPath}")
    private String similarCate3UrlPath;

    //key 为后台三级类目ID，value 为相似三级类目ID
    private Map<Long, Long> similarCate3Map = new HashMap<>();

    protected void init(){
      refresh();
    }

    /**
     * 刷新相似三级类目配置信息
     */
    public void refresh(){
        log.info("[任务进度][相似三级类目缓存]开始刷新相似三级类目缓存");
        if(StringUtils.isBlank(similarCate3UrlPath)){
            log.error("[严重异常][相似三级类目缓存]刷新相似三级类目缓存失败，文件路径为空");
            return;
        }

        Map<Long, Long> similarCate3MapTmp = new HashMap<>();
        List<String> similarCate3LineList;
        try{
            if (similarCate3UrlPath.startsWith("http://") || similarCate3UrlPath.startsWith("https://")) {
                similarCate3LineList = FileUtil.getRemoteFile(similarCate3UrlPath);
            } else {
                similarCate3LineList = FileUtil.getFileFromLocal(similarCate3UrlPath, false);
            }

            //过滤掉不合法的行
            similarCate3LineList.stream()
                    .filter(l -> l.trim().length() > 0)
                    .filter(l -> !l.trim().startsWith("#"))
                    .filter(l -> l.trim().split("\t").length == 2)
                    .forEach(l -> {
                        try {
                            String[] similarCate3Array = l.trim().split("\t");
                            String similarIdStr = similarCate3Array[0].trim();
                            if (StringUtils.isBlank(similarIdStr)) {
                                return;
                            }
                            Long similarId = Long.valueOf(similarIdStr);
                            String[] cate3IdArray = similarCate3Array[1].trim().split(",");
                            for(String cate3IdStr : cate3IdArray) {
                                if(StringUtils.isBlank(cate3IdStr)){
                                    continue;
                                }
                                Long cate3Id = Long.valueOf(cate3IdStr);
                                similarCate3MapTmp.put(cate3Id, similarId);
                            }
                        }catch (Exception e){
                            log.error("[严重异常][相似三级类目缓存]解析相似三级类目错误， line {}， e ", l, e);
                        }
                    });
        }catch (Exception e){
            log.error("[严重异常][相似三级类目缓存]处理相似三级类目配置文件失败， e ", e);
        }

        if(similarCate3MapTmp.size() > 0){
            similarCate3Map = similarCate3MapTmp;
            log.info("[任务进度][相似三级类目缓存]结束刷新相似三级类目缓存，后台三级类目ID数量 {}", similarCate3Map.size());
        }else{
            log.error("[严重异常][相似三级类目缓存]获取相似三级类目信息为空，不刷新缓存");
        }

    }

    /**
     * 根据后台三级类目ID获取相似三级类目ID
     * @param cate3Id
     * @return
     */
    public Long getSimilarCate3Id(Long cate3Id){
        if(cate3Id == null){
            return null;
        }
        return similarCate3Map.getOrDefault(cate3Id, cate3Id);
    }

}
