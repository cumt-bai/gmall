package com.cumt.gmall.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

//beans.xml   RedisConfig使其变成xml
@Configuration
public class RedisConfig {

    // :disabled 表示如果配置文件中没有获取到host ，则表示默认值disabled
    @Value("${spring.redis.host:disabled}")
    private String host;

    @Value("${spring.redis.port:0}")
    private int port;

    @Value("${spring.redis.database:0}")
    private int database;

    @Value("${spring.redis.timeout:10000}")
    private int timeOut;

    @Bean
    public RedisUtil getRedisUtil(){
        RedisUtil redisUtil = new RedisUtil();

        //初始化 initJedisPool 获取Jedis
        redisUtil.initJedisPool(host,port,timeOut,database);

        return redisUtil;
    }

}
