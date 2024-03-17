package com.biyao.search.ui.service;

import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.CollectionUtils;

import com.biyao.search.common.enums.PlatformEnum;
import com.biyao.search.ui.model.VModel;
import com.biyao.search.ui.remote.request.UISearchRequest;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/spring/search-ui.xml")
public class VModelHandlerTest {

	
	@Autowired
	private VModelHandler vModelHandler;
	
	@Test
	public void testVmodel() {
		UISearchRequest request = new UISearchRequest();
		request.setUid(100);
		request.setOriginalQuery("必要");
		request.setPvid("12345678946512315646851324564");
		request.setPlatform(PlatformEnum.IOS);
		
		
		 List<VModel> vList = vModelHandler.getVmodelList(request);
		 assertTrue(! CollectionUtils.isEmpty(vList));
	} 
}
