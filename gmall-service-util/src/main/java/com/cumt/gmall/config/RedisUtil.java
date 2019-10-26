package com.cumt.gmall.config;


import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;


public class RedisUtil {

    private JedisPool jedisPool;

    //初始化连接池
    public void initJedisPool(String host,int port,int timeOut,int database){

        // 构建连接池配置信息
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        //获取到jedis后自检
        jedisPoolConfig.setTestOnBorrow(true);
        //设置阻塞队列
        jedisPoolConfig.setBlockWhenExhausted(true);
        //设置剩余数
        jedisPoolConfig.setMinIdle(10);
        //设置最大数  根据访问量和机器配置
        jedisPoolConfig.setMaxTotal(200);
        //设置等待时间
        jedisPoolConfig.setMaxWaitMillis(10*1000);

        jedisPool = new JedisPool(jedisPoolConfig,host,port,timeOut);
    }

    public Jedis getJedis(){
        return jedisPool.getResource();
    }



}
