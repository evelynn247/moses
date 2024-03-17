package com.biyao.search.ui.rest.impl.module;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import com.alibaba.dubbo.rpc.RpcContext;
import com.biyao.search.common.constant.SearchLimit;
import com.biyao.search.common.constant.SearchStatus;
import com.biyao.search.common.model.Status;
import com.biyao.search.ui.cache.RedisDataCache;
import com.biyao.search.ui.constant.CommonConstant;
import com.biyao.search.ui.model.RequestBlock;
import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 初始化RequestBlock数据对象
 */
public class ModInitRequest implements UIModule {
    
    // 特殊字符正则表达式(特殊字符、emoji表情)
    private static final String SPECIAL_CHAR_REGEXP = "[`~!@#$%^&*()+=|{}':;',\\[\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]"
                                              +"|[\ud83c\udc00-\ud83c\udfff]|[\ud83d\udc00-\ud83d\udfff]|[\u2600-\u27ff]";
    private static Pattern pattern = Pattern.compile(SPECIAL_CHAR_REGEXP, Pattern.UNICODE_CASE|Pattern.CASE_INSENSITIVE);

    @Autowired
    RedisDataCache redisDataCache;
    /**
     * 初始化操作
     * 程序启动时执行一次
     */
    public void init(){}
    
    /**
     * 清理操作
     * 启动关闭时执行一次
     */
    public void destroy(){}

    /**
     * 处理请求
     */
    public Status run( RequestBlock request ) {

        // 重写或生成request中的部分参数：IP、pageSize、sid 、requestTime
        rewrite(request);
        
        // 参数校验
        if (parameterCheck(request) == false) {
            return SearchStatus.UI.BAD_REQUEST;
        }
        // 安全校验
        if (securityCheck(request) == false || redisDataCache.isBlockQuery(request.getQuery())) {
            return SearchStatus.UI.BAD_REQUEST;
        }
        
        return SearchStatus.OK;
    }

    /**
     * 参数校验
     */
    public static boolean parameterCheck(RequestBlock request) {
        // query
        if (Strings.isNullOrEmpty(request.getQuery())) {
            return false;
        }
        // uuid
        if (Strings.isNullOrEmpty(request.getUuid())) {
            return false;
        }

        // platform
        if (!CommonConstant.platforms.contains(request.getPlatform())) {
            return false;
        }

        // SID: sid要么为空字符串，要么是32位字符串
        String sid = request.getSid();
        if (sid != null && sid.length() != 32) {
            return false;
        }

        // pageIndex > 0
        if( request.getPageIndex() < 1 ) {
            return false;
        }
        return true;
    }

    /**
     * 重写或生成部分参数
     */
    public static void rewrite(RequestBlock request) {
        /**
         *  Query
         */
        if( request.getQuery() == null ) {
            request.setQuery("");
        }
        // 搜索词清理（清理掉里面的& = 等特殊字符）
        request.setQuery( clearSpecialChar(request.getQuery() ) );
        // 搜索词的长度不能大于SearchLimit.MAX_QUERY_LENGTH
        if( request.getQuery().length() > SearchLimit.MAX_QUERY_LENGTH ){
            request.setQuery( request.getQuery().substring(0, SearchLimit.MAX_QUERY_LENGTH));
        }
        
        /**
         *  sid: 如果sid为空，则表示是新的搜索，pageIndex改写为1
         */
        if (Strings.isNullOrEmpty(request.getSid())) {
            String sid = createSid(request.getUuid(), request.getQuery());
            request.setSid(sid);
            request.setPageIndex(1); 
            request.setScroll(false);  // 新的搜索请求
        } else {
            request.setScroll(true);   // 翻页查询
        }
        // pageSize
        request.setPageSize(20);
        // IP
        request.setIp( getRemoteIp() );
        // requestTime
        request.setRequestTime( System.currentTimeMillis() );
    }

    /**
     * 安全校验： 通过SID校验
     */
    public static boolean securityCheck(RequestBlock request) {
        String sid = request.getSid();
        String uuid = request.getUuid();
        String query = request.getQuery();

        HashFunction hf = Hashing.md5();
        String uuidMd5 = hf.newHasher().putString(uuid, Charsets.UTF_8).hash().toString();
        String queryMd5 = hf.newHasher().putString(query, Charsets.UTF_8).hash().toString();
        
        // SID非空：分页拉取查询结果
        String[] items = sid.split("-");
        if (items.length != 3) {
            return false;
        }
        
        if(! queryMd5.substring(0, 4).equals(items[1] )  ) {
            return false;
        }
        if(! uuidMd5.substring(0, 13).equals(items[2] )  ) {
            return false;
        }
        
        return true;
    }

    /**
     * 生成新的SID
     * SID的生成规则为：${timestamp_ms}-${query_md5_start4}-${uuid_md5_13}，其中：
     *     1. ${timestamp_ms}是当前时间戳（到毫秒），共13位；
     *     2. ${query_md5_start4}是查询词MD5 值（32位小写）的前4个字符，共4位；
     *     3. ${uuid_md5_13}是UUID MD5值（32位小写）的前13个字符。 
     * SID共32位长度。
     */
    public static String createSid(String uuid, String query) {
        HashFunction hf = Hashing.md5();
        String uuidMd5 = hf.newHasher().putString(uuid, Charsets.UTF_8).hash().toString();
        String queryMd5 = hf.newHasher().putString(query, Charsets.UTF_8).hash().toString();
        
        return String.format("%d-%s-%s", System.currentTimeMillis(),
                queryMd5.substring(0, 4), uuidMd5.substring(0, 13));
    }
    
    /**
     * 移除字符串中的特殊字符
     */
    public static String clearSpecialChar(String query) {
        if( query == null ) {
            query = "";
        }
        Matcher m = pattern.matcher(query);
        return m.replaceAll("").trim();
    }

    /**
     * 通过Header中的X-Forwarded-For字段获取用户真实IP
     */
    private static String getRemoteIp() {
        if (RpcContext.getContext().getRequest() != null
                && RpcContext.getContext().getRequest() instanceof HttpServletRequest) {
            
            HttpServletRequest request = (HttpServletRequest) RpcContext.getContext() .getRequest();
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if( xForwardedFor  == null ){
                return "";
            } 

            List<String> ips = Splitter.on(',').splitToList(xForwardedFor);
            if( ips.size() > 0 ) {
                return ips.get(0).trim();
            } else {
                return "";
            }
        }
        return "";
    }
}
