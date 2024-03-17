package com.biyao.search.ui.util;

import java.util.UUID;

import com.google.common.base.Charsets;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

public class IdCalculateUtil {
    /**
     * 生成新的SID
     * SID的生成规则为：${timestamp_ms}-${query_md5_start4}-${uuid_md5_13}，其中：
     * 1. ${timestamp_ms}是当前时间戳（到毫秒），共13位；
     * 2. ${query_md5_start4}是查询词MD5 值（32位小写）的前4个字符，共4位；
     * 3. ${uuid_md5_13}是UUID MD5值（32位小写）的前13个字符。
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
     * ${timestamp_ms}-${query_md5_start4}-${uuid_md5_13}
     */
    public static String createBlockId() {
        HashFunction hf = Hashing.md5();
        String uuidMd5 = hf.newHasher().putString(UUID.randomUUID().toString(), Charsets.UTF_8).hash().toString();

        return uuidMd5.substring(9, 25);
    }

    /**
     * 服务端唯一标识(30位)生成规则：
     * sid=md5(did,16) + "." + timestamp(13)
     * 首页添加追踪参数：aidMap.put('home', sid)
     * 推荐中间页添加追踪参数：aidMap.put('rcd', sid)
     * 搜索中间页添加追踪参数：aidMap.put('search', sid)
     * 追踪参数添加到stp中：stpMap.put('aid', JSON.toString(aidMap))
     *
     * @param did
     * @return
     */
    public static String createAid(String did) {
        HashFunction hf = Hashing.md5();
        String didMd5 = hf.newHasher().putString(did, Charsets.UTF_8).hash().toString();
        return didMd5.substring(0, 16) + "." + System.currentTimeMillis();
    } 
}
