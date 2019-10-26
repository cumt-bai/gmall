package com.cumt.gmall.service;


import com.cumt.gmall.bean.UserAddress;
import com.cumt.gmall.bean.UserInfo;

import java.util.List;

public interface UserInfoService {

    List<UserInfo> findAll();

    /**
     * 根据用户名查询
     * @param userInfo
     * @return
     */
    List<UserInfo> findUserByName(UserInfo userInfo);

    List<UserAddress> getUserAddressByUserId(String userId);

    /**
     * 登陆
     * @param userInfo
     * @return
     */
    UserInfo login(UserInfo userInfo);

    /**
     * 根据id获取用户
     * @param userId
     * @return
     */
    UserInfo verify(String userId);
}
