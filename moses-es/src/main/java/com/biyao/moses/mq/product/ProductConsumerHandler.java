package com.biyao.moses.mq.product;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.biyao.moses.mq.BodyInfo;
import com.biyao.moses.mq.IConsumerHandler;
import com.biyao.moses.mq.InvalidMqMsgException;
import com.biyao.moses.service.ProductEsServiceImpl;
import com.biyao.moses.util.CommonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @program: moses-parent-online
 * @description: 商品基本信息mq处理器
 * @author: changxiaowei
 * @Date: 2021-12-03 15:07
 **/
@Slf4j
@Component("productConsumerHandler")
public class ProductConsumerHandler implements IConsumerHandler {

    @Autowired
    ProductEsServiceImpl productEsService;
    @Override
    public void handle(MessageExt messageExt, JSONObject body) throws InvalidMqMsgException, Exception {
        /**
         {"data":[12313],"sendTime":1640436290551}
         */
        //解析body中的信息
        log.info("mq消费消息开始,mqbody:{}",JSONObject.toJSONString(body));
        JSONArray data = body.getJSONArray("data");
        // 更新es中本批商品信息
        productEsService.updateIndexByPids(CommonUtil.jsonArrTOLongList(data));
    }

    @Override
    public BodyInfo getBodyInfo(JSONObject body) {
        Long publishTime = body.getLong("sendTime");
        return new BodyInfo("", publishTime);
    }
}