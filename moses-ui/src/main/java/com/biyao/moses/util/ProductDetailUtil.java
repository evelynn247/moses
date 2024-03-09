package com.biyao.moses.util;

import com.alibaba.fastjson.JSONObject;
import com.biyao.mac.client.redbag.shop.privilegebag.dto.ShowPrivilegeLogoResultDto;
import com.biyao.mac.client.redbag.shop.privilegebag.service.IShopRedBagPrivilegeBagService;
import com.biyao.moses.cache.ProductDetailCache;
import com.biyao.moses.cache.SearchProductLabelCache;
import com.biyao.moses.common.constant.UPCUserTypeConstants;
import com.biyao.moses.constants.ColorCodeConsts;
import com.biyao.moses.context.ByUser;
import com.biyao.moses.model.BaseProductTemplateInfo;
import com.biyao.moses.model.ProductTemplateInfo;
import com.biyao.moses.model.template.Label;
import com.biyao.moses.model.template.TemplateInfo;
import com.biyao.moses.model.template.entity.TotalTemplateInfo;
import com.biyao.moses.params.ProductInfo;
import com.biyao.moses.params.SuProductInfo;
import com.biyao.search.common.enums.PlatformEnum;
import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 商品详情逻辑处理
 * 
 * @author monkey
 * @date 2018年9月4日
 */
@Component
@Slf4j
public class ProductDetailUtil {

	@Autowired
	private IShopRedBagPrivilegeBagService shopRedBagPrivilegeBagService;
	
	@Autowired
	private ProductDetailCache productDetailCache;

	@Autowired
	private SearchProductLabelCache searchProductLabelCache;
	
	private DecimalFormat df = new DecimalFormat("#.##");

	// 图标展示信息 左上角提示类型。0没有，1新品 2 团购 3 一起拼
	public String getShowIcon(ProductInfo productInfo) {
		if (isNewProduct(productInfo)) {
			return "1";
		} else {
			return "0";
		}
	}

	public boolean isNewProduct(ProductInfo productInfo) {
		/* 判断是否是新品，7天以内 */
		try {
			Date firstOnshelfDate = productInfo.getFirstOnshelfTime();
			int days = 0;
			if (firstOnshelfDate != null) {
				days = (int) ((System.currentTimeMillis() - firstOnshelfDate.getTime()) / (1000 * 3600));
			}
			return days <= 72;
		} catch (Exception e) {
			return false;
		}
	}
	
	public String getCommentInfo(ProductInfo productInfo) {
		try {
			
//			Long positiveComment = productInfo.getSalesVolume7();
			Integer positiveComment = productInfo.getPositiveComment();
			
			if (positiveComment >= 1) {
				return (positiveComment < 10000 ? positiveComment
						: String.format("%.1fw", (float) positiveComment / 10000)) + "条好评";
			} else {
				return "";
			}
		} catch (Exception e) {
			return "";
		}
	}

