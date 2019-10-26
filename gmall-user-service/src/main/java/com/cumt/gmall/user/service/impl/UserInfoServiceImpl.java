package com.cumt.gmall.user.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.cumt.gmall.bean.UserAddress;
import com.cumt.gmall.bean.UserInfo;
import com.cumt.gmall.config.RedisUtil;
import com.cumt.gmall.service.UserInfoService;
import com.cumt.gmall.user.mapper.UserAddressMapper;
import com.cumt.gmall.user.mapper.UserInfoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import redis.clients.jedis.Jedis;

import java.util.List;

@Service
public class UserInfoServiceImpl implements UserInfoService {

    @Autowired
    private UserInfoMapper userInfoMapper;

    @Autowired
    private UserAddressMapper userAddressMapper;

    @Autowired
    private RedisUtil redisUtil;

    public String userKey_prefix="user:";
    public String userinfoKey_suffix=":info";
    public int userKey_timeOut=60*60*24;

    @Override
    public List<UserInfo> findAll() {
        return userInfoMapper.selectAll();
    }

    @Override
    public List<UserInfo> findUserByName(UserInfo userInfo) {
        return userInfoMapper.select(userInfo);
    }

    @Override
    public List<UserAddress> getUserAddressByUserId(String userId) {

        UserAddress userAddress = new UserAddress();
        userAddress.setUserId(userId);
        return userAddressMapper.select(userAddress);
    }

    @Override
    public UserInfo login(UserInfo userInfo) {

        String password = DigestUtils.md5DigestAsHex(userInfo.getPasswd().getBytes());
        userInfo.setPasswd(password);

        UserInfo user = userInfoMapper.selectOne(userInfo);

        // 放入缓存中
        if(user != null){

            Jedis jedis = redisUtil.getJedis();
            String userKey = userKey_prefix + user.getId() + userinfoKey_suffix;
            jedis.setex(userKey,userKey_timeOut, JSON.toJSONString(user));
            jedis.close();
        }

        return user;
    }


    @Override
    public UserInfo verify(String userId) {

        String userKey = userKey_prefix + userId + userinfoKey_suffix;

        Jedis jedis = redisUtil.getJedis();
        String userInfoJson = jedis.get(userKey);
        if(userInfoJson != null){
            jedis.expire(userKey,userKey_timeOut);
            UserInfo userInfo = JSON.parseObject(userInfoJson, UserInfo.class);
            jedis.close();
            return userInfo;
        }

        return null;
    }
}
