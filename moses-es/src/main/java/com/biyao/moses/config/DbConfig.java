package com.biyao.moses.config;

import com.alibaba.druid.pool.DruidDataSource;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.mapper.MapperScannerConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

/**
 * @program: moses-parent-online
 * @description:
 * @author: changxiaowei
 * @Date: 2022-04-20 19:02
 **/
@Configuration
public class DbConfig {

    @Bean(name = "dataSource")
    public DruidDataSource getDruidDataSource(){
        DruidDataSource dataSource = null;
        try {
            dataSource =new DruidDataSource();
            dataSource.setDriverClassName("com.mysql.jdbc.Driver");
            dataSource.setUrl("jdbc:mysql://mysql.biyao.com:3306/by_pdc?characterEncoding=UTF-8&allowMultiQueries=true");
            dataSource.setUsername("by_test");
            dataSource.setPassword("0-dev..Com");
            dataSource.setInitialSize(5);
            dataSource.setMinIdle(5);
            dataSource.setMaxActive(300);
            dataSource.setMaxWait(60000);
            dataSource.setTimeBetweenEvictionRunsMillis(60000);
            dataSource.setMinEvictableIdleTimeMillis(300000);
            dataSource.setTestWhileIdle(true);
            dataSource.setValidationQuery("select 1");
            dataSource.setTestOnBorrow(false);
            dataSource.setTestOnReturn(false);
            dataSource.setPoolPreparedStatements(true);
            dataSource.setMaxPoolPreparedStatementPerConnectionSize(20);
            dataSource.setFilters("stat");
            dataSource.setRemoveAbandoned(true);
            dataSource.setLogAbandoned(true);
            dataSource.setRemoveAbandonedTimeout(1800);
        }catch (Exception e){

        }
        return dataSource;
    }

    @Bean(name = "mysqlSessionFactory")
    public SqlSessionFactoryBean get() throws Exception{
        SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
        sqlSessionFactoryBean.setDataSource(getDruidDataSource());
        sqlSessionFactoryBean.setMapperLocations(
                new PathMatchingResourcePatternResolver().getResources("classpath:mybatis/*.xml"));
        return sqlSessionFactoryBean;
    }


    @Bean
    public MapperScannerConfigurer get1() throws Exception{
        MapperScannerConfigurer mapperScannerConfigurer = new MapperScannerConfigurer();
        mapperScannerConfigurer.setSqlSessionFactoryBeanName("mysqlSessionFactory");
        mapperScannerConfigurer.setBasePackage("com.biyao.moses.pdc.mapper");
        return mapperScannerConfigurer;
    }
    @Bean
    public DataSourceTransactionManager get2() throws Exception{
        DataSourceTransactionManager dataSourceTransactionManager = new DataSourceTransactionManager();
        dataSourceTransactionManager.setDataSource(getDruidDataSource());
        return dataSourceTransactionManager;
    }
}
