package com.biyao.search.ui.config;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;


/**
 * 本地配置
 * @author: guochong
 * @date: 2017-09-18
 */
public class LocalConfig {
    
    public String rankServerIp;
    public Integer rankServerPort;
    
    private static LocalConfig instance = null;
  
    public static LocalConfig getInstance() {
        if(instance == null) {
            synchronized (LocalConfig.class) {
                if(instance == null) {
                    instance = new LocalConfig();
                }
            }
        }
        return instance;
    }
    
    private LocalConfig()  {
        
        String configPath = this.getClass().getResource("/") + "search-ui.properties";
        configPath = configPath.replace("file:", "");
        Configurations configs = new Configurations();
        try {
            Configuration config = configs.properties( configPath );
            rankServerIp     = config.getString("rankserver.ip");
            rankServerPort   = config.getInt("rankserver.port");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getRankServerIp() {
        return rankServerIp;
    }

    public Integer getRankServerPort() {
        return rankServerPort;
    }
}
