package com.biyao.search.ui.util;

import com.biyao.dclog.producer.service.DclogProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author zhaiweixi@idstaff.com
 * @date 2019/12/2
 **/
public class DcLogUtil {

    private static Logger logger = LoggerFactory.getLogger(DcLogUtil.class);

    private static DclogProducer uiResponseDcLogProducer = DclogProducer.getLogger("searchui_response");

    private static DclogProducer searchDetailLog = DclogProducer.getLogger("search_detail_log");

    /**
     * 打印searchui_response
     */
    public static void printUIResponse(String logBody){
        try {
            uiResponseDcLogProducer.printDCLog(logBody);
        }catch (Exception e){
            logger.error("[严重异常]发送dcLog消息失败，topic=[searchui_response], msg={}", logBody, e);
        }
    }

    /**
     * search_detail_log日志
     */
    public static void printSearchDetailLog(String title,String uuid,String sid ,String logBody){
        try {
            StringBuilder sb = new StringBuilder(10240);
            sb.append("title=").append(title);
            sb.append("\tuuid=").append(uuid);
            if(sid == null){
                sb.append("\tsid=");
            }else{
                sb.append("\tsid=").append(sid);
            }
            sb.append("\tdetail=").append(logBody);
            searchDetailLog.printDCLog(sb.toString());
        }catch (Exception e){
            logger.error("[严重异常]发送dcLog消息失败，topic=[search_detail_log], msg={}", logBody, e);
        }
    }
}
