package com.biyao.search.ui.util;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.alibaba.dubbo.rpc.RpcContext;
import com.google.common.base.Splitter;

public class IPUtil {
    public static String getRemoteIp() {
        if (RpcContext.getContext().getRequest() != null
                && RpcContext.getContext().getRequest() instanceof HttpServletRequest) {

            HttpServletRequest request = (HttpServletRequest) RpcContext.getContext().getRequest();
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor == null) {
                return "";
            }

            List<String> ips = Splitter.on(',').splitToList(xForwardedFor);
            if (ips.size() > 0) {
                return ips.get(0).trim();
            } else {
                return "";
            }
        }
        return "";
    }
}
