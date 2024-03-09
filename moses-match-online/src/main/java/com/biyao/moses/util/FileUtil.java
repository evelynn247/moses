package com.biyao.moses.util;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * @program: moses-parent-online
 * @description: 文件类工具类
 * @author: changxiaowei
 * @Date: 2021-12-24 20:28
 **/
@Slf4j
public class FileUtil {
    /**
     * 从网络读取文件
     * @param
     * @return
     */
    public static  void  getRemoteFile(String filePath,String savePath) throws Exception {
        FileOutputStream fos = null;
        BufferedInputStream bis = null;
        HttpURLConnection httpUrl = null;
        URL url = null;
        byte[] buf = new byte[10240];
        int size = 0;
        try {
            // 建立链接
            url = new URL(filePath);
            httpUrl = (HttpURLConnection) url.openConnection();
            // 连接指定的资源
            httpUrl.connect();
            // 获取网络输入流
            bis = new BufferedInputStream( httpUrl.getInputStream() );
            isExistDir(savePath);
            // 建立文件
            fos = new FileOutputStream( savePath );
            // 保存文件
            while ((size = bis.read( buf )) != -1) {
                fos.write( buf, 0, size );
            }
            fos.close();
            bis.close();
            httpUrl.disconnect();
        } catch (Exception e) {
            throw new Exception("读取网络文件出错", e );
        }
    }


    /**
     * 判断多级路径是否存在，不存在就创建
     * @param filePath 支持带文件名的Path：如：D:\news\2014\12\abc.text，和不带文件名的Path：如：D:\news\2014\12
     */
    public static void isExistDir(String filePath) {
        String paths[] = {""};
        //切割路径
        try {
            String tempPath = new File(filePath).getCanonicalPath();//File对象转换为标准路径并进行切割，有两种windows和linux
            paths = tempPath.split("\\\\");//windows
            if(paths.length==1){paths = tempPath.split("/");}//linux
        } catch (IOException e) {
            log.error("[严重异常]创建多级文件时切割路径错误",e);
        }
        //判断是否有后缀
        boolean hasType = false;
        if(paths.length>0){
            String tempPath = paths[paths.length-1];
            if(tempPath.length()>0){
                if(tempPath.indexOf(".")>0){
                    hasType=true;
                }
            }
        }
        //创建文件夹
        String dir = paths[0];
        for (int i = 0; i < paths.length - (hasType?2:1); i++) {// 注意此处循环的长度，有后缀的就是文件路径，没有则文件夹路径
            try {
                dir = dir + "/" + paths[i + 1];//采用linux下的标准写法进行拼接，由于windows可以识别这样的路径，所以这里采用警容的写法
                File dirFile = new File(dir);
                if (!dirFile.exists()) {
                    dirFile.mkdir();
                    log.info("[操作日志]成功创建目录", dirFile.getCanonicalFile());
                }
            } catch (Exception e) {
                log.error("[严重异常]文件夹创建发生异常",e);
            }
        }
    }
}