	public List<Label> getLabels(ProductInfo productInfo, ByUser byUser,
			ShowPrivilegeLogoResultDto isUserHasPrivilege, TotalTemplateInfo totalTemplateInfo) {
		ArrayList<Label> result = new ArrayList<Label>();
		try {
			if (productInfo.getSearchLabels() != null) {
				List<String> labelContent = productInfo.getSearchLabels().size() <= 2 ? productInfo.getSearchLabels()
						: productInfo.getSearchLabels().subList(0, 2);
				List<Label> labels = labelContent.stream().map(i -> {
					Label label = new Label();
					label.setContent(i);
					return label;
				}).collect(Collectors.toList());
				result.addAll(labels);
			}

			// 定制标签，如果支持 "图案定制"
			if (productInfo.getSupportTexture() != null &&
					(productInfo.getSupportTexture() == 1 || productInfo.getSupportTexture() == 2)){
				Label label = new Label();
				label.setContent("定制");
				label.setTextColor(ColorCodeConsts.TITLE_COLOR_SUPPORT_TEXTURE);
				label.setRoundColor(ColorCodeConsts.BORDOR_COLOR_SUPPORT_TEXTURE);
				result.add(label);
			}

			PlatformEnum platformEnum = byUser.getPlatform();
			boolean isShowPrivilege = isShowPrivilege(productInfo, byUser, platformEnum,
					isUserHasPrivilege);
			// 一起拼逻辑大于特权金
			if (productInfo.getIsToggroupProduct() == 1) { // 支持一起拼，加入一起拼标签
				Label together = new Label();
				together.setContent("一起拼");
				result.add(together);
			}
			if (productInfo.getIsLaddergroupProduct()!=null&& productInfo.getIsLaddergroupProduct() == 1) { // 阶梯团商品
				Label together = new Label();
				together.setContent("阶梯团");
				result.add(together);
			}

			// 特权金标签
			if (isShowPrivilege) {
				Label privilegeTag = new Label();
				privilegeTag.setContent("特权金");
				result.add(privilegeTag);
			}
			if(byUser.getIsShowPrivilege()){
				//商品支持新手特权金，就加入新手特权金。 老客不展示新手特权金标签
				if(productInfo.getNewUserPrivilege()==1
						&& !PlatformEnum.M.getName().equals(platformEnum.getName())
						&& !PlatformEnum.MINI.getName().equals(platformEnum.getName())
						&& byUser.getUpcUserType()!= UPCUserTypeConstants.CUSTOMER){
					Label newPrivilegeTag = new Label();
					newPrivilegeTag.setContent("新手特权金");
					result.add(newPrivilegeTag);
				}
			}

			String labelConfigStr = searchProductLabelCache.getSearchProductLabel();
			JSONObject labelConfig = Strings.isNullOrEmpty(labelConfigStr) ? new JSONObject()
					: JSONObject.parseObject(labelConfigStr);

			// 标签颜色处理
			for (Label label2 : result) {
				String content = label2.getContent();
				if (!labelConfig.containsKey(content)) {
					label2.setColor(ColorCodeConsts.DEFAULT_LABEL_COLOR);
					label2.setTextColor(ColorCodeConsts.TITLE_COLOR_WHITE);
					label2.setRoundColor(ColorCodeConsts.TITLE_COLOR_WHITE);
				} else {
					JSONObject thisConfig = labelConfig.getJSONObject(content);
					label2.setColor(thisConfig.getString("color")); // 背景颜色

					if (!thisConfig.containsKey("textColor")) { // 文本颜色
						label2.setTextColor(ColorCodeConsts.TITLE_COLOR_WHITE);
					} else {
						label2.setTextColor(thisConfig.getString("textColor"));
					}

					if (!thisConfig.containsKey("roundColor")) { // 边框颜色
						label2.setRoundColor(ColorCodeConsts.TITLE_COLOR_WHITE);
					} else {
						label2.setRoundColor(thisConfig.getString("roundColor"));
					}
				}
			}
		} catch (Exception e) {
			log.error("[严重异常]获取商品标签时出现异常，", e);
		}

		return result;
	}

	/**
	 * 是否要显示特权金
	 */
	public boolean isShowPrivilege(ProductInfo product, ByUser byUser, PlatformEnum platformEnum,
			ShowPrivilegeLogoResultDto isUserHasPrivilege) {

		boolean isShowPrivilege = false;

		try {
			if (isUserHasPrivilege == null || product == null
					|| org.springframework.util.StringUtils.isEmpty(isUserHasPrivilege.getUserType())
					|| org.springframework.util.StringUtils.isEmpty(isUserHasPrivilege.getPrivilegeLogoShowType())
					|| org.springframework.util.StringUtils.isEmpty(product.getOldUserPrivilege())
					|| org.springframework.util.StringUtils.isEmpty(product.getNewUserPrivilege())) {
				return isShowPrivilege;
			}
			// 类型中含有新客类型并且用户身份是新客，privilegeLogoShowType赋值为1返回,同理，通用特权金1，老客2，不展示
			// 0。无需关注userType
			if (isUserHasPrivilege.getPrivilegeLogoShowType() == 1 && product.getNewUserPrivilege() == 1) {
				// 新客特权金，通用特权金逻辑相同
				isShowPrivilege = true;
			}
			if (isUserHasPrivilege.getPrivilegeLogoShowType() == 2 && product.getOldUserPrivilege() == 1) {
				// 老客特权金
				isShowPrivilege = true;
			}
		} catch (Exception e) {
			log.error("[严重异常]获取显示特权金标记出现错误，product={} ", JSONObject.toJSONString(product), e);
		}
		return isShowPrivilege;
	}

	/**
	 * 用户是否拥有特权金
	 */
	public ShowPrivilegeLogoResultDto isUserHasPrivilege(String uid, String platform) {
		// 显示支持特权金
		com.biyao.mac.client.common.bean.Result<ShowPrivilegeLogoResultDto> privilegeRes = null;

		// 未登录用户、M站、PC 不显示支持特权金
		try {
			if (StringUtils.isNotBlank(uid) && Integer.valueOf(uid) > 0) {
				// zhaiweixi 20180822 改调新的方法
				privilegeRes = shopRedBagPrivilegeBagService.isShowPrivilegeLogo(Long.valueOf(uid.toString()));
			}
		} catch (Exception e) {
			log.error("[严重异常]调用shopRedBagPrivilegeBagService发生异常，uid {} ", uid, e);
			return null;
		}
		if (privilegeRes != null) {
			return privilegeRes.getData();
		} else {
			return null;
		}
	}

