package com.biyao.moses.util;

import com.biyao.moses.params.MatchSourceData;
import com.biyao.moses.params.ProductScoreInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.regex.Pattern;

/**
* @Description 字符串工具类
* @date 2019年6月25日下午2:05:24
* @version V1.0 
* @author 邹立强 (zouliqiang@idstaff.com)
* <p>Copyright (c) Department of Research and Development/Beijing.</p>
 */
@Slf4j
public class StringUtil {

    public static final String EMPTY_STRING = "";

	public static final Double DEFAULT_SCORE = 0.0;


	/**
	 * 13332,1334,183   类型的数据 转成list
	 * @param str
	 * @return
	 */
	public static  List<Long> strConverToList(String str){
		List<Long> result = new ArrayList<>();
		if(isBlank(str)){
			return result;
		}
		String[] dataStr = str.split(",");
		for (int i = 0; i < dataStr.length; i++) {
			try {
				result.add(Long.valueOf(dataStr[i]));
			}catch (Exception e){
				log.error("[严重异常]字符串转long类型异常，str：{}",dataStr[i]);
			}
		}
		return result;
	}


	public static  Set<String> strConverToSet(String str){
		Set<String> result = new HashSet<>();
		if(isBlank(str)){
			return result;
		}
		result.addAll(Arrays.asList(str.split(",")));
		return result;
	}


    /**
    * @Description 字符串工具类 
    * @param str
    * @return boolean 
    * @version V1.0
    * @auth 邹立强 (zouliqiang@idstaff.com)
     */
    public static boolean isBlank(String str) {
        if ("".equals(str) || str == null) {
            return true;
        } else {
            return false;
        }
    }
    /**
     * @Description连接字符串
     * @param objs
     * @return
     * @return String
     * @version V1.0
     * @auth 邹立强 (zouliqiang@idstaff.com)
     */
    public static String concat(Object... objs) {
        if (null == objs || objs.length < 1)
            return EMPTY_STRING;
        StringBuffer sb = new StringBuffer();
        for (Object obj : objs) {
            sb.append(null == obj ? EMPTY_STRING : obj.toString());
        }
        return sb.toString();
    }
    
