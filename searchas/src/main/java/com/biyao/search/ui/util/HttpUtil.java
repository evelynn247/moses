package com.biyao.search.ui.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class HttpUtil {
    /**
     * 将字符串按照UTF-8进行URL编码
     * @param s 目标字符串
     * @return 成功返回编码后的字符串，失败返回空字符串
     */
    public static String urlUtf8Encode( String s ) {
        try {
            return URLEncoder.encode(s,"UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return "";
    }
}