	public TemplateInfo buildProductTemplate(BaseProductTemplateInfo templateInfo, ProductInfo productInfo,
			Integer curDataIndex,ByUser user, TotalTemplateInfo totalTemplateInfo) {

		// 商品短标题
		templateInfo.setMainTitle(productInfo.getShortTitle());

		if (templateInfo instanceof ProductTemplateInfo) {
			ProductTemplateInfo feedTemplateInfo = (ProductTemplateInfo) templateInfo;
			// 制造商背景 ,卖点
			String supplierBack = StringUtils.isNotBlank(productInfo.getSupplierBackground())
					? productInfo.getSupplierBackground()
					: "";
			String salePoinit = StringUtils.isNotBlank(productInfo.getSalePoint()) ? productInfo.getSalePoint() : "";
			String subtitle = supplierBack + "|" + salePoinit;

			feedTemplateInfo.setSubtitle(subtitle);
			// 左上角icon
			feedTemplateInfo.setIsShowIcon(this.getShowIcon(productInfo));
			// 朋友买及好评数 暂时没有朋友买
			String thirdContent = this.getCommentInfo(productInfo) + "|" + "";
			feedTemplateInfo.setThirdContent(thirdContent);
			//设置对所有人可见的好评数数字(包含默认好评数)
			feedTemplateInfo.setGoodCommentAll(productInfo.getGoodCommentAll());
			//设置旧好评数数字
			feedTemplateInfo.setGoodComment(productInfo.getPositiveComment());
		}

		// 价格处理
		if (productInfo.getPrice() == null) {
			templateInfo.setPriceStr(null);
			templateInfo.setPriceCent(null);
		} else {
			templateInfo.setPriceStr(df.format(productInfo.getPrice() / 100.00));
			templateInfo.setPriceCent(String.valueOf(productInfo.getPrice()));
		}
		ShowPrivilegeLogoResultDto userHasPrivilege = user.getUserHasPrivilege();

		// 标签处理
		List<Label> labels = this.getLabels(productInfo, user, userHasPrivilege, totalTemplateInfo);
		templateInfo.setLabels(labels);
		dealWeatherLabel(templateInfo, totalTemplateInfo);

		Map<String, String> routerParams = templateInfo.getRouterParams();
		routerParams.put("suId", String.valueOf(productInfo.getSuId()));

		return templateInfo;
	}

	/**
	 * 对天气标签做特殊处理
	 * @param templateInfo
	 * @param totalTemplateInfo
	 */
	private void dealWeatherLabel(BaseProductTemplateInfo templateInfo, TotalTemplateInfo totalTemplateInfo){
		if(StringUtils.isBlank(totalTemplateInfo.getLabelContent())){
			return;
		}
		List<Label> labels = templateInfo.getLabels();
		if(labels == null){
			labels = new ArrayList<>();
			templateInfo.setLabels(labels);
		}
		//标签字数
		int count = 0;
		//是否存在爆品或精选两个互斥标签
		boolean isExistMutexLabel = false;
		// 标签颜色处理
		for (Label label2 : labels) {
			//计算标签字数
			String content = label2.getContent();
			if (content.equals("精选") || content.equals("爆品")) {
				if (!isExistMutexLabel) {
					count += content.length();
				}
				isExistMutexLabel = true;
			} else {
				count += content.length();
			}
		}

		//判断是否需要添加天气标签
		if(count <= 5 && StringUtils.isNotBlank(totalTemplateInfo.getLabelContent())){
			Label weatherLabel = new Label();
			weatherLabel.setContent(totalTemplateInfo.getLabelContent());
			weatherLabel.setColor(ColorCodeConsts.TITLE_COLOR_WHITE);
			weatherLabel.setTextColor(ColorCodeConsts.TITLE_COLOR_SUPPORT_TEXTURE);
			weatherLabel.setRoundColor(ColorCodeConsts.BORDOR_COLOR_SUPPORT_TEXTURE);
			labels.add(weatherLabel);
		}

	}
	public ProductInfo preSaveProductInfo(String productId,ByUser user) {

		// 获取本地线程中的商品信息，获取商品信息，拼接数据
//		Map<Long, ProductInfo> localProductInfo = user.getLocalProductInfo();

		// 添加商品spuid到本地线程，上报日志需要
		// UserContext.getUser().getCurRequestPids().add(productId);
//		ProductInfo productInfo = localProductInfo.get(Long.valueOf(productId));
		ProductInfo productInfo = productDetailCache.getProductInfo(Long.valueOf(productId));
		
		if (productInfo==null) {
			log.error("[严重异常]当前商品不存在pid={}",productId);
		}
		
		return productInfo;
	}

	/**
	 * 获取su的详细信息
	 * @param productInfo
	 * @param suId
	 * @return
	 */
	public static SuProductInfo getSuInfo(ProductInfo productInfo, String suId){
		if(StringUtils.isBlank(suId) || productInfo == null
				|| CollectionUtils.isEmpty(productInfo.getSuProductList())){
			return null;
		}

		for(SuProductInfo suInfo : productInfo.getSuProductList()){
			if(suInfo == null || suInfo.getSuId() == null){
				continue;
			}
			if(suId.equals(suInfo.getSuId().toString())){
				return suInfo;
			}
		}
		return null;
	}

}