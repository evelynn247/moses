package com.biyao.moses.util;

import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import static com.biyao.moses.common.constant.EsIndexConstant.*;
import static com.biyao.moses.constant.CommonConstant.*;
import static com.biyao.moses.constant.ElasticSearchConstant.*;
import static com.biyao.moses.constant.EsIndexTypeConstant.*;

/**
 * @program: moses-parent-online
 * @description:
 * @author: changxiaowei
 * @create: 2021-09-16 11:33
 **/
@Slf4j
public class ESUtils {
    /**
     * 普通商品 mapping
     * @return
     */
    public static XContentBuilder getProductMapping(){
        XContentBuilder mapping ;
        try {
            mapping =  JsonXContent.contentBuilder()
                    .startObject()
                        .startObject(PROPERTIES)
                            // 下面数据均来自数据库
                            .startObject(PRODUCT_ID).field(TYPE,LONG).endObject()
                            .startObject(FIRST_SHELLF_TIME).field(TYPE,DATE).endObject()
                            .startObject(SHORT_TITLE).field(TYPE,TEXT).endObject()
                            .startObject(SHELF_STATUS).field(TYPE,BYTE).endObject()
                            .startObject(SHOW_STATUS).field(TYPE,BYTE).endObject()
                            .startObject(SUPPORT_PLATFORM).field(TYPE,BYTE).endObject()
                            .startObject(SUPPORT_ACT).field(TYPE,BYTE).endObject()
                            .startObject(CATEGORY3ID).field(TYPE,LONG).endObject()
                            .startObject(CATEGORY2ID).field(TYPE,LONG).endObject()
                            .startObject(F_CATEGORY1ID).field(TYPE,LONG).endObject()
                            .startObject(F_CATEGORY3ID).field(TYPE,LONG).endObject()
                            .startObject(IS_CREATOR).field(TYPE,BYTE).endObject()
                            .startObject(NEW_PRIVILEGE).field(TYPE,BYTE).endObject()
                            .startObject(IS_TOGGROUP).field(TYPE,BYTE).endObject()
                            .startObject(NEW_PRIVILATE_DEDUCT).field(TYPE,LONG).endObject()
                            .startObject(SUPPORT_TEXTURE).field(TYPE,BYTE).endObject()
                            .startObject(TAGSID).field(TYPE,LONG).endObject()
                            //以下属性来自redis
                            .startObject(NEWV_PRODUCT).field(TYPE,BYTE).endObject()
                            .startObject(IS_VIDEO).field(TYPE,BYTE).endObject()
                            .startObject(HOT_SCORE).field(TYPE,FLOAT).endObject()
                            .startObject(FM_VECTOR).field(TYPE,DENSE_VECTOR).field(DIMS,8).endObject()
                            .startObject(ICF_VECTOR).field(TYPE,DENSE_VECTOR).field(DIMS,16).endObject()
                            .startObject(SEASON).field(TYPE,BYTE).endObject()
                            .startObject(SEX).field(TYPE,BYTE).endObject()
                            .startObject(VID_SUPPORT_PALTFORM).field(TYPE,INTEGER).endObject()
                            .startObject(SUPPORT_CHANNEL).field(TYPE,BYTE).endObject()
//                            .startObject(IS_TOGGROUP).field(TYPE,BYTE).endObject()
//                            .startObject(SUPPORT_MJQ).field(TYPE,LONG).endObject()
//                            .startObject(SUPPORT_FX).field(TYPE,TEXT).endObject()
                        .endObject()
                    .endObject();
        }catch (Exception e){
            log.error("[严重异常]创建mapping异常，异常信息：",e);
            return null;
        }
        //mappings设置
       return mapping;
    }


    /**
     * 设置分片数和副本数和刷新周期
     * @return
     */
    public static Settings.Builder getSettings(){
        return Settings.builder()
                .put(NUM_SHARDS,SHARDS_NUM)
                .put(NUM_REPLICAS,REPLICAS_NUM);
    }
}
