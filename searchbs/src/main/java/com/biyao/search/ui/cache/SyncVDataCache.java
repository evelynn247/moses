package com.biyao.search.ui.cache;

import com.alibaba.dubbo.common.utils.StringUtils;
import com.biyao.css.dubbo.client.sync.ISyncSearchSystemToBService;
import com.biyao.css.dubbo.dto.sync.SyncCelebrityDto;
import com.biyao.css.dubbo.dto.synonyms.SearchWordsDto;
import com.biyao.css.dubbo.param.sync.SyncCelebrityParam;
import com.biyao.css.dubbo.result.Result;
import com.biyao.search.ui.constant.CommonConstant;
import com.biyao.search.ui.enums.VModelTypeEnum;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;

/**
 * @program: search-ui
 * @description 从SRM拉取全量有效大V和企业定制用户数据，5分钟执行一次定时任务写到本地缓存
 * @author: xiafang
 * @create: 2019-10-16 14:33
 **/
@Component
@Slf4j
@Data
public class SyncVDataCache {

    @Autowired
    ISyncSearchSystemToBService syncVDataDubboService;
    /**
     * SRM接口要求每次请求数据量，取值为1~100，不能为null
     */
    private static final int PAGE_SIZE_LIMIT = 100;

    /**
     * 大VId和大V原始数据的映射
     */
    private Map<Long, SyncCelebrityDto> celebrityDtoMap = new HashMap();
    /**
     * key为大V昵称/搜索词/搜索词的同义词，value为对应的大VId
     */
    private Map<String, Long> celebritySearchWordMap = new HashMap<>();

    /**
     * 企业定制用户Id和原始数据的映射,大运河V1.2新增
     */
    private Map<Long, SyncCelebrityDto> enterpriseDtoMap = new HashMap();
    /**
     * key为企业定制用户昵称/搜索词/搜索词的同义词，value为对应的Id,大运河V1.2新增
     */
    private Map<String, Long> enterpriseSearchWordMap = new HashMap<>();
    /**
     * 平台号数据Id和原始数据的映射, uadteDate:20200518
     */
    private Map<Long, SyncCelebrityDto> platformDtoMap = new HashMap<Long, SyncCelebrityDto>();
    /**
     * key为平台号昵称/搜索词/搜索词的同义词，value为对应的Id , uadteDate:20200518
     */
    private Map<String, Long> platformSearchWordMap = new HashMap<String, Long>();

    /**
     * 获取大V数据ById
     * @param createUid
     * @return
     */
    public SyncCelebrityDto getVMapById(Long createUid){
        if(createUid == null){
            return null;
        }
        return celebrityDtoMap.get(createUid);
    }

    /**
     * 获取企业定制号数据ById
     * @param createUid
     * @return
     */
    public SyncCelebrityDto getEnterpriseMapById(Long createUid){
        if(createUid == null){
            return null;
        }
        return enterpriseDtoMap.get(createUid);
    }

    @PostConstruct
    public void init() {
        log.info("[任务报告]同步大V/企业定制用户/平台号数据，系统启动时初始化到本地缓存--》start");
        refreshCache();
        log.info("[任务报告]同步大V/企业定制用户/平台号数据，系统启动时初始化到本地缓存--》end");
    }

