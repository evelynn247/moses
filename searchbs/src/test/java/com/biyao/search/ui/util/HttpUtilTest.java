package com.biyao.search.ui.util;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


public class HttpUtilTest {
    
    @Before
    public void init() throws Exception {
    }
    
    @After
    public void free(){
    }
    
    @Test
    public void testUrlUtf8Encode(){
        Assert.assertEquals(HttpUtil.urlUtf8Encode("你好"), "%e4%bd%a0%e5%a5%bd".toUpperCase());
    }
}
