package com.biyao.moses.service.imp;

import java.io.UnsupportedEncodingException;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.biyao.moses.util.HttpClientUtil;

public class TestMatchAndRankAnsyService {

	public static void main(String[] args) throws UnsupportedEncodingException {
//
//		String reqURL = "http://localhost:8081/recommend/match";
//		Map<String, String> head = null;
//		String requestStr = "{\"block\":{\"bid\":\"moses:bid_1184\",\"block\":[{\"data\":[{\"height\":\"20\",\"mainTitleColor\":\"#CCB17A\",\"priceColor\":\"#BF9E6B\",\"routerType\":7,\"subtitleColor\":\"#4A4A4A\"}],\"dataSourceType\":\"10300128\",\"dynamic\":true,\"templateName\":\"doubleRowList\",\"templateType\":14,\"tid\":\"moses:tid_3962\"}],\"dynamic\":true,\"exp\":false,\"feed\":true},\"categoryIds\":[],\"dataSourceType\":\"\",\"feedPageNum\":1,\"pageId\":\"moses:pid_129\",\"pvid\":\"7181011-1011141323665-55a481590\",\"scmIds\":[],\"sessionId\":\"ef622bb114d8029e.1569381121391\",\"siteId\":\"7\",\"uuId\":\"7181011141323f228e8fc0efd4e80000000\"}";
//		int timeout = 2000;
// 
//		 
//		
//		long startTime = System.currentTimeMillis();
//		for (int i = 0; i <1; i++) {
//			try {
//				String content = HttpClientUtil.sendPostJSONGZIP(reqURL, head, requestStr, timeout);
//				JSONObject obj = JSON.parseObject(content);
//				System.out.println(obj);
//				System.out.println(obj.size());
//				System.out.println("un gzip.length= " + content.length());
//			} catch (Exception e) {
//				// TODO Auto-generated catch block e.printStackTrace();
//			}
//		}
//		System.out.println(System.currentTimeMillis() - startTime);
//		
//		long startTime = System.currentTimeMillis();
//		for (int i = 0; i <1; i++) {
//			try {
//				String content = com.biyao.moses.util.HttpClientUtil.sendPostJSONGZIP(reqURL, head, requestStr, timeout);
//				System.out.println(content);
//				System.out.println("gzip.length= " + content.length());
//			} catch (Exception e) {
//				// TODO Auto-generated catch block e.printStackTrace();
//			}
//		}
//		System.out.println(System.currentTimeMillis() - startTime);

		String reqURL = "http://localhost:8082/recommend/rank";
		Map<String, String> head = null;
		String requestStr = "{\"matchData\":{\"moses:10300128_match.zwx.rexiaohaoping.20190408\":[{\"height\":\"20\",\"id\":\"1300476682\"},{\"height\":\"20\",\"id\":\"1300475878\"},{\"height\":\"20\",\"id\":\"1300476529\"},{\"height\":\"20\",\"id\":\"1300435047\"},{\"height\":\"20\",\"id\":\"1300475143\"},{\"height\":\"20\",\"id\":\"1300435045\"},{\"height\":\"20\",\"id\":\"1300435039\"},{\"height\":\"20\",\"id\":\"1301425011\"},{\"height\":\"20\",\"id\":\"1300476185\"},{\"height\":\"20\",\"id\":\"1300475183\"},{\"height\":\"20\",\"id\":\"1300475992\"},{\"height\":\"20\",\"id\":\"1300476743\"},{\"height\":\"20\",\"id\":\"1300475230\"},{\"height\":\"20\",\"id\":\"1301565016\"},{\"height\":\"20\",\"id\":\"1300476420\"},{\"height\":\"20\",\"id\":\"1301565002\"},{\"height\":\"20\",\"id\":\"1300476762\"},{\"height\":\"20\",\"id\":\"1300435048\"},{\"height\":\"20\",\"id\":\"1300476356\"},{\"height\":\"20\",\"id\":\"1300475773\"},{\"height\":\"20\",\"id\":\"1301415001\"},{\"height\":\"20\",\"id\":\"1300435057\"},{\"height\":\"20\",\"id\":\"1300476524\"},{\"height\":\"20\",\"id\":\"1300476766\"},{\"height\":\"20\",\"id\":\"1301555004\"},{\"height\":\"20\",\"id\":\"1300476248\"},{\"height\":\"20\",\"id\":\"1301425009\"},{\"height\":\"20\",\"id\":\"1301275006\"},{\"height\":\"20\",\"id\":\"1300475962\"},{\"height\":\"20\",\"id\":\"1300475935\"},{\"height\":\"20\",\"id\":\"1300476646\"},{\"height\":\"20\",\"id\":\"1300476733\"},{\"height\":\"20\",\"id\":\"1300475795\"},{\"height\":\"20\",\"id\":\"1300475838\"},{\"height\":\"20\",\"id\":\"1300475755\"},{\"height\":\"20\",\"id\":\"1301425004\"},{\"height\":\"20\",\"id\":\"1300476051\"},{\"height\":\"20\",\"id\":\"1300475835\"},{\"height\":\"20\",\"id\":\"1300476696\"},{\"height\":\"20\",\"id\":\"1300476424\"},{\"height\":\"20\",\"id\":\"1300476595\"},{\"height\":\"20\",\"id\":\"1300476317\"},{\"height\":\"20\",\"id\":\"1301565018\"},{\"height\":\"20\",\"id\":\"1300435042\"},{\"height\":\"20\",\"id\":\"1301565005\"},{\"height\":\"20\",\"id\":\"1301425007\"},{\"height\":\"20\",\"id\":\"1300476418\"},{\"height\":\"20\",\"id\":\"1301285001\"},{\"height\":\"20\",\"id\":\"1300476260\"},{\"height\":\"20\",\"id\":\"1301605003\"},{\"height\":\"20\",\"id\":\"1300476224\"},{\"height\":\"20\",\"id\":\"1300476691\"},{\"height\":\"20\",\"id\":\"1300435056\"},{\"height\":\"20\",\"id\":\"1300476048\"},{\"height\":\"20\",\"id\":\"1300475909\"},{\"height\":\"20\",\"id\":\"1301565008\"},{\"height\":\"20\",\"id\":\"1300435054\"},{\"height\":\"20\",\"id\":\"1300475893\"},{\"height\":\"20\",\"id\":\"1300476694\"},{\"height\":\"20\",\"id\":\"1300475876\"},{\"height\":\"20\",\"id\":\"1301565010\"},{\"height\":\"20\",\"id\":\"1300435038\"},{\"height\":\"20\",\"id\":\"1301555009\"},{\"height\":\"20\",\"id\":\"1300475853\"},{\"height\":\"20\",\"id\":\"1300475520\"},{\"height\":\"20\",\"id\":\"1301565014\"},{\"height\":\"20\",\"id\":\"1301415013\"},{\"height\":\"20\",\"id\":\"1300475201\"},{\"height\":\"20\",\"id\":\"1301565009\"},{\"height\":\"20\",\"id\":\"1300476240\"},{\"height\":\"20\",\"id\":\"1301275003\"},{\"height\":\"20\",\"id\":\"1301565000\"},{\"height\":\"20\",\"id\":\"1301425014\"},{\"height\":\"20\",\"id\":\"1300476677\"},{\"height\":\"20\",\"id\":\"1300476407\"},{\"height\":\"20\",\"id\":\"1300476297\"},{\"height\":\"20\",\"id\":\"1300476027\"},{\"height\":\"20\",\"id\":\"1301425015\"},{\"height\":\"20\",\"id\":\"1300435051\"},{\"height\":\"20\",\"id\":\"1300475911\"},{\"height\":\"20\",\"id\":\"1301425005\"},{\"height\":\"20\",\"id\":\"1300475234\"},{\"height\":\"20\",\"id\":\"1300435059\"},{\"height\":\"20\",\"id\":\"1300476239\"},{\"height\":\"20\",\"id\":\"1301275002\"},{\"height\":\"20\",\"id\":\"1300476139\"},{\"height\":\"20\",\"id\":\"1300476213\"},{\"height\":\"20\",\"id\":\"1300475156\"},{\"height\":\"20\",\"id\":\"1300476697\"},{\"height\":\"20\",\"id\":\"1301565001\"},{\"height\":\"20\",\"id\":\"1300435050\"},{\"height\":\"20\",\"id\":\"1300476428\"},{\"height\":\"20\",\"id\":\"1301415002\"},{\"height\":\"20\",\"id\":\"1300476654\"},{\"height\":\"20\",\"id\":\"1301605001\"},{\"height\":\"20\",\"id\":\"1301415003\"},{\"height\":\"20\",\"id\":\"1300476364\"},{\"height\":\"20\",\"id\":\"1301565017\"},{\"height\":\"20\",\"id\":\"1301425008\"},{\"height\":\"20\",\"id\":\"1300476562\"},{\"height\":\"20\",\"id\":\"1300476411\"},{\"height\":\"20\",\"id\":\"1300476765\"},{\"height\":\"20\",\"id\":\"1300475699\"},{\"height\":\"20\",\"id\":\"1300475944\"},{\"height\":\"20\",\"id\":\"1301565004\"},{\"height\":\"20\",\"id\":\"1300435032\"},{\"height\":\"20\",\"id\":\"1300476676\"},{\"height\":\"20\",\"id\":\"1301555001\"},{\"height\":\"20\",\"id\":\"1300476637\"},{\"height\":\"20\",\"id\":\"1300475516\"},{\"height\":\"20\",\"id\":\"1300475919\",\"score\":1.0},{\"height\":\"20\",\"id\":\"1300476681\",\"score\":1.0},{\"height\":\"20\",\"id\":\"1300435044\",\"score\":1.0},{\"height\":\"20\",\"id\":\"1300475851\",\"score\":1.0},{\"height\":\"20\",\"id\":\"1301415015\",\"score\":1.0},{\"height\":\"20\",\"id\":\"1300476626\",\"score\":1.0},{\"height\":\"20\",\"id\":\"1301425006\",\"score\":1.0},{\"height\":\"20\",\"id\":\"1300476647\",\"score\":1.0},{\"height\":\"20\",\"id\":\"1300435052\",\"score\":1.0},{\"height\":\"20\",\"id\":\"1301605000\",\"score\":1.0},{\"height\":\"20\",\"id\":\"1300476198\",\"score\":1.0},{\"height\":\"20\",\"id\":\"1301555006\",\"score\":1.0},{\"height\":\"20\",\"id\":\"1300475091\",\"score\":1.0},{\"height\":\"20\",\"id\":\"1300476649\",\"score\":1.0},{\"height\":\"20\",\"id\":\"1300475098\",\"score\":1.0},{\"height\":\"20\",\"id\":\"1300475232\",\"score\":1.0},{\"height\":\"20\",\"id\":\"1300435053\",\"score\":1.0},{\"height\":\"20\",\"id\":\"1300476258\",\"score\":1.0},{\"height\":\"20\",\"id\":\"1300476451\",\"score\":1.0},{\"height\":\"20\",\"id\":\"1300476690\",\"score\":1.0},{\"height\":\"20\",\"id\":\"1300476442\",\"score\":1.0},{\"height\":\"20\",\"id\":\"1301555000\",\"score\":1.0},{\"height\":\"20\",\"id\":\"1300475263\",\"score\":1.0},{\"height\":\"20\",\"id\":\"1300475184\",\"score\":1.0},{\"height\":\"20\",\"id\":\"1300475904\",\"score\":1.0},{\"height\":\"20\",\"id\":\"1301415010\",\"score\":1.0},{\"height\":\"20\",\"id\":\"1300475883\",\"score\":1.0},{\"height\":\"20\",\"id\":\"1300476758\",\"score\":1.0},{\"height\":\"20\",\"id\":\"1300475890\",\"score\":1.0},{\"height\":\"20\",\"id\":\"1301555010\",\"score\":1.0},{\"height\":\"20\",\"id\":\"1300476541\",\"score\":1.0},{\"height\":\"20\",\"id\":\"1300476760\",\"score\":1.0},{\"height\":\"20\",\"id\":\"1300476744\",\"score\":1.0},{\"height\":\"20\",\"id\":\"1300476757\",\"score\":1.0},{\"height\":\"20\",\"id\":\"1301415007\",\"score\":1.0},{\"height\":\"20\",\"id\":\"1301555007\",\"score\":1.0},{\"height\":\"20\",\"id\":\"1300475350\",\"score\":1.0},{\"height\":\"20\",\"id\":\"1300476710\",\"score\":1.0},{\"height\":\"20\",\"id\":\"1301415014\",\"score\":1.0},{\"height\":\"20\",\"id\":\"1300476704\",\"score\":1.0},{\"height\":\"20\",\"id\":\"1300476033\",\"score\":1.0},{\"height\":\"20\",\"id\":\"1301425013\",\"score\":1.0},{\"height\":\"20\",\"id\":\"1300475916\",\"score\":1.0},{\"height\":\"20\",\"id\":\"1300476238\",\"score\":1.0},{\"height\":\"20\",\"id\":\"1300476698\",\"score\":1.0},{\"height\":\"20\",\"id\":\"1301415006\",\"score\":1.0},{\"height\":\"20\",\"id\":\"1300475329\",\"score\":1.0},{\"height\":\"20\",\"id\":\"1300476036\",\"score\":1.0},{\"height\":\"20\",\"id\":\"1301275004\",\"score\":1.0},{\"height\":\"20\",\"id\":\"1300476414\",\"score\":1.0},{\"height\":\"20\",\"id\":\"1301425019\",\"score\":1.0},{\"height\":\"20\",\"id\":\"1300476750\",\"score\":1.0},{\"height\":\"20\",\"id\":\"1300475137\",\"score\":1.0},{\"height\":\"20\",\"id\":\"1301555005\",\"score\":1.0},{\"height\":\"20\",\"id\":\"1300475087\",\"score\":1.0},{\"height\":\"20\",\"id\":\"1300476233\",\"score\":1.0},{\"height\":\"20\",\"id\":\"1301425010\",\"score\":1.0},{\"height\":\"20\",\"id\":\"1300475521\",\"score\":1.0}]},\"sessionId\":\"c177402826b98986.1569502684874\",\"uid\":\"1\",\"categoryIds\":\"{217,156}\",\"uuid\":\"71906102353124b20730f6fde45930000000\"}";
		int timeout = 2000;
		

		long startTime = System.currentTimeMillis();
		for (int i = 0; i <1; i++) {
			try {
				String content = HttpClientUtil.sendPostJSONGZIP(reqURL, head, requestStr, timeout);
				System.out.println(content);
				System.out.println("gzip.length= " + content.length());
			} catch (Exception e) {
				// TODO Auto-generated catch block e.printStackTrace();
			}
		}
		
		
	}
	
}
