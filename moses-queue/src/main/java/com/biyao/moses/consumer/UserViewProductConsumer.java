package com.biyao.moses.consumer;

import com.biyao.moses.common.constant.CommonConstants;
import com.biyao.moses.common.constant.RedisKeyConstant;
import com.biyao.moses.consumer.template.MqConsumerTemplate;
import com.biyao.moses.params.ProductInfo;
import com.biyao.moses.schedule.ProductDetailCache;
import com.biyao.moses.util.RedisUtil;
import com.biyao.uc.service.UcServerService;
import com.by.profiler.annotation.BProfiler;
import com.by.profiler.annotation.MonitorType;
import com.uc.domain.bean.User;
import com.uc.domain.constant.UserFieldConstants;
import com.uc.domain.params.UserRequest;
import com.uc.domain.result.ApiResult;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.PostConstruct;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.common.message.MessageExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @Description 商品浏览消息逻辑处理
 * @date 2019年6月4日下午7:52:50
 * @version V1.0
 * @author 邹立强 (zouliqiang@idstaff.com)
 */
@Component
public class UserViewProductConsumer extends MqConsumerTemplate {

	@Autowired
	private RedisUtil redisUtil;
	private Logger logger = LoggerFactory.getLogger(UserViewProductConsumer.class);
	@Value("${rocketmq.server.namesrvAddr}")
	private String namesrvAddr;
	@Value("${rocketmq.user.view.product.consumer.group}")
	private String customerGroup;
	@Value("${rocketmq.user.view.product.topic}")
	private String topic;
	@Value("${rocketmq.user.view.product.tags}")
	private String tags = "*";
	// 最近浏览的商品
	private static final String KEY_PREFIEX = "moses:user_viewed_products_";
	private static final String KEY_PREFIEX_CATEGORY_PREFERENCE = "moses:level3hobby_";
	private static final String KEY_PREFIEX_SHORT_INTEREST = "moses:u_sl_";
	private static final int NUMBER_RECORDS_KEEPING = 100;
	private static final int DAYS_EXPIRY = 30;
	private static final int TIME_DIFF = 259200000;// 60*60*24*1000*3 3天
	public static final int CATEGORY_PREFERENCE_TIME = 259200; // 60 * 60 * 24 * 3 3天
	public static final int SHORT_INTEREST_TIME = 86400; // 60 * 60 * 24 1天
	// 字符串格式化为8位小数
	private final static DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.000000000");
	@Autowired
	private ProductDetailCache productDetailCacheNoCron;
	@Autowired
	private UcServerService ucServerService;
	private static final String TAG_API_RAW = "api.biyao.com:raw_pdetail";

	@PostConstruct
	public void init() {
		initConsumer(namesrvAddr, customerGroup, topic, tags);
	}

	@BProfiler(key = "UserViewProductConsumer.handleMessage", monitorType = { MonitorType.TP,
			MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR })
	public void handleMessage(List<MessageExt> msgs) {

			// 将消息解析成kv结构
			Function<String, Map<String, String>> transform = msg -> Arrays.stream(msg.split("\t"))
					.filter(x -> x.contains("=")).map(String::trim).collect(Collectors.toMap(x -> {
						int idx = x.indexOf('=');
						return x.substring(0, idx);
					}, x -> {
						int idx = x.indexOf('=');
						return idx < x.length() - 1 ? x.substring(idx + 1) : "";
					}));

			// 对tag=api.biyao.com:raw_pdetail的消息需要做规则过滤，即不记录用户足迹。
			// 过滤规则：(uuid=9**** || uuid=7****) && pf=mweb
			Stream<Map<String, String>> s1 = msgs.stream().filter(msg -> TAG_API_RAW.equals(msg.getTags()))
					.map(msg -> new String(msg.getBody())).map(transform).filter(x -> x.containsKey("uu"))
					.filter(x -> !"mweb".equals(x.get("pf"))
							|| !x.get("uu").startsWith("9") && !x.get("uu").startsWith("7"));
			Stream<Map<String, String>> s2 = msgs.stream().filter(msg -> !TAG_API_RAW.equals(msg.getTags()))
					.map(msg -> new String(msg.getBody())).map(transform);
			Stream.concat(s1, s2).forEach(x -> {
				if (!x.containsKey("uu") || x.get("uu").equals("") || !x.containsKey("pid") || x.get("pid").equals("")) {
					logger.warn("invalid msg: {}", x);
					return;
				}
				String uuid = x.get("uu");
				String key = KEY_PREFIEX + uuid;

				String pid = x.get("pid");
				redisUtil.lpush(key, pid + ":" + System.currentTimeMillis());
				redisUtil.ltrim(key, 0, NUMBER_RECORDS_KEEPING - 1); // 限制缓存数目
				redisUtil.expire(key, 60 * 60 * 24 * DAYS_EXPIRY);
				List<String> productIds = redisUtil.lrange(KEY_PREFIEX + uuid, 0, NUMBER_RECORDS_KEEPING);
				dealThirdCategoryPreference(uuid,productIds);
				dealShortInterest(uuid);
				dealCategoryProductModel(uuid,productIds);
			});
	}
    
