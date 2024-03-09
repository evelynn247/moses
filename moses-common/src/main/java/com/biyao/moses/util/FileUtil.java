package com.biyao.moses.util;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author zhaiweixi@idstaff.com
 * @date 2019/10/12
 **/
@Slf4j
public class FileUtil {


    public static boolean download(String urlStr, String fileName) {
        File file = new File(fileName + ".tmp");

        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                log.error("下载文件时创建本地文件失败！！url:{}, fileName:{}", urlStr, fileName);
                log.error("下载文件时创建本地文件失败！！", e);
                return false;
            }
        }
        FileOutputStream fos = null;
        BufferedInputStream bis = null;
        HttpURLConnection httpUrl = null;
        URL url = null;
        byte[] buf = new byte[10240];

        try {
            url = new URL(urlStr);
            httpUrl = (HttpURLConnection) url.openConnection();
            httpUrl.connect();
            bis = new BufferedInputStream(httpUrl.getInputStream());
            fos = new FileOutputStream(file);

            int size1;
            while ((size1 = bis.read(buf)) != -1) {
                fos.write(buf, 0, size1);
            }

            fos.close();
            bis.close();
            httpUrl.disconnect();

            log.info("*********远程下载文件正常 {} *********", fileName);

            // file.renameTo(new File(fileName));
            FileWriter fw = new FileWriter(fileName, false);
            FileReader fr = new FileReader(file);
            char[] reader = new char[1024];
            int num = 0;
            while ((num = fr.read(reader)) != -1) {
                fw.write(reader, 0, num);
            }
            fw.flush();
            fw.close();
            fr.close();
            return true;
        } catch (Exception e) {
            log.error("下载文件时发生异常！！url:{}, fileName:{}", urlStr, fileName);
            log.error("下载文件时发生异常！！", e);
            return false;
        } finally {
            if (file.isFile() && file.exists()) {
                file.delete();
            }
        }
    }

    /**
     * 从本地文件读取配置
     * @param filePath
     * @return
     */
    public static List<String> getFileFromLocal(String filePath, boolean isDeleteFile) throws Exception {
        List<String> lines = new ArrayList<>();

        File file = new File( filePath );

        try {
            lines = Files.readLines( file, Charsets.UTF_8 );
        } catch (IOException e) {
            throw new Exception("从本地文件读取配置出错", e);
        }finally {
            if(isDeleteFile){
                file.delete();
            }
        }

        return lines;
    }

    /**
     * 从网络读取文件
     * @param destUrl
     * @return
     */
    public static List<String> getRemoteFile(String destUrl) throws Exception {
        List<String> lines = new ArrayList<>();
        File tmpDir = Files.createTempDir();
        String fileName = tmpDir + File.separator + String.valueOf( Math.random() ) + (new Date()).getTime() + ".bak";
        FileOutputStream fos = null;
        BufferedInputStream bis = null;
        HttpURLConnection httpUrl = null;
        URL url = null;
        byte[] buf = new byte[10240];
        int size = 0;
        try {
            // 建立链接
            url = new URL( destUrl );
            httpUrl = (HttpURLConnection) url.openConnection();
            // 连接指定的资源
            httpUrl.connect();
            // 获取网络输入流
            bis = new BufferedInputStream( httpUrl.getInputStream() );
            // 建立文件
            fos = new FileOutputStream( fileName );

            // 保存文件
            while ((size = bis.read( buf )) != -1) {
                fos.write( buf, 0, size );
            }
            fos.close();
            bis.close();
            httpUrl.disconnect();

            lines = getFileFromLocal(fileName, true);
        } catch (Exception e) {
            throw new Exception("读取网络文件出错", e );
        }finally {
            tmpDir.delete();
        }

        return lines;
    }

}
