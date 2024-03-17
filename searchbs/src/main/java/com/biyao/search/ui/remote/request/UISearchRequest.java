package com.biyao.search.ui.remote.request;

import java.math.BigDecimal;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.ws.rs.QueryParam;

import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.fastjson.JSONObject;
import com.biyao.mac.client.redbag.shop.privilegebag.dto.ShowPrivilegeLogoResultDto;
import com.biyao.search.common.constant.SearchLimit;
import com.biyao.search.common.enums.PlatformEnum;
import com.biyao.search.common.enums.SearchOrderByEnum;
import com.biyao.search.common.model.FacetItem;
import com.biyao.search.ui.constant.PageSourceEnum;
import com.biyao.search.ui.enums.ActivityEnum;
import com.biyao.search.ui.util.IdCalculateUtil;
import com.google.common.base.Strings;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Data
public class UISearchRequest extends UIBaseRequest {
	private static final String SPECIAL_CHAR_REGEXP = "[`~!@#$%^&*()+=|{}':;',\\[\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？\t\r\n]"
			+ "|[\ud83c\udc00-\ud83c\udfff]|[\ud83d\udc00-\ud83d\udfff]|[\u2600-\u27ff]";
	private static Pattern pattern = Pattern.compile(SPECIAL_CHAR_REGEXP,
			Pattern.UNICODE_CASE | Pattern.CASE_INSENSITIVE);

	/**
	 * 活动一起拼标识
	 */
	public static final String GROUP_BUG_ACTIVITYSTR = "活动:一起拼";

	/**
	 * 阶梯团
	 */
	public static final String LADDERGROUP_BUG_ACTIVITYSTR = "活动:阶梯团";

	private static Logger logger = LoggerFactory.getLogger(UISearchRequest.class);

	@QueryParam("sid")
	private String sid;

	@QueryParam("odb")
	private String orderByStr;

	@QueryParam("sp")
	private String searchParam;

	@QueryParam("tpid")
	private Integer topicId;

	@QueryParam("aid")
	private String aid;

	/**
	 * key:code键值对，多个code用逗号隔开，多对用|隔开 eg：size:36,38|color:green
	 */
	@QueryParam("facet")
	private String facetStr;

	/**
	 * 搜索提示词参数，URLEncode的JSON字符串 input index
	 */
	@QueryParam("sugp")
	private String sugp;

	@QueryParam("queryFrom")
	@Setter
	@Getter
	private String queryFrom;

	/**
	 * 内部参数
	 */
	private String query;
	/**
	 * 原始搜索词，未过滤
	 */
	private String originalQuery;
	/**
	 * 是否跳转到使用topic聚合商品承载的搜索结果页
	 * 1:是 0:否
	 */
	private Integer toTopicPage = 0;
	/**
	 * 是否跳转到商家商品承载的搜索结果页
	 * 1:是 0:否
	 */
	private Integer toSupplierListPage = 0;
	/**
	 * 筛选项
	 */
	private List<FacetItem> facets = new ArrayList<>();
	/**
	 * 排序规则，默认SearchOrderByEnum.NORMAL
	 */
	private SearchOrderByEnum orderBy = SearchOrderByEnum.NORMAL;
	/**
	 * 是否跳转到朋友买商品承载的搜索结果页
	 * 1:是 0:否
	 */
	private Integer toFriendsBuy;
	/**
	 * 搜索建议词原输入内容
	 */
	private String suggestionInput;
	/**
	 * 选择搜索建议词时，搜索建议词的位置
	 */
	private Integer suggestionIndex;
	/**
	 * 选择搜索建议词时，搜索建议词的唯一标志
	 */
	private String suggestionUniqueId;

	// 一起拼1.3.3新增字段
	/**
	 * 活动搜索
	 * 0: 无活动，1:一起拼,3:买二返一，4：津贴抵扣
	 */
	@QueryParam("toActivity")
	private Integer toActivity;

	/**
	 * 店铺名称，店铺内搜索时使用
	 */
	@QueryParam("supplierId")
	private String supplierId;

	/**
	 * 阶梯团id
	 */
	@QueryParam("groupId")
	private String groupId;

	/**
	 * 阶梯团活动id
	 */
	@QueryParam("groupActId")
	private String groupActId;

	/**
	 * 特权金2.2.1 增加当前可用特权金面额
	 */
	@QueryParam("privilegeAmount")
	private String privilegeAmount;

	/**
	 * 搜索来源标识
	 */
	@Setter
	@Getter
	private String sourceId;
	/**
	 * 筛选项
	 */
	@Setter
	@Getter
	@QueryParam("sfilter")
	private String imageFilterParam;

    /**
     * 特权金
     */
	private ShowPrivilegeLogoResultDto privilegeLogo;

    /**
     * 一起拼标识
     */
	private Boolean isJumpTogroup;

    /**
     * 身份标识（true:新客 false:老访客、老客）
     */
    private Boolean isNewUser;

	/**
	 * 用户个性化尺码（Map<category3Id,<size,size...>>）
	 */
    private Map<Long, List<String>> userSizeMap;

	/**
	 * 用户总津贴数
	 */
	@Setter
	@Getter
	private BigDecimal userTotalAllowanceAmt;


	/**
	 * 是否支持必要造物商品，isSupportCreation 1:支持必要造物商品  0：不支持必要造物商品
	 */
	@Setter
	@Getter
	private Integer isSupportCreation = 0;


	/**
	 * 是否支持webp图片格式 1：支持 0：不支持，，小程序使用
	 */

	@Setter
	@Getter
	@QueryParam("isSupportWebP")
	private Integer isSupportWebP;



	/**
	 * 处理请求传入参数
	 */
	@Override
	public void preHandleParam() {
		super.preHandleParam();
		
		
		Map<String, String> searchParamMap = new HashMap<>();
		if (Strings.isNullOrEmpty(searchParam)) {
			searchParam = "";
		}
		String[] sps = searchParam.split("&");
		for (String sp : sps) {
			sp = URLDecoder.decode(sp);
			if (!StringUtils.isBlank(sp)) {
				int index = sp.indexOf("=");
				if(index > 0){
                    searchParamMap.put(sp.substring(0,index),sp.substring(index+1));
                }
			}
		}
		
		if (PlatformEnum.MINI.getName().equals(this.platform.getName())) {
			if (org.apache.commons.lang3.StringUtils.isNotEmpty(searchParam)) {
				
				String weappDecode = URLDecoder.decode(searchParam);
				if (weappDecode.contains("嘟嘟唇的秘密&tpid")||weappDecode.contains("化身圣诞小精灵&tpid")||weappDecode.contains("榜&tpid")) {
					String[] split = weappDecode.split("&");
					for (String sp : split) {
						if(!StringUtils.isBlank(sp)) {
							String[] items = sp.split("=");
							searchParamMap.put(items[0], items[1]);
						}
					}
				}
			}
		}
		
		this.query = searchParamMap.get("q");
		this.originalQuery = searchParamMap.get("q");
		this.toTopicPage = searchParamMap.get("toTP") == null ? 0 : Integer.valueOf(searchParamMap.get("toTP"));
		this.topicId = searchParamMap.get("tpid") == null ? null : Integer.valueOf(searchParamMap.get("tpid"));
		this.toSupplierListPage = searchParamMap.get("toSP") == null ? 0 : Integer.valueOf(searchParamMap.get("toSP"));
		this.toFriendsBuy = searchParamMap.get("toFB") == null ? 0 : Integer.valueOf(searchParamMap.get("toFB"));
		this.orderBy = SearchOrderByEnum.getBycode(orderByStr);
		this.toActivity = toActivity == null ? 0 : toActivity;
		String facetSuffix = "";
		if (ActivityEnum.GROUP_BUY.getCode().equals(toActivity)) {
			// 一起拼活动页面ID
			this.pageId = siteId + "-100018-500231";
			facetSuffix = GROUP_BUG_ACTIVITYSTR;

			String sch = this.getSch();
			if (!StringUtils.isBlank(sch)) {
				sch = URLDecoder.decode(sch);
				JSONObject parseObject = null;
				try {
					parseObject = JSONObject.parseObject(sch);
					sourceId = parseObject.getString("sourceId");
				} catch (Exception e) {
					logger.error("[严重异常]sch不规范: sch={}", sch, e);
				}
			}

			if (!StringUtils.isBlank(sourceId) && sourceId.equals(PageSourceEnum.JOINGROUP.getSourceId())) {
				this.pageId = siteId + "-100018-500135";
			}

		} else if (ActivityEnum.LADDER_GROUP.getCode().equals(toActivity)) {
			facetSuffix = LADDERGROUP_BUG_ACTIVITYSTR;
		}

		if (Strings.isNullOrEmpty(facetStr)) {
			facetStr = facetSuffix;
		} else if (!Strings.isNullOrEmpty(facetStr) && !facetStr.contains(facetSuffix)) {
			facetStr = facetStr + "|" + facetSuffix;
		}


		// 处理搜索建议词参数
		if (!StringUtils.isBlank(this.sugp)){
			try{
				JSONObject suggestionParamJson = JSONObject.parseObject(URLDecoder.decode(this.sugp));
				this.suggestionInput = suggestionParamJson.getString("input");
				this.suggestionIndex = suggestionParamJson.getInteger("index");
				this.suggestionUniqueId = suggestionParamJson.getString("uniqId");
			}catch (Exception e){
				logger.error("[严重异常]sugp解析失败: sugp={}", this.sugp , e );
			}
		}

		// key:code键值对，多个code用逗号隔开，多对用|隔开
		if (!Strings.isNullOrEmpty(facetStr)) {
			try {
				String[] facets = facetStr.split("\\|");
				for (String facet : facets) {
					String[] parts = facet.split(":");

					FacetItem item = new FacetItem();
					item.setKey(parts[0]);
					item.setValues(Arrays.asList(parts[1].split(",")));

					this.facets.add(item);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		// 请求参数过滤特殊符号
		if (query == null) {
			this.query = "";
		}
		this.query = pattern.matcher(query).replaceAll("").trim();
		if (query.length() > SearchLimit.MAX_QUERY_LENGTH) {
			this.query = query.substring(0, SearchLimit.MAX_QUERY_LENGTH);
		}

		if (groupId == null) {
			this.groupId = "";
		}
		if (groupActId == null) {
			this.groupActId = "";
		}

		/* sid处理 */
		if (Strings.isNullOrEmpty(sid)) {
			this.sid = IdCalculateUtil.createSid(uuid, query);
		}
		// 添加aid参数，用于埋点追踪
		if (Strings.isNullOrEmpty(aid)) {
			JSONObject ctpJson = JSONObject.parseObject(this.ctp);
			this.aid = IdCalculateUtil.createAid(ctpJson == null ? "" : ctpJson.getString("did"));
		}
	}

	@Override
	public boolean checkParameter() {
		return super.checkParameter();
	}

}