	/**
	* @Description 处理 类目推荐的商品数
	* @param uuid
	* @param productIds 
	* @version V1.0
	* @auth 邹立强 (zouliqiang@idstaff.com)
	 */
	private void dealCategoryProductModel(String uuid, List<String> productIds) {
		try {
			 //UC获取曝光浏览集合
            ApiResult<User> result = new ApiResult<>();
            try{
                UserRequest ur = new UserRequest();
                ur.setUuid(uuid);
                ur.setCaller("mosesqueue.biyao.com");
                List<String> fields = new ArrayList<>();
                fields.add(UserFieldConstants.EXPPIDS);
                ur.setFields(fields);
                result =ucServerService.query(ur);
            }catch(Exception e){
                logger.error("uc获取数据异常："+e.getMessage());
            }
            List<String> expProductIds=result.getData().getExpPids();
			//List<String> expProductIds = redisUtil.lrange(CommonConstants.PRODUCT_EXPOSURE_KEY_PREFIEX + uuid, 0,
			//		EXP_NUMBER_RECORDS_KEEPING - 1);
			// 曝光类目Id：num
			Map<Long, Integer> exposureMap = getTodayCategoryCount(expProductIds);
			// 浏览类目Id：num
			Map<Long, Integer> viewMap = getTodayCategoryCount(productIds);
			// 曝光和浏览类目集合merge
			Set<Long> categorySet = new HashSet<Long>();
			categorySet.addAll(exposureMap.keySet());
			categorySet.addAll(viewMap.keySet());
			if (categorySet.size() == 0) {
				return;
			}
			// 最高类目的浏览数
			Integer viewMax = Collections.max(viewMap.values());
			StringBuffer resultValue = new StringBuffer();
			for (Long categoryId : categorySet) {
				Integer cateExpNum = exposureMap.get(categoryId);
				Integer cateViewNum = viewMap.get(categoryId);
				cateExpNum = cateExpNum != null ? cateExpNum : 0;
				cateViewNum = cateViewNum != null ? cateViewNum : 0;
				// 计算每个类目的降权分数 exp_score = 1/(max(cate_exp_num-cate_clk_num, 0) + 1)
				// cate_exp_num是类目的曝光数，cate_clk_num是类目的点击数
				BigDecimal expScoreBd = new BigDecimal(1.0 / (Math.max((cateExpNum - cateViewNum), 0) + 1));
				Double expScore = expScoreBd.setScale(4, BigDecimal.ROUND_HALF_UP).doubleValue();
				// 计算每个类目的加权分数 clk_score =
				// (cate_clk_num/max_clk_num)+(cate_clk_num>=cate_exp_num ? 0.5 : 0)
				// max_cate_clk_num是指点击数最高的类目对应的点击数。相当于最大值归一化。加0.5的原因是cate_exp_num-cate_clk_num>0的时候，exp_score
				// <= 0.5。这样能保证被点击的类目的权重始终比充分曝光的类目得分高
				BigDecimal viewScoreBd = new BigDecimal(
						(cateViewNum * 1.0 / viewMax) + ((cateViewNum >= cateExpNum) ? 0.5 : 0));
				Double viewScore = viewScoreBd.setScale(4, BigDecimal.ROUND_HALF_UP).doubleValue();
				// 每个类目可推荐商品数为 floor(2*(exp_score+clk_score))
				int count = (int) Math.floor(2 * (viewScore + expScore));
				resultValue.append(categoryId).append(":").append(count).append(",");
			}
			if (StringUtils.isNotBlank(resultValue)) {
				resultValue.deleteCharAt(resultValue.length() - 1);
				String key = CommonConstants.CATE_PRODUCT_TODAY_KEY_PREFIEX + uuid;
				redisUtil.setString(key, resultValue.toString(), CATEGORY_PREFERENCE_TIME);
				redisUtil.expire(key, 86400);// 1天
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	* @Description 获取商品集合类目Id以及商品出现数量 
	* @param productIds
	* @return Map<Long,Integer> 
	* @version V1.0
	* @auth 邹立强 (zouliqiang@idstaff.com)
	 */
	private Map<Long, Integer> getTodayCategoryCount(List<String> productIds) {
		Map<Long, Integer> categoryMap = new HashMap<Long, Integer>();
		long nowTime = System.currentTimeMillis();
		long todayStartTime = nowTime - (nowTime + TimeZone.getDefault().getRawOffset()) % (1000 * 3600 * 24);
		if (CollectionUtils.isNotEmpty(productIds)) {
			for (String productIdTime : productIds) {
				if (StringUtils.isNotBlank(productIdTime)) {
					String productId = productIdTime.split(":")[0];
					String time = productIdTime.split(":")[1];
					if (StringUtils.isBlank(productId)) {
						continue;
					}
					if (StringUtils.isBlank(time)) {
						continue;
					}
					ProductInfo productInfo = productDetailCacheNoCron.getProductInfo(Long.valueOf(productId));
					if (productInfo == null) {
						continue;
					}
					Long category3Id = productInfo.getThirdCategoryId();
					Long category2Id = productInfo.getSecondCategoryId();
					if (CommonConstants.SPECIAL_CATEGORY2_IDS.contains(category2Id)) { // 二级类目是眼镜的需要特殊处理 后台二级类目是39
						category3Id = category2Id;
					}
					if ( todayStartTime <= Long.valueOf(time).longValue()) {
						if (!categoryMap.containsKey(category3Id)) {
							categoryMap.put(category3Id, 1);
						} else {
							categoryMap.put(category3Id, categoryMap.get(category3Id) + 1);
						}
					}
				}
			}
		}
		return categoryMap;
	}

	/**
	 * @Description 通过用户浏览行为获取三级类目偏好
	 * @param uuid
	 * @version V1.0
	 * @auth 邹立强 (zouliqiang@idstaff.com)
	 */
	private void dealThirdCategoryPreference(String uuid,List<String> productIds) {
		try {
			long now = System.currentTimeMillis();
			Map<Long, Integer> category3CountMap = new HashMap<Long, Integer>();
			StringBuffer redisValue = new StringBuffer();
			for (String productIdTime : productIds) {
				if (StringUtils.isNotBlank(productIdTime)) {
					String productId = productIdTime.split(":")[0];
					String time = productIdTime.split(":")[1];
					if (now - Long.valueOf(time) <= TIME_DIFF) {
						ProductInfo productInfo = productDetailCacheNoCron.getProductInfo(Long.valueOf(productId));
						if (productInfo == null) {
							continue;
						}
						Long category3Id = productInfo.getThirdCategoryId();
						Long category2Id = productInfo.getSecondCategoryId();
						if (CommonConstants.SPECIAL_CATEGORY2_IDS.contains(category2Id)) { // 二级类目是眼镜的需要特殊处理 后台二级类目是39
							category3Id = category2Id;
						}
						if (!category3CountMap.containsKey(category3Id)) {
							category3CountMap.put(category3Id, 1);
						} else {
							category3CountMap.put(category3Id, category3CountMap.get(category3Id) + 1);
						}
					}
				}
			}
			for (Map.Entry<Long, Integer> entry : category3CountMap.entrySet()) {
				Long category3Id = entry.getKey();
				Integer count = entry.getValue();
				redisValue.append(category3Id).append(":").append(count).append(",");
			}
			if (StringUtils.isNotBlank(redisValue)) {
				redisValue.deleteCharAt(redisValue.length() - 1);
			}
			redisUtil.setString(KEY_PREFIEX_CATEGORY_PREFERENCE + uuid, redisValue.toString(),
					CATEGORY_PREFERENCE_TIME);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * @Description 计算用户短兴趣向量 
	 * @param uuid
	 * @return void
	 * @version V1.0
	 * @auth 邹立强 (zouliqiang@idstaff.com)
	 */
	public void dealShortInterest(String uuid) {
		/*
		 * List<String> viewProductIds= Arrays.asList(new
		 * String[]{"1300925044","1301325029","1302045238","1302015015","1301565214"});
		 */
		// pid:t1,pid:t2,pid:t3,
		try {
			if (StringUtils.isBlank(uuid)) {
				return;
			}
			List<String> viewProductValues = redisUtil.lrange(KEY_PREFIEX + uuid, 0, 4);
			// 浏览商品集合
			List<String> viewProductIds = new ArrayList<String>();
			long nowTime = System.currentTimeMillis();
			long todayStartTime = nowTime - (nowTime + TimeZone.getDefault().getRawOffset()) % (1000 * 3600 * 24);
			long today5Time = todayStartTime + 18000000;// 5*60*60*1000;
			if (CollectionUtils.isNotEmpty(viewProductValues)) {
				for (String pids : viewProductValues) {
					if (StringUtils.isNotBlank(pids)) {
						String[] pidsSplit = pids.split(":");
						String pid = pidsSplit[0];
						String time = pidsSplit[1];
						if (StringUtils.isNotBlank(time) && todayStartTime <= Long.valueOf(time).longValue()) {
							viewProductIds.add(pid);
						}
					}
				}
			}
			if (CollectionUtils.isNotEmpty(viewProductIds)) {
				String[] arrayPids = viewProductIds.toArray(new String[viewProductIds.size()]);
				// 获取商品向量 每个商品向量格式-0.208675832,0.052863453.......
				List<String> vectorList = redisUtil.hmget(RedisKeyConstant.MOSES_PRODUCT_VECTOR, arrayPids);
				// List<String> vectorList=Arrays.asList(new String[]{
				// "-0.208675832,0.052863453,0.512428880,-0.175380528,0.638493896,0.346288741,0.144150764,0.219553649,-0.616361976,0.297394633,-0.689053893,0.769556344,-0.490721047,0.244733706,-0.264220476,0.277600616,-0.154490888,0.245241344,0.330956459,0.114069611,-0.118312575,0.003976144,-0.352578610,-0.105060421,0.033171233,0.271619052,-0.099564306,0.015694732,0.450419098,0.113456436,-0.417058706,-0.294031829,0.286986470,-0.422549188,-0.627372921,0.804516435,-0.516465068,0.079811096,1.010558248,0.411974341,-0.574378610,-0.491938055,0.394402474,-0.124120496,-0.131825805,0.075781830,0.613098145,-0.548911393,0.147302151,-0.448676288",
				// "-0.729932666,0.333131850,-0.379231006,-0.243864596,0.069204606,0.198799625,-0.141598746,0.170671999,-0.402278274,-0.039393589,0.496601731,0.605247378,-0.139284417,-0.140373975,0.510646641,-0.893003166,0.790122032,0.247228205,0.175342247,0.352480710,0.449915707,-0.485127330,0.268630058,0.499821454,-0.783644319,-0.014777108,-0.189300865,-0.104116850,-0.278918415,0.379990131,0.230946019,0.007515139,-0.112689644,-0.699090123,-0.019074483,0.115558945,-0.314017713,-0.387779057,-0.040858675,-0.252574801,-0.154627576,0.506839097,0.053734846,-0.600098848,-0.190946892,0.689280033,1.000962377,-0.940361321,-0.219834656,-0.356919080",
				// "-1.078960776,0.180056572,-0.432696402,0.986843109,-0.704187214,0.442016900,-0.292727619,0.788003266,-0.773804069,-0.189120337,0.113592714,0.006473118,-0.109127894,0.005369408,1.125480890,-0.632593095,-0.240049034,-0.218243480,-0.311957240,-0.072615683,-0.274135858,-0.112319931,-0.034397371,0.252400964,0.507190347,0.042127747,-0.456425548,-0.276131451,0.432567477,-0.009520668,0.023298062,0.486585230,0.811293006,-0.692156017,-1.096410394,0.112680867,0.351406604,0.576764226,0.259636730,-0.545181215,-0.121789023,-0.004384230,-0.333073229,0.010041585,-0.609748721,0.059668005,-0.307262152,-0.483868867,0.195394009,-0.378732324",
				// "-0.201692984,-0.436811030,0.257070690,0.378232867,-0.266107142,0.251651675,0.242080167,0.547603130,-0.377499998,0.220943987,0.333419293,0.298102051,-0.408428818,-0.210061759,0.159161747,-0.055554938,0.224105164,0.087975420,0.009202607,-0.019720586,0.938306808,-0.794177771,0.111119002,0.216792226,-0.160583511,0.101275444,0.028139273,0.341984749,-0.140491962,0.208298549,-0.410864741,0.013096244,-0.343189627,-0.504862070,-0.254203320,-0.025094397,0.265737951,-0.111304894,-0.239371285,0.069673344,0.644891024,-0.180710524,0.328559875,-0.553967416,-0.643292546,0.559498608,0.489058495,-1.114761829,0.330337644,-0.025297657",
				// "-0.939308286,0.319683939,0.097880900,0.499881327,0.214588284,0.082398668,-0.404440522,0.227038339,0.241738707,-0.393641353,-0.055176329,0.366411686,-0.177655220,-0.091618657,0.118226238,-0.390751630,-0.060453828,0.094511926,-0.128836930,0.086336181,0.207792014,-0.538753271,0.077898413,0.052523408,0.132642880,0.335249782,-0.268911332,0.117930897,0.438739419,0.283350557,-0.064826019,0.464008778,0.385763615,-0.325390190,-0.061163910,0.120560952,-0.725631297,-0.096313447,0.266871601,-0.258934826,-0.140765727,-0.203253731,0.528626978,-0.177239269,-0.463389337,0.257200032,0.499819905,-0.193047538,0.169986740,-1.000519633
				// "
				// });

//				List<Float> list = new ArrayList<Float>();
				// 用最近浏览的商品向量初始化用户向量
				double[] userVector = new double[50]; // 默认50维
				int lastIndex = 0, productCount = 0;
				for (int j=0; j<vectorList.size() && j<arrayPids.length; j++){
					if (StringUtils.isNotBlank(vectorList.get(j))){
						String[] productVectorStr = vectorList.get(j).split(",");
						userVector = new double[productVectorStr.length];
						try{
							for (int k=0; k<productVectorStr.length; k++){
								userVector[k] = Double.valueOf(productVectorStr[k]);
							}
							productCount += 1;
							lastIndex = j + 1; // 跳出之前需要加1
							break;
						}catch (Exception e){
							logger.error("商品向量错误：pid={}", arrayPids[j], e);
						}
					}
					lastIndex = j + 1; // 循环完成需要加1
				}
				// 与其他浏览的商品向量叠加
				for (int j=lastIndex; j<vectorList.size() && j<arrayPids.length; j++){
					if (StringUtils.isNotBlank(vectorList.get(j))){
						String[] productVectorStr = vectorList.get(j).split(",");
						if (productVectorStr.length != userVector.length){
							continue;
						}
						try{
							for (int k=0; k<productVectorStr.length; k++){
								userVector[k] = userVector[k] + Double.valueOf(productVectorStr[k]);
							}
							productCount += 1;
						}catch (Exception e){
							logger.error("商品向量错误：pid={}", arrayPids[j], e);
						}
					}
				}
				// 将用户向量写入redis
				if (productCount > 0) {
					StringBuffer sb = new StringBuffer();
					for (int j = 0; j < userVector.length; j++) {
						double avg = userVector[j] / productCount;
//						DecimalFormat decimalFormat = new DecimalFormat("0.000000000");
						String p = DECIMAL_FORMAT.format(avg);// format 返回的是字符串
						sb.append(p).append(",");
					}

					if (StringUtils.isNotBlank(sb)) {
						sb.deleteCharAt(sb.length() - 1);
						if (nowTime > todayStartTime && nowTime < today5Time) {
							redisUtil.setString(KEY_PREFIEX_SHORT_INTEREST + uuid, sb.toString(),
									(int) (today5Time - nowTime) / 1000);
						} else {
							redisUtil.setString(KEY_PREFIEX_SHORT_INTEREST + uuid, sb.toString(),
									(int) (today5Time + 86400000 - nowTime) / 1000);
						}
//						logger.error("用户兴趣向量写入成功:uu={}, vec={}", uuid, sb);
					}
				}

//				 行向量计算
//				int vectorSize = 0;
//				for (int j = 0; j < vectorList.size(); j++) {
//					String string = vectorList.get(j);
//					if (StringUtils.isNotBlank(string)) {
//						vectorSize++;
//						String[] productSplit = string.split(",");
//						for (int i = 0; i < productSplit.length; i++) {
//							if (j == 0) {
//								list.add(Float.parseFloat(productSplit[i]));
//							} else {
//								list.set(i, Float.parseFloat(productSplit[i]) + list.get(i));
//							}
//						}
//					}
//				}
//				if (CollectionUtils.isNotEmpty(list)) {
//					StringBuffer sb = new StringBuffer();
//					for (Float sum : list) {
//						float avg = sum / vectorSize;
//						DecimalFormat decimalFormat = new DecimalFormat("0.000000000");
//						String p = decimalFormat.format(avg);// format 返回的是字符串
//						sb.append(p).append(",");
//					}
//					if (StringUtils.isNotBlank(sb)) {
//						sb.deleteCharAt(sb.length() - 1);
//						if (nowTime > todayStartTime && nowTime < today5Time) {
//							redisUtil.setString(KEY_PREFIEX_SHORT_INTEREST, sb.toString(),
//									(int) (today5Time - nowTime) / 1000);
//						} else {
//							redisUtil.setString(KEY_PREFIEX_SHORT_INTEREST, sb.toString(),
//									(int) (today5Time + 86400000 - nowTime) / 1000);
//						}
//					}
//				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
