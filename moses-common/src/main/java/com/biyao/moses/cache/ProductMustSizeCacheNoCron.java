package com.biyao.moses.cache;

import com.biyao.moses.common.constant.RedisKeyConstant;
import com.biyao.moses.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class ProductMustSizeCacheNoCron {

    @Autowired
    private RedisUtil redisUtil;
    //  <productid,黄金尺码是否充足（0不足、1足）>
    private Map<String, String> mustSizeMap = new HashMap<String, String>();


    /**
     * 初始化
     */
    protected void init() {
        log.info("初始化黄金尺码是否充足缓存...");
        refreshProductMustSizeCache();
    }

    /**
     * 每2分钟刷新一次
     */
    protected void refreshProductMustSizeCache() {

        //获取全量商品以及黄金尺码是否充足
        try {
            Map<String, String> tmpMap = redisUtil.hgetAll(RedisKeyConstant.MOSES_PRODUCT_MUST_SIZE);
            if (tmpMap != null && tmpMap.size() > 0) {
                mustSizeMap = tmpMap;
            }
        } catch (Exception e) {
            log.error("获取全量商品以及黄金尺码是否充足信息失败", e);
        }

    }

    /**
     * 获取全量商品以及黄金尺码是否充足Map
     *
     * @return
     */
    public Map<String, String> getMustSizeMap() {
        return mustSizeMap;
    }

    /**
     * 判断商品黄金尺码是否充足
     *
     * @param productId
     * @return
     */
    public boolean isMustSizeFull(Long productId) {
        String rt = mustSizeMap.get(String.valueOf(productId));
        if (StringUtils.isBlank(rt)) {
            return true;
        }
        if ("0".equals(rt)) {
            return false;
        }
        if ("1".equals(rt)) {
            return true;
        }
        return true;
    }
}

