//package com.biyao.moses.config;
//
//import org.apache.hadoop.fs.FileSystem;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.context.annotation.PropertySource;
//
//import java.net.URI;
//
///**
// * @Description hdfs配置
// * @author yangyuliang
// */
//@Configuration
//@PropertySource("classpath:conf/fileconf.properties")
//public class HDFSConfig {
//    private static final Logger log = LoggerFactory.getLogger(HDFSConfig.class);
//
//    @Value("${hadoop_fsUri}")
//    private String path;
//
//    @Value("${hadoop.username}")
//    private String username;
//
//    @Value("${dfs.nameservices}")
//    private String nameServices;
//
//    @Value("${dfs.ha.namenodes.nn}")
//    private String nameNodes;
//
//    @Value("${dfs.namenode.nn1.rpc-address}")
//    private String nn1RpcAddress;
//
//    @Value("${dfs.namenode.nn2.rpc-address}")
//    private String nn2RpcAddress;
//
//    @Value("${dfs.client.failover.proxy.provider.nn}")
//    private String methodName;
//
//
//    @Bean(name = "fileSystem")
//    public FileSystem fileSystem() throws Exception {
//        org.apache.hadoop.conf.Configuration conf = new org.apache.hadoop.conf.Configuration();
//        String[] split = nameNodes.split(",");
//        if (split.length !=2){
//            String errMessage = "prop/fileconf.properties配置文件有误，请检查dfs.ha.namenodes.nn";
//            log.error(errMessage);
//            throw new Exception(errMessage);
//        }
//        conf.set("fs.defaultFS",path);
//        conf.set("fs.defaultFS", "hdfs://" + nameServices);
//        conf.set("dfs.nameservices",nameServices);
//        conf.set("dfs.ha.namenodes." + nameServices,nameNodes);
//        conf.set("dfs.namenode.rpc-address." + nameServices + "." + split[0], nn1RpcAddress);
//        conf.set("dfs.namenode.rpc-address." + nameServices + "." + split[1], nn2RpcAddress);
//        conf.set("dfs.client.failover.proxy.provider." + nameServices,methodName);
//        FileSystem fileSystem = FileSystem.get(new URI(path), conf, username);
//        return fileSystem;
//    }
//
//}