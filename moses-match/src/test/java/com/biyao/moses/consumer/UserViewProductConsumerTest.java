package com.biyao.moses.consumer;

import com.alibaba.fastjson.JSON;
import com.biyao.moses.util.RedisUtil;
import org.apache.commons.lang.math.IntRange;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.common.message.MessageExt;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:app-ctx.xml")
public class UserViewProductConsumerTest {

//    @Autowired MqProducer p;
//
//    @Resource UserViewProductConsumer consumer;
//
//    @Autowired
//    private RedisUtil redisUtil;
//
//    String uuid1 = "71809021315296509c34ec4e98e1a0000000";
//    String uuid2 = "91809021315296509c34ec4e98e1axxxxxxx";
//    String uuid3 = "21809021315296509c34ec4e98e1axxxxxxx";
//    //String suid = "1301615048010000001";
//
//    @Before
//    public void setUp(){
//        redisUtil.del(consumer.KEY_PREFIEX + uuid1);
//        redisUtil.del(consumer.KEY_PREFIEX + uuid2);
//        redisUtil.del(consumer.KEY_PREFIEX + uuid3);
//    }
//
//    @Test
//    public void testInit(){
//        SendResult sr = p.sendMessageByTag("hello", "ProductPublish");
//        System.out.println(sr);
//        assertEquals(SendStatus.SEND_OK, sr.getSendStatus());
//        //System.out.println(consumer);
//    }
//
//
//
//    private String getMsg(String uuid, String pid, String pf){
//        String msg = "2019-05-23 15:05:39 172.16.16.155\tlt=raw_pdetail\tlv=1.0\tpvid=\t" +
//                "uu=$UUID$\tu=4981480\tpf=$PF$\tav=\td=\t" +
//                "os=\tip=172.16.20.47\tsuid=1301615048010000001\tpid=$PID$\tsid=130161\t" +
//                "uprice=109.00\tdur=7\ttt=\uDBC6\uDF6C\uDBC3\uDFDC\uDBC3\uDE14\uDBC5\uDF30 \uDBC6\uDF7B\uDBC1\uDE37\uDBC7\uDD20\uDBC3\uDE1A \uDBC3\uDE29\uDBC7\uDD68\uDBC1\uDD81\tpsp=\uDBC1\uDE42\uDBC6\uDF7B\uDBC1\uDE37\uDBC7\uDD33\uDBC4\uDCC5\t" +
//                "\uDBC7\uDD20\uDBC3\uDE1A \uDBC6\uDF6C\uDBC3\uDFDC\uDBC3\uDE14\uDBC5\uDF30 \uDBC1\uDED2\uDBC2\uDF69\uDBC3\uDCAF\uDBC7\uDDDA\uDBC1\uDD81\uDBC7\uDE4B\tutm=";
//        return msg.replace("$UUID$", uuid).replace("$PID$", pid).replace("$PF$", pf);
//    }
//
//
//    @Test
//    public void testHandleMessage1(){
//        consumer.handleMessage(IntStream.range(0, 200).mapToObj(i->new MessageExt(){{
//            setTags(consumer.TAG_API_RAW);
//            setBody(getMsg(uuid1, i + "", "web").getBytes());
//        }}).collect(Collectors.toList()));
//
//        String key = consumer.KEY_PREFIEX + uuid1;
//        List<String> lst = redisUtil.lrange(key, 0, 200);
//        assertEquals(consumer.NUMBER_RECORDS_KEEPING, lst.size());
//        assertTrue(lst.get(0).startsWith("199:"));
//        //System.out.println(lst);
//    }
//
//    @Test
//    public void testHandleMessage2(){
//        consumer.handleMessage(IntStream.range(0, 200).mapToObj(i->new MessageExt(){{
//            setTags(consumer.TAG_API_RAW);
//            setBody(getMsg(uuid1, i + "", "mweb").getBytes());
//        }}).collect(Collectors.toList()));
//
//        String key = consumer.KEY_PREFIEX + uuid1;
//        List<String> lst = redisUtil.lrange(key, 0, 200);
//        assertTrue(lst.isEmpty());
//    }
//
//    @Test
//    public void testHandleMessage3(){
//        consumer.handleMessage(IntStream.range(0, 100).mapToObj(i->new MessageExt(){{
//            setTags(consumer.TAG_API_RAW);
//            setBody(getMsg(uuid2, i + "", "mweb").getBytes());
//        }}).collect(Collectors.toList()));
//
//        String key = consumer.KEY_PREFIEX + uuid2;
//        List<String> lst = redisUtil.lrange(key, 0, 100);
//        assertTrue(lst.isEmpty());
//    }
//
//    @Test
//    public void testHandleMessage4(){
//        consumer.handleMessage(IntStream.range(0, 200).mapToObj(i->new MessageExt(){{
//            setTags("apiplus.biyao.com:raw_pdetail");
//            setBody(getMsg(uuid2, i + "", "mweb").getBytes());
//        }}).collect(Collectors.toList()));
//
//        String key = consumer.KEY_PREFIEX + uuid2;
//        List<String> lst = redisUtil.lrange(key, 0, 200);
//        assertEquals(consumer.NUMBER_RECORDS_KEEPING, lst.size());
//        assertTrue(lst.get(0).startsWith("199:"));
//        //System.out.println(lst);
//    }
//
//    @Test
//    public void testHandleMessage5(){
//        consumer.handleMessage(IntStream.range(0, 200).mapToObj(i->new MessageExt(){{
//            setTags("appapi.biyao.com:raw_pdetail");
//            setBody(getMsg(uuid2, i + "", "mweb").getBytes());
//        }}).collect(Collectors.toList()));
//
//        String key = consumer.KEY_PREFIEX + uuid2;
//        List<String> lst = redisUtil.lrange(key, 0, 200);
//        assertEquals(consumer.NUMBER_RECORDS_KEEPING, lst.size());
//        assertTrue(lst.get(0).startsWith("199:"));
//        //System.out.println(lst);
//    }
//
//    @Test
//    public void testHandleMessage6(){
//        consumer.handleMessage(IntStream.range(0, 200).mapToObj(i->new MessageExt(){{
//            setTags(consumer.TAG_API_RAW);
//            setBody(getMsg(uuid3, i + "", "mweb").getBytes());
//        }}).collect(Collectors.toList()));
//
//        String key = consumer.KEY_PREFIEX + uuid3;
//        List<String> lst = redisUtil.lrange(key, 0, 200);
//        assertEquals(consumer.NUMBER_RECORDS_KEEPING, lst.size());
//        assertTrue(lst.get(0).startsWith("199:"));
//        //System.out.println(lst);
//    }
//
//    @Test
//    public void testHandleMessage7(){
//        Map<String, String > testMap = new HashMap<>();
//        testMap.put("1", "1");
//        testMap.put("2", "2");
//        testMap.put("3", "3");
//        redisUtil.hmset("test1", testMap);
//        String[] key = {"2", "1", "3"};
//        System.out.println(JSON.toJSONString(redisUtil.hmget("test1", key)));
//        String[] key2 = {"2", "3", "1"};
//        System.out.println(JSON.toJSONString(redisUtil.hmget("test1", key2)));
//        //System.out.println(lst);
//    }
}
