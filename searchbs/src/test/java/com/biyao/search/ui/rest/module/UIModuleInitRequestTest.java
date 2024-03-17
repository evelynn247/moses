package com.biyao.search.ui.rest.module;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.biyao.search.ui.model.RequestBlock;
import com.biyao.search.ui.rest.impl.module.ModInitRequest;

public class UIModuleInitRequestTest {

	@Before
	public void init() throws Exception {
	}
	
	@After
	public void free(){
	}
	
	@Test
	public void testCreateSid(){
	    String uuid = "jdkafjkAKKLlddad"; // md5: 38303ccfa0e948db0d4239ac155934de
	    String query = "女装";  // md5: e875124048961d3392194b1325b1a16d
	    String sid = ModInitRequest.createSid(uuid, query);
	    
	    Assert.assertEquals(sid.length(), 32);
	    String [] items = sid.split("-");
	    Assert.assertEquals(items[1], "e875");
	    Assert.assertEquals(items[2], "38303ccfa0e94");
	}
	
	@Test
    public void testSecurityCheck(){
        String uuid = "jdkafjkAKKLlddad"; // md5: 38303ccfa0e948db0d4239ac155934de
        String query = "女装";  // md5: e875124048961d3392194b1325b1a16d
        String sid = ModInitRequest.createSid(uuid, query);
        
        RequestBlock request = new RequestBlock();
        
        // 正常情况
        request.setUuid(uuid);
        request.setQuery(query);
        request.setSid(sid);
        Assert.assertTrue( ModInitRequest.securityCheck(request) );
        
        // query有问题
        request.setQuery("男装");
        Assert.assertTrue( ! ModInitRequest.securityCheck(request) );
        
        // uuid有问题
        request.setQuery(query);
        request.setUuid("xxx");
        Assert.assertTrue( ! ModInitRequest.securityCheck(request) );
    }
	
	@Test
    public void testParameterCheck(){
	    RequestBlock request = new RequestBlock();
        
        // 正常情况
        request.setUuid("xxx");
        request.setQuery("想吃金钱豹");
        request.setSid( ModInitRequest.createSid(request.getUuid(), request.getQuery()) );
        request.setPlatform("mweb");
        Assert.assertTrue( ModInitRequest.parameterCheck(request) );
        
        request.setSid(null);
        Assert.assertTrue( ModInitRequest.parameterCheck(request) );
        
        // sid错误
        request.setSid("xxxxx");
        Assert.assertTrue( ! ModInitRequest.parameterCheck(request) );
    }
	
	@Test
    public void testClearSpecialChar(){
	    Assert.assertEquals(ModInitRequest.clearSpecialChar("你好中国*&中国_+QP)中国"), "你好中国中国_QP中国" );
	    Assert.assertEquals(ModInitRequest.clearSpecialChar("你好 中国"), "你好 中国" );
	    Assert.assertEquals(ModInitRequest.clearSpecialChar("test & word "), "test  word" );
	}
}