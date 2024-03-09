//package com.biyao.moses.service.imp;
//
//import com.biyao.moses.common.enums.MosesConfTypeEnum;
//import com.biyao.moses.common.enums.RedisKeyTypeEnum;
//import com.biyao.moses.exp.cache.ConfDataCache;
//import com.biyao.moses.exp.util.MosesConfUtil;
//import org.databene.contiperf.PerfTest;
//import org.databene.contiperf.Required;
//import org.databene.contiperf.junit.ContiPerfRule;
//import org.junit.Rule;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.context.junit4.SpringRunner;
//
///**
// * @ClassName TestMosesConfUtil
// * @Description TODO
// * @Author admin
// * @Date 2019/9/22 23:53
// * @Version 1.0
// **/
//@RunWith(SpringRunner.class)
//@SpringBootTest
//public class TestMosesConfUtil {
//    @Autowired
//    MosesConfUtil mosesConfUtil;
//    @Autowired
//    ConfDataCache confDataCache;
//
//    //引入 ContiPerf 进行性能测试
//    @Rule
//    public ContiPerfRule contiPerfRule = new ContiPerfRule();
//
//    //test1-3测试，访问redis异常场景
//    @Test
//    public void test1() throws Exception{
//        //缓存中没有，数据库中没有，则邮件告警
//        String key = "xjk_string_test";
//        //启动到这后再往数据库中插入这个key
//        String value = mosesConfUtil.dealOnRedisValueNullOrException(MosesConfTypeEnum.ExpConf, key, null, RedisKeyTypeEnum.STRING.getId(),Boolean.TRUE);
//        System.out.println(value + " cache:" + confDataCache.getExpConfMap().get(key));
//        //第二次进入不满5分钟，则不再发邮件(测试时间将邮件间隔改为1分钟)
//        value = mosesConfUtil.dealOnRedisValueNullOrException(MosesConfTypeEnum.ExpConf, key, null, RedisKeyTypeEnum.STRING.getId(),Boolean.TRUE);
//        System.out.println(value + " cache:" + confDataCache.getExpConfMap().get(key));
//        //第三次进入超过5分钟，则再发邮件
//        Thread.sleep(65000);
//        value = mosesConfUtil.dealOnRedisValueNullOrException(MosesConfTypeEnum.ExpConf, key, null, RedisKeyTypeEnum.STRING.getId(),Boolean.TRUE);
//        System.out.println(value + " cache:" + confDataCache.getExpConfMap().get(key));
//        Thread.sleep(30000);
//    }
//
//    @Test
//    public void test2() throws Exception{
//        //缓存中有，则从缓存中查找并返回
//        String key = "xjk_string_test";
//        confDataCache.getExpConfMap().put(key, key);
//        String value = mosesConfUtil.dealOnRedisValueNullOrException(MosesConfTypeEnum.ExpConf, key, null, RedisKeyTypeEnum.STRING.getId(),Boolean.TRUE);
//        System.out.println(value + " cache:" + confDataCache.getExpConfMap().get(key));
//        //第二次进入不满5分钟，则不再发邮件(测试时间将邮件间隔改为1分钟)
//        value = mosesConfUtil.dealOnRedisValueNullOrException(MosesConfTypeEnum.ExpConf, key, null, RedisKeyTypeEnum.STRING.getId(),Boolean.TRUE);
//        System.out.println(value + " cache:" + confDataCache.getExpConfMap().get(key));
//        //第三次进入超过5分钟，则再发邮件
//        Thread.sleep(65000);
//        value = mosesConfUtil.dealOnRedisValueNullOrException(MosesConfTypeEnum.ExpConf, key, null, RedisKeyTypeEnum.STRING.getId(),Boolean.TRUE);
//        System.out.println(value + " cache:" + confDataCache.getExpConfMap().get(key));
//        Thread.sleep(20000);
//    }
//
//    @Test
//    public void test3() throws Exception{
//        //缓存中没有，数据库中有，则告警
//        String key = "xjk_string_test";
//        String value = mosesConfUtil.dealOnRedisValueNullOrException(MosesConfTypeEnum.ExpConf, key, null, RedisKeyTypeEnum.STRING.getId(),Boolean.TRUE);
//        System.out.println(value + " cache:" + confDataCache.getExpConfMap().get(key));
//        //第二次进入不满5分钟，则不再发邮件
//        Thread.sleep(1000);
//        value = mosesConfUtil.dealOnRedisValueNullOrException(MosesConfTypeEnum.ExpConf, key, null, RedisKeyTypeEnum.STRING.getId(),Boolean.TRUE);
//        System.out.println(value + " cache:" + confDataCache.getExpConfMap().get(key));
//        //第二次进入满5分钟，则再发邮件
//        Thread.sleep(65000);
//        value = mosesConfUtil.dealOnRedisValueNullOrException(MosesConfTypeEnum.ExpConf, key, null, RedisKeyTypeEnum.STRING.getId(),Boolean.TRUE);
//        System.out.println(value + " cache:" + confDataCache.getExpConfMap().get(key));
//        Thread.sleep(20000);
//    }
//
//
//    //测试redis中的value为空的场景
//    @Test
//    public void test4() throws Exception{
//        //数据库中没有，缓存中也没有，key类型为String
//        String key = "xjk_string_test";
//        String value = mosesConfUtil.dealOnRedisValueNullOrException(MosesConfTypeEnum.ExpConf, key, null, RedisKeyTypeEnum.STRING.getId(),Boolean.FALSE);
//        System.out.println(value + " cache:" + confDataCache.getExpConfMap().get(key));
//        //第二次进入不满5分钟，则不再发邮件
//        Thread.sleep(1000);
//        value = mosesConfUtil.dealOnRedisValueNullOrException(MosesConfTypeEnum.ExpConf, key, null, RedisKeyTypeEnum.STRING.getId(),Boolean.FALSE);
//        System.out.println(value + " cache:" + confDataCache.getExpConfMap().get(key));
//        //第二次进入满5分钟，则再发邮件
//        Thread.sleep(100000);
//        value = mosesConfUtil.dealOnRedisValueNullOrException(MosesConfTypeEnum.ExpConf, key, null, RedisKeyTypeEnum.STRING.getId(),Boolean.FALSE);
//        System.out.println(value + " cache:" + confDataCache.getExpConfMap().get(key));
//        Thread.sleep(20000);
//    }
//
//    @Test
//    public void test5() throws Exception{
//        //数据库中没有，缓存中有，key类型为String
//        String key = "xjk_string_test";
//        confDataCache.getExpConfMap().put(key, key);
//        String value = mosesConfUtil.dealOnRedisValueNullOrException(MosesConfTypeEnum.ExpConf, key, null, RedisKeyTypeEnum.STRING.getId(),Boolean.FALSE);
//        System.out.println(value + " cache:" + confDataCache.getExpConfMap().get(key));
//        //第二次进入不满5分钟，则不再发邮件
//        Thread.sleep(1000);
//        value = mosesConfUtil.dealOnRedisValueNullOrException(MosesConfTypeEnum.ExpConf, key, null, RedisKeyTypeEnum.STRING.getId(),Boolean.FALSE);
//        System.out.println(value + " cache:" + confDataCache.getExpConfMap().get(key));
//        //第二次进入满5分钟，则再发邮件
//        Thread.sleep(20000);
//        value = mosesConfUtil.dealOnRedisValueNullOrException(MosesConfTypeEnum.ExpConf, key, null, RedisKeyTypeEnum.STRING.getId(),Boolean.FALSE);
//        System.out.println(value + " cache:" + confDataCache.getExpConfMap().get(key));
//        Thread.sleep(20000);
//    }
//
//    @Test
//    public void test6() throws Exception{
//        //测试数据库中有，key类型为Hash
//        String key = "xjk_hash_test";
//        String value = mosesConfUtil.dealOnRedisValueNullOrException(MosesConfTypeEnum.ExpConf, key, null, RedisKeyTypeEnum.HASH.getId(),Boolean.FALSE);
//        System.out.println(value + " cache:" + confDataCache.getExpConfMap().get(key));
//        //第二次进入也发邮件
//        Thread.sleep(1000);
//        value = mosesConfUtil.dealOnRedisValueNullOrException(MosesConfTypeEnum.ExpConf, key, null, RedisKeyTypeEnum.HASH.getId(),Boolean.FALSE);
//        System.out.println(value + " cache:" + confDataCache.getExpConfMap().get(key));
//        //第三次也发邮件
//        Thread.sleep(65000);
//        value = mosesConfUtil.dealOnRedisValueNullOrException(MosesConfTypeEnum.ExpConf, key, null, RedisKeyTypeEnum.HASH.getId(),Boolean.FALSE);
//        System.out.println(value + " cache:" + confDataCache.getExpConfMap().get(key));
//        Thread.sleep(20000);
//    }
//
//    //并发测试
//    @Test
//    @PerfTest(invocations = 500, threads = 50)
//    @Required(percentile99 = 1500)
//    public void test7() throws Exception{
//        //缓存中有，则从缓存中查找并返回
//        String key = "xjk_string_test";
//        confDataCache.getExpConfMap().put(key, key);
//        String value = mosesConfUtil.dealOnRedisValueNullOrException(MosesConfTypeEnum.ExpConf, key, null, RedisKeyTypeEnum.STRING.getId(),Boolean.TRUE);
////        System.out.println(value + " cache:" + confDataCache.getExpConfMap().get(key));
////        //第二次进入不满5分钟，则不再发邮件(测试时间将邮件间隔改为1分钟)
////        value = mosesConfUtil.dealOnRedisValueNullOrException(MosesConfTypeEnum.ExpConf, key, null, RedisKeyTypeEnum.STRING.getId(),Boolean.TRUE);
////        System.out.println(value + " cache:" + confDataCache.getExpConfMap().get(key));
////        //第三次进入超过5分钟，则再发邮件
////        Thread.sleep(65000);
////        value = mosesConfUtil.dealOnRedisValueNullOrException(MosesConfTypeEnum.ExpConf, key, null, RedisKeyTypeEnum.STRING.getId(),Boolean.TRUE);
////        System.out.println(value + " cache:" + confDataCache.getExpConfMap().get(key));
////        Thread.sleep(20000);
//    }
//
//    @Test
//    @PerfTest(invocations = 500, threads = 50)
//    public void test8() throws Exception{
//        //测试数据库中有，key类型为Hash
//        String key = "xjk_hash_test";
//        String value = mosesConfUtil.dealOnRedisValueNullOrException(MosesConfTypeEnum.ExpConf, key, null, RedisKeyTypeEnum.HASH.getId(),Boolean.FALSE);
//        System.out.println(value + " cache:" + confDataCache.getExpConfMap().get(key));
//        //第二次进入也发邮件
////        Thread.sleep(1000);
////        value = mosesConfUtil.dealOnRedisValueNullOrException(MosesConfTypeEnum.ExpConf, key, null, RedisKeyTypeEnum.HASH.getId(),Boolean.FALSE);
////        System.out.println(value + " cache:" + confDataCache.getExpConfMap().get(key));
////        //第三次也发邮件
////        //Thread.sleep(65000);
////        value = mosesConfUtil.dealOnRedisValueNullOrException(MosesConfTypeEnum.ExpConf, key, null, RedisKeyTypeEnum.HASH.getId(),Boolean.FALSE);
////        System.out.println(value + " cache:" + confDataCache.getExpConfMap().get(key));
////        //Thread.sleep(20000);
//    }
//}
//
