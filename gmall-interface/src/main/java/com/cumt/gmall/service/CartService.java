package com.cumt.gmall.service;


import com.cumt.gmall.bean.CartInfo;

import java.util.List;

public interface CartService {

    /**
     * 添加购物车
     * @param skuId
     * @param userId
     * @param skuNum
     */
    void  addToCart(String skuId,String userId,Integer skuNum);

    /**
     * 购物车详情
     * @return
     */
    List<CartInfo> getCartList(String userId);

    /**
     * 合并购物车
     * @param cartListCK
     * @param userId
     * @return
     */
    List<CartInfo> mergeToCartList(List<CartInfo> cartListCK, String userId);

    /**
     * 更改购物车选中状态
     * @param userId
     * @param isChecked
     * @param skuId
     */
    void checkCart(String userId, String isChecked, String skuId);

    /**
     * 添加购物车(未登陆存入redis)
     * @param skuId
     * @param uuid
     * @param skuNum
     */
    void addToCartRedis(String skuId, String uuid, int skuNum);

    /**
     * 获取购物车被选中的商品
     * @param userId
     * @return
     */
    List<CartInfo> getCartCheckedList(String userId);
}
