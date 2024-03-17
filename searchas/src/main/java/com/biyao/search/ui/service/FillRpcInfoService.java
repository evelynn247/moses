package com.biyao.search.ui.service;

import com.biyao.search.ui.constant.CmsSwitch;
import com.biyao.search.ui.remote.response.BlockData;
import com.biyao.search.ui.remote.response.SearchProduct;
import com.biyao.search.ui.remote.response.Template;
import com.biyao.search.ui.remote.response.TemplateData;
import com.biyao.search.ui.rpc.VideoRpcService;
import com.biyao.search.ui.util.CmsUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class FillRpcInfoService {

    @Autowired
    private VideoRpcService videoRpcService;
    /**
     * 查询远程接口并填充商品信息
     * @param blockDatas
     * @return
     */
    public void fillProductInfo(List<BlockData> blockDatas){
        if (blockDatas == null) {
            return;
        }
        try {
            Set<Long> pidSet = getPidSet(blockDatas);
            Map<Long, Boolean> productVideoStatusMap = rpcInterfaceInvoke(pidSet);
            fillProductByRpcInfo(blockDatas, productVideoStatusMap);
        } catch (Exception e) {
            log.error("[一般异常]商品填充视频标识异常", e);
        }
    }

    /**
     * 根据远程接口查询结果填充商品的活动信息
     * @param blockDatas
     * @param productVideoStatusMap
     */
    private void fillProductByRpcInfo(List<BlockData> blockDatas, Map<Long, Boolean> productVideoStatusMap) {
        if (MapUtils.isEmpty(productVideoStatusMap)) {
            return;
        }
        for(BlockData blockData: blockDatas) {
            List<Template> templates = blockData.getTemplates();
            //pid是否展示视频标识赋值给videoStatus字段，返回给前端
            for (Template item : templates) {
                List<TemplateData> templateDataList = item.getData();
                for (int i = 0; i < templateDataList.size(); i++) {
                    SearchProduct searchProduct = (SearchProduct) templateDataList.get(i);
                    Long pid = searchProduct.getProductId().longValue();
                    if (productVideoStatusMap.get(pid)) {
                        searchProduct.setVideoStatus("1");
                    }
                }
            }
        }
    }

    /**
     * 远程接口调用，根据spuId集合查询活动信息
     * @param pidSet
     * @return
     */
    private Map<Long, Boolean> rpcInterfaceInvoke(Set<Long> pidSet) {
        if (pidSet.size() == 0) {
            return null;
        }
        //通过getProductsLiveStatus()方法拿到商品是否有关联的视频标识
        Map<Long, Boolean> productVideoStatusMap = new HashMap<>(pidSet.size());
        if ("1".equals(CmsUtil.getMaterialValue(CmsSwitch.VIDEO_SWITCH_ID))) {
            productVideoStatusMap = videoRpcService.getProductsVideoStatus(pidSet);
        }
        return productVideoStatusMap;
    }

    /**
     * 搜索结果中拿到pid集合构建RPC接口请求
     * @param blockDatas
     * @return
     */
    private Set<Long> getPidSet(List<BlockData> blockDatas) {
        Set<Long> pidSet = new HashSet<>();
        for(BlockData blockData: blockDatas) {
            if (blockData == null) {
                return null;
            }
            List<Template> templates = blockData.getTemplates();
            for (Template item : templates) {
                List<TemplateData> templateDataList = item.getData();
                for (int i = 0; i < templateDataList.size(); i++) {
                    SearchProduct searchProduct = (SearchProduct) templateDataList.get(i);
                    pidSet.add(searchProduct.getProductId().longValue());
                }
            }
        }
        return pidSet;
    }
}