    /**
	 * @Description 字符串转化Map
	 * @param logStr
	 * @return HashMap<String,String>
	 * @version V1.0
	 * @auth 邹立强 (zouliqiang@idstaff.com)
	 */
    public static HashMap<String, String> getMapByStr(String logStr) {
		HashMap<String, String> result = new HashMap<String, String>();
		if(isBlank(logStr)) {
			return result;
		}
		try {
			String[] qs = logStr.split("\t");
			for (String q : qs) {
				int indexOf = q.indexOf("=");
				if (indexOf < 0) {
					continue;
				}
				String key = q.substring(0, indexOf);
				String value = q.substring(indexOf + 1);
				result.put(key, value);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * products格式为：pid:score,pid:score,...,pid:score; pid存放时是有序的，前面的pid的分值比后面的高。
	 * 若pid有多个相同时，则该pid的分值为第一次出现时的分值。
	 * @param productStr
	 * @param caller  调用者信息
	 * @param pidNumMaxLimit  解析pid数量上限
	 *                        若解析的pid数量超过该限制，则只取该限制数量的pid，后续的不再继续解析。
	 *                        如果该值为-1，则认为没有上限
	 * @return
	 */
	public static List<ProductScoreInfo> parseProductStr(String productStr, int pidNumMaxLimit, String caller){
		List<ProductScoreInfo> result = new ArrayList<>();
		if(pidNumMaxLimit < -1){
			log.error("[严重异常]入参检验失败，上限值必须大于等于-1. pidNumMaxLimit {}, caller {}", pidNumMaxLimit, caller);
			return result;
		}

		if(StringUtils.isBlank(productStr)){
			return result;
		}

		boolean isCheckErr = false;
		Set<Long> pidSet = new HashSet<>();
		String[] pidScores = productStr.trim().split(",");
		for(String pidScore : pidScores){
			if(StringUtils.isBlank(pidScore)){
				continue;
			}
			try{
				String[] pidAndScore = pidScore.trim().split(":");
				String productIdStr;
				String scoreStr = null;
				if(pidAndScore.length == 1){
					productIdStr = pidAndScore[0].trim();
				}else if(pidAndScore.length == 2){
					productIdStr = pidAndScore[0].trim();
					scoreStr = pidAndScore[1].trim();
				}else{
					isCheckErr = true;
					continue;
				}
				Double score;
				if(StringUtils.isBlank(scoreStr)){
					score = DEFAULT_SCORE;
				}else {
					score = Double.valueOf(scoreStr);
				}
				Long productId = Long.valueOf(productIdStr);

				//重复的商品ID不再处理
				if(pidSet.contains(productId)){
					continue;
				}else{
					pidSet.add(productId);
					ProductScoreInfo productScoreInfo = new ProductScoreInfo();
					productScoreInfo.setProductId(productId);
					productScoreInfo.setScore(score);
					//上限为-1时，表示没有上限
					if(pidNumMaxLimit == -1){
						result.add(productScoreInfo);
					}else{
						if(result.size() >= pidNumMaxLimit){
							return result;
						}
						result.add(productScoreInfo);
					}
				}
			}catch(Exception e){
				log.error("[严重异常]解析商品ID信息时发生异常,product {}, caller {}, e {}", pidScore, caller, e);
			}
		}
		if(isCheckErr){
			log.error("[严重异常]解析商品ID信息时发生错误,caller {}", caller);
		}
		return result;
	}

	/**
	 * products格式为：pid:score,pid:score,...,pid:score; pid存放时是有序的，前面的pid的分值比后面的高。
	 * 若pid有多个相同时，则该pid的分值为第一次出现时的分值。
	 * @param idScoreStr
	 * @param bizName  业务名称，用于标识那个业务调用的该方法，如是解析IBCF召回源数据，则传入ibcf
	 * @return
	 */
	public static Map<String, Double> parseIdScoreStr(String idScoreStr, String bizName){
		Map<String,Double> result = new HashMap<>();

		if(StringUtils.isBlank(idScoreStr)){
			return result;
		}

		boolean isCheckErr = false;
		String[] idScoreArray = idScoreStr.trim().split(",");
		for(String idScore : idScoreArray){
			if(StringUtils.isBlank(idScore)){
				continue;
			}
			try{
				String[] idAndScore = idScore.trim().split(":");
				String productIdStr;
				String scoreStr = null;
				if(idAndScore.length == 1){
					productIdStr = idAndScore[0].trim();
					isCheckErr = true;
				}else if(idAndScore.length == 2){
					productIdStr = idAndScore[0].trim();
					scoreStr = idAndScore[1].trim();
				}else{
					isCheckErr = true;
					continue;
				}
				Double score;
				if(StringUtils.isBlank(scoreStr)){
					score = DEFAULT_SCORE;
				}else {
					score = Double.valueOf(scoreStr);
				}
				//重复的商品ID不再处理
				if(!result.containsKey(productIdStr)){
					result.put(productIdStr, score);
				}
			}catch(Exception e){
				isCheckErr = false;
				log.error("[严重异常]解析idScoreStr时发生异常,idScoreStr {}, caller {}, e {}", idScoreStr, bizName, e);
			}
		}
		if(isCheckErr){
			log.error("[严重异常]解析idScoreStr时发生错误,idScoreStr {}, caller {}", idScoreStr, bizName);
		}
		return result;
	}


	/**
	 * products格式为：pid:score:realMatchName,pid:score:realMatchName,...,pid:score:realMatchName; pid存放时是有序的，前面的pid的分值比后面的高。
	 * 若pid有多个相同时，则该pid的分值为第一次出现时的分值。
	 * @param matchSourceDataStr
	 * @param bizName  业务名称，用于标识那个业务调用的该方法，如是解析IBCF召回源数据，则传入ibcf
	 * @return
	 */
	public static Map<String, MatchSourceData> parseMatchSourceDataStr(String matchSourceDataStr, String bizName){
		Map<String, MatchSourceData> result = new HashMap<>();

		if(StringUtils.isBlank(matchSourceDataStr)){
			return result;
		}

		boolean isCheckErr = false;
		String[] matchSourceDataArray = matchSourceDataStr.trim().split(",");
		for(String data : matchSourceDataArray){
			if(StringUtils.isBlank(data)){
				continue;
			}
			try{
				String[] matchSourceArray = data.trim().split(":");
				String idStr;
				String scoreStr = null;
				String realSourceName = "";
				if(matchSourceArray.length == 1){
					idStr = matchSourceArray[0].trim();
					isCheckErr = true;
				}else if(matchSourceArray.length == 2){
					idStr = matchSourceArray[0].trim();
					scoreStr = matchSourceArray[1].trim();
				}else if(matchSourceArray.length == 3){
					idStr = matchSourceArray[0].trim();
					scoreStr = matchSourceArray[1].trim();
					realSourceName = matchSourceArray[2].trim();
				}else{
					isCheckErr = true;
					continue;
				}
				Double score;
				if(StringUtils.isBlank(scoreStr)){
					score = DEFAULT_SCORE;
				}else {
					score = Double.valueOf(scoreStr);
				}

				//重复的商品ID不再处理
				if(!result.containsKey(idStr)){
					MatchSourceData matchSourceData = new MatchSourceData();
					matchSourceData.setId(idStr);
					matchSourceData.setScore(score);
					matchSourceData.setRealSourceName(realSourceName);
					result.put(idStr, matchSourceData);
				}
			}catch(Exception e){
				isCheckErr = true;
			}
		}
		if(isCheckErr){
			log.error("[严重异常]解析matchSourceDataStr时发生错误,matchSourceDataStr {}, caller {}", matchSourceDataStr, bizName);
		}
		return result;
	}


	/**
	 * 判读字符串是否为正整数
	 * @param str
	 * @return
	 */
	public  static  boolean isInteger(String str){

		if(isBlank(str)){
			return false;
		}
		Pattern pattern = Pattern.compile("[0-9]+");
		return pattern.matcher(str).matches();
	}
}