    /**
     * 从SRM拉取大V和企业定制用户数据,5分钟同步一次全量数据
     * 将拉取数据存在本地缓存中
     */
    public void refreshCache() {
        SyncCelebrityParam param = new SyncCelebrityParam();
        param.setCallSysName(CommonConstant.SYSTEM_NAME);
        param.setPageSize(PAGE_SIZE_LIMIT);
        Map<Long, SyncCelebrityDto> tempCelebrityDtoMap = new HashMap<Long, SyncCelebrityDto>();
        Map<String, Long> tempCelebritySearchWordMap = new HashMap<String, Long>();
        Map<Long, SyncCelebrityDto> tempEnterpriseDtoMap = new HashMap<Long, SyncCelebrityDto>();
        Map<String, Long> tempEnterpriseSearchWordMap = new HashMap<String, Long>();
        //平台号
        Map<Long, SyncCelebrityDto> tempPlatformDtoMap = new HashMap<Long, SyncCelebrityDto>();
        Map<String, Long> tempPlatformSearchWordMap = new HashMap<String, Long>();
        
        boolean exceptionFlag = false;
        try {
            while (true) {
                Result<List<SyncCelebrityDto>> rpcResult;
                try {
                    rpcResult = syncVDataDubboService.celebrity(param);
                } catch (Exception e) {
                    exceptionFlag = true;
                    log.error("[严重异常]同步大V/企业定制用户/平台号数据，SRM同步数据接口异常:", e);
                    break;
                }
                if (rpcResult == null || !rpcResult.isSuccess()) {
                    exceptionFlag = true;
                    log.error("[严重异常]同步大V/企业定制用户/平台号数据，SRM同步数据接口返回null");
                    break;
                }
                List<SyncCelebrityDto> tempSyncCelebrityDtoList = rpcResult.getObj();
                if (tempSyncCelebrityDtoList == null || tempSyncCelebrityDtoList.size() == 0) {
                    break;
                }
                for (SyncCelebrityDto syncCelebrityDto : tempSyncCelebrityDtoList) {
                    if (syncCelebrityDto.getRoleType() == null) {
                        continue;
                    }
                    String vtype = syncCelebrityDto.getRoleType().toString();
                    if (VModelTypeEnum.DAV.getCode().equals(vtype)) {
                        analyseSyncCelebrityDto(syncCelebrityDto, tempCelebrityDtoMap, tempCelebritySearchWordMap);
                    } else if (VModelTypeEnum.ENTERPRISE.getCode().equals(vtype)) {
                        analyseSyncCelebrityDto(syncCelebrityDto, tempEnterpriseDtoMap, tempEnterpriseSearchWordMap);
                    } else if(VModelTypeEnum.PLATFORM.getCode().equals(vtype)) {
                    	analyseSyncCelebrityDto(syncCelebrityDto, tempPlatformDtoMap, tempPlatformSearchWordMap);
                    }
                }
                //上一页最后一个用户id
                param.setCustomerIdGt(tempSyncCelebrityDtoList.get(tempSyncCelebrityDtoList.size() - 1).getCustomerId());
            }
        } catch (Exception e) {
            log.error("[严重异常]大V和企业定制用户数据转换异常:", e);
        }
        if (!exceptionFlag) {
            log.info("SRM数据成功同步到本地缓存");
            log.info("本次同步大V数据量{}:,上次同步大V数据量：{}", tempCelebrityDtoMap.size(), celebrityDtoMap.size());
            log.info("本次同步企业定制用户数据量{}:,上次同步企业定制用户数据量：{}", tempEnterpriseDtoMap.size(), enterpriseDtoMap.size());
            log.info("本次同步平台号数据量{}:,上次同步平台号数据量：{}", tempPlatformDtoMap.size(), platformDtoMap.size());
            
            this.celebrityDtoMap = tempCelebrityDtoMap;
            this.celebritySearchWordMap = tempCelebritySearchWordMap;
            this.enterpriseDtoMap = tempEnterpriseDtoMap;
            this.enterpriseSearchWordMap = tempEnterpriseSearchWordMap;
            
            this.platformDtoMap = tempPlatformDtoMap;
            this.platformSearchWordMap  = tempPlatformSearchWordMap;
        }
    }

    /**
     * 该方法将SRM接口返回的数据对象转换数据格式存储在本地缓存
     *
     * @param syncCelebrityDto  SRM接口定义的数据对象
     * @param tempDtoMap        用户Id和用户原始数据的映射
     * @param tempSearchWordMap 用户昵称/搜索词/搜索词的同义词为key,用户Id为value的map
     */
    private void analyseSyncCelebrityDto(SyncCelebrityDto syncCelebrityDto, Map<Long, SyncCelebrityDto> tempDtoMap, Map<String, Long> tempSearchWordMap) {
        if (syncCelebrityDto == null) {
            return;
        }
        Long customerId = syncCelebrityDto.getCustomerId();
        if (customerId != null && customerId > 0 && StringUtils.isNotEmpty(syncCelebrityDto.getNickname())) {
            tempDtoMap.put(customerId, syncCelebrityDto);
            tempSearchWordMap.put(syncCelebrityDto.getNickname().toUpperCase(), customerId);
            if (StringUtils.isNotEmpty(syncCelebrityDto.getRealName())) {
                tempSearchWordMap.put(syncCelebrityDto.getRealName().toUpperCase(), customerId);
            }
            SearchWordsDto searchWordsDto = syncCelebrityDto.getSearchWords();
            if (searchWordsDto == null) {
                return;
            }
            searchWordsDto.getSynonymsList().forEach(synonymsDto -> {
                if (synonymsDto != null) {
                    if (StringUtils.isNotEmpty(synonymsDto.getSynonym())) {
                        tempSearchWordMap.put(synonymsDto.getSynonym().toUpperCase(), customerId);
                    }
                }
            });
            if (StringUtils.isNotEmpty(searchWordsDto.getSearchWord())) {
                tempSearchWordMap.put(searchWordsDto.getSearchWord().toUpperCase(), customerId);
            }
        }
    }
}
