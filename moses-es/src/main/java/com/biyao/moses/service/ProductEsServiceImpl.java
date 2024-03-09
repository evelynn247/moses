package com.biyao.moses.service;

import com.alibaba.fastjson.JSONObject;
import com.biyao.moses.Enum.IndexTypeEnum;
import com.biyao.moses.pdc.domain.ProductDomain;
import com.biyao.moses.pdc.impl.ProductDaoImpl;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.biyao.moses.common.CommonConstant.ONE_HOUR_MILLSECONDS;
import static com.biyao.moses.constant.CommonConstant.ONE_MINUTE;
import static com.biyao.moses.constant.CommonConstant.REFRESH_INTERVAL;
import static com.biyao.moses.constant.ElasticSearchConstant.*;

/**
 * @program: moses-parent-online
 * @description:
 * @author: changxiaowei
 * @create: 2021-09-17 10:58
 **/
@Service
@Slf4j
public class ProductEsServiceImpl implements IEsService {
    @Autowired
    EsConmonService esConmonService;
    @Autowired
    ProductDaoImpl productDao;
    private  final  static  int loopSize = 500;
    private  final  static  int PageSize = 500;

    public void init(){
      long start= System.currentTimeMillis();
      log.info("[检查日志]系统初始化，重建索引开始");
      rebulidIndex();
      log.info("[检查日志]系统初始化，重建索引结束,耗时：{}",System.currentTimeMillis()-start);
    }

    /**
     * 重建索引 每天一次
     */
    @Override
    @PostConstruct
    public void rebulidIndex(){
        String aliasName = PRODUCT_INDEX_ALIAS + esConmonService.getEnvName();
        // 创建新的索引
        String newIndex = esConmonService.creatIndex(IndexTypeEnum.PRODUCT.getType());
        if(newIndex == null){
            log.error("[严重异常]创建索引失败，需要人工介入");
            return;
        }
        // 获取当前别名指向的所有索引
        Set<String> indexSet = esConmonService.getIndexByAlias(aliasName);
        // 将商品数据批量导入es
        Long lastId = 0L;
        while (true){
            List<ProductDomain> productList = productDao.getProductsByLastId(lastId,PageSize);
            if(CollectionUtils.isEmpty(productList)){
                break;
            }
            lastId=  productList.get(productList.size()-1).getProductId();
            updateIndexByProductDomains(productList,newIndex);
        }
        // 如果当前别名有指向的索引 则删除并指向新的索引
        indexSet.forEach(index-> esConmonService.updateALias(index,aliasName,"remove"));
        // 直接指向新的索引
        esConmonService.updateALias(newIndex,aliasName,"add");
        // 更新es 的刷新频率
        esConmonService.updateSetting(REFRESH_INTERVAL,ONE_MINUTE);
    }

    @Override
    public void updateIndex(List<ProductDomain> productDomains) {

    }

    /**
    * @Des  更新前{}个小时内信息变更的商品到es
    * @Param [time] 小时
    * @return void
    * @Author changxiaowei
    * @Date  2021/12/17
    */
    @Override
    public void updateIndexByTime(int time){
        // 获取指定时间内更新的商品
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
        String date = sdf.format(System.currentTimeMillis()-time* ONE_HOUR_MILLSECONDS);
        // 查询数据库获取{time}小时前商品信息变更的商品
        List<ProductDomain> productList = productDao.getUpdateProductsByLimitTime(date);
        if(!CollectionUtils.isEmpty(productList)){
            // 获取别名指向最新的索引
            String indexName = esConmonService.getLatestIndexNameByAlais(PRODUCT_INDEX_ALIAS + esConmonService.getEnvName());
            // 更新索引
            updateIndexByProductDomains(productList,indexName);
        }
    }

    /**
     * 删除过期的索引  三天前的索引视为过期
     */
    @Override
    public void removeIndexTimer(){
        // 三天前的时间戳
        long day3Mills  = System.currentTimeMillis()-INDEX_EXPIRE_MILLS;
        // 获取所有的索引
        Set<String> allIndex = esConmonService.getAllIndex();
        Set<String> delIndex = new HashSet<>();
        // 判读该索引是否应该被删除格式 product_envName_时间戳  删除三天前的索引
        for (String indexName :allIndex){
            if(!indexName.startsWith(PRODUCT_INDEX_PREFIX)){
                continue;
            }
            String[] split = indexName.split("_");
            Long indexCreateTime = Long.valueOf(split[split.length - 1]);
            if(indexCreateTime < day3Mills){
                delIndex.add(indexName);
            }
        }
        // 批量删除索引
        if(!CollectionUtils.isEmpty(delIndex)){
            esConmonService.deleteIndex(delIndex.toArray(new String[0]));
        }
    }

 /**
  * @Des 根据商品id 更新es中的商品数据
  * @Param [pids]
  * @return void
  * @Author changxiaowei
  * @Date  2021/12/16
  */
    @Override
    public void updateIndexByPids(List<Long> pids) {
        log.info("[检查日志]更新es商品信息，pids:{}", JSONObject.toJSONString(pids));
        if(CollectionUtils.isEmpty(pids)){
            return;
        }
        // 查询数据库中本批商品的信息
        List<ProductDomain> productList = productDao.getProductInfoByPidList(pids);
        //根据索引别名 获取最新的索引
        String indexName = esConmonService.getLatestIndexNameByAlais(PRODUCT_INDEX_ALIAS + esConmonService.getEnvName());
        // 更新es
        updateIndexByProductDomains(productList,indexName);
    }

    /**
     * @Des 更新指定商品索引
     * @Param [productList] 完整商品信息集合
     *  @Param [indeName] 索引名字
     * @return void
     * @Author changxiaowei
     * @Date  2021/12/17
     */
    private void  updateIndexByProductDomains(List<ProductDomain> productList,String indexName) {
        if(CollectionUtils.isEmpty(productList)){
            return;
        }
        // 获取当前别名指向的最新索引
        int size = productList.size();
        int temp = size / loopSize;
        for (int i = 0; i < temp+1; i++) {
            int end = (i + 1) * loopSize;
            // 如果剩下的不足 loopSize 则取集合最后一位
            if (end > size) {
                end = size;
            }
            List<ProductDomain> loopList = productList.subList(i * loopSize, end);
            if(CollectionUtils.isEmpty(loopList)){
                break;
            }
            BulkRequest bulkRequest = esConmonService.buildProductBulkRequest(loopList, indexName);
            esConmonService.blukUpdateDoc(bulkRequest);
        }
    }




}
