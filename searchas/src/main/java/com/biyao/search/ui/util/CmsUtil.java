package com.biyao.search.ui.util;


import com.biyao.cms.client.common.bean.ImageDto;
import com.biyao.cms.client.material.dto.*;
import com.biyao.search.ui.remote.common.SpringContextDzHolder;
import com.biyao.search.ui.service.impl.MaterialQueryRpcServiceImpl;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author by 李志伟
 * Create Date: 2019/4/24 14:50
 * Description:
 */
public class CmsUtil {
    private static Logger logger = LoggerFactory.getLogger(CmsUtil.class);
    /**
     * 缓存中最大数量
     */
    public static final int MAXIMUM_SIZE = 300;
    /**
     * 缓存过期时间
     */
    public static final int DURATION = 10;
    /**
     * 并发数
     */
    public static final int CONCURRENCY_LEVEL = 8;
    /**
     * cms受不了，做个内存缓存
     */
    private static LoadingCache<Long, Optional<MaterialElementBaseDTO>> materialElementCache = CacheBuilder.newBuilder()
            //设置并发级别为x，并发级别是指可以同时写缓存的线程数
            .concurrencyLevel(CONCURRENCY_LEVEL)
            //设置写缓存后x秒钟过期
            .expireAfterWrite(DURATION, TimeUnit.SECONDS)
            //设置缓存容器的初始容量为10
            .initialCapacity(MAXIMUM_SIZE)
            //设置缓存最大容量为100，超过100之后就会按照LRU最近虽少使用算法来移除缓存项
            .maximumSize(MAXIMUM_SIZE)
            //设置要统计缓存的命中率
            .recordStats()
            //设置缓存的移除通知
            .removalListener(notification -> logger.info(notification.getKey() + " 移除，原因：" + notification.getCause()))
            //build方法中可以指定CacheLoader，在缓存不存在时通过CacheLoader的实现自动加载缓存
            .build(
                    new CacheLoader<Long, Optional<MaterialElementBaseDTO>>() {
                        @Override
                        public Optional<MaterialElementBaseDTO> load(Long id) {
                            logger.info("未命中缓存，id={}", id);
                            Map<Long, MaterialElementBaseDTO> longMaterialElementBaseDTOMap = queryMaterialFromCms(Collections.singletonList(id));
                            return Optional.ofNullable(longMaterialElementBaseDTOMap.get(id));
                        }
                    }
            );

    /**
     * 根据编辑查询素材
     *
     * @param materialIdIn 此list中的id不要随处乱写，一定要写到@see MaterialId 类中
     * @return
     */
    private static Map<Long, MaterialElementBaseDTO> queryMaterialFromCms(List<Long> materialIdIn) {
        if (CollectionUtils.isEmpty(materialIdIn)) {
            return MapUtils.EMPTY_MAP;
        }

        MaterialQueryRpcServiceImpl bean = SpringContextDzHolder.getApplicationContext().getBean(MaterialQueryRpcServiceImpl.class);
        try {
            MaterialQueryParamDTO materialQueryParamDTO = new MaterialQueryParamDTO();
            materialQueryParamDTO.setCaller(ConstantUtil.SERVICE_NAME);
            List<Long> distinctCollect = materialIdIn.stream().distinct().collect(Collectors.toList());
            materialQueryParamDTO.setMaterialIdIn(distinctCollect);
            Map<Long, MaterialElementBaseDTO> result = bean.queryMaterial(materialQueryParamDTO);
            return result;
        } catch (Exception e) {
            return MapUtils.EMPTY_MAP;
        }
    }

    /**
     * 批量查询
     *
     * @param materialIdIn
     * @return
     */
    public static Map<Long, MaterialElementBaseDTO> queryMaterial(List<Long> materialIdIn) {
        try {
            List<Long> distinctCollect = materialIdIn.stream().distinct().collect(Collectors.toList());
            ImmutableMap<Long, Optional<MaterialElementBaseDTO>> allOption = materialElementCache.getAll(distinctCollect);
            Map<Long, MaterialElementBaseDTO> collect = allOption.values().stream().filter(Optional::isPresent).map(Optional::get).
                    collect(Collectors.toMap(MaterialElementBaseDTO::getMaterialElementId, v -> v));
            return collect;
        } catch (Exception e) {
            return MapUtils.EMPTY_MAP;
        }
    }

    /**
     * 查询单个，调用方要做null判断
     *
     * @param id
     * @return
     */
    public static MaterialElementBaseDTO queryMaterial(long id) {
        try {
            Optional<MaterialElementBaseDTO> materialElementBaseDTO = materialElementCache.getUnchecked(id);
            return materialElementBaseDTO.orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 根据id，得到value，调用方要做null判断
     *
     * @param id
     * @return
     */
    public static String getMaterialValue(long id) {
        MaterialElementBaseDTO materialElementBaseDTO = queryMaterial(id);
        return Optional.ofNullable(materialElementBaseDTO).map(v -> v.getValue().toString()).orElse(null);
    }

    /**
     * 根据id，得到value，调用方要做null判断
     *
     * @param id
     * @return
     */
    public static <T> T getMaterialValueV2(long id) {
        MaterialElementBaseDTO materialElementBaseDTO = queryMaterial(id);
        return Optional.ofNullable(materialElementBaseDTO).map(v -> (T) v.getValue()).orElse(null);
    }

    /**
     * 得到图片url
     *
     * @param id
     * @return
     */
    public static String getMaterialImage(long id) {
        MaterialElementBaseDTO materialElementBaseDTO = queryMaterial(id);
        if (materialElementBaseDTO == null) {
            return null;
        }
        if (materialElementBaseDTO instanceof MaterialElementImageDto) {
            ImageDto imgDTO = (ImageDto) materialElementBaseDTO.getValue();
            if (imgDTO == null) {
                return null;
            }
            String webpImageUrl = imgDTO.getWebpImageUrl();
            if (StringUtils.isNotBlank(webpImageUrl)) {
                return webpImageUrl;
            }
            return imgDTO.getOriginUrl();
        }
        return null;
    }

    /**
     * 得到图片url
     *
     * @param id
     * @return
     */
    public static String getMaterialImageOrigin(long id) {
        MaterialElementBaseDTO materialElementBaseDTO = queryMaterial(id);
        if (materialElementBaseDTO == null) {
            return null;
        }
        if (materialElementBaseDTO instanceof MaterialElementImageDto) {
            ImageDto imgDTO = (ImageDto) materialElementBaseDTO.getValue();
            if (imgDTO != null) {
                String originUrl = imgDTO.getOriginUrl();
                if (StringUtils.isNotBlank(originUrl)) {
                    return originUrl;
                }
            }
        }
        return null;
    }


    /**
     * 得到单行文本
     *
     * @param id
     * @return
     */
    public static String getMaterialInput(long id) {
        MaterialElementBaseDTO materialElementBaseDTO = queryMaterial(id);
        if (materialElementBaseDTO != null && materialElementBaseDTO instanceof MaterialElementTextDTO) {
            String str = (String) materialElementBaseDTO.getValue();
            if (com.alibaba.dubbo.common.utils.StringUtils.isNotEmpty(str)) {
                return str;
            }
        }
        return null;
    }
}
