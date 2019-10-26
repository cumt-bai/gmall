package com.cumt.gmall.cart.controller;


import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.cumt.gmall.bean.CartInfo;
import com.cumt.gmall.bean.SkuInfo;
import com.cumt.gmall.config.CookieUtil;
import com.cumt.gmall.service.ManageService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

@Component
public class CartCookieHandler {

    private String cookieCartName = "cart";

    private int COOKIE_CART_MAXAGE = 7*24*3600;

    @Reference
    private ManageService manageService;

    /**
     * cookie添加购物车
     * @param request
     * @param response
     * @param skuId
     * @param userId
     * @param skuNum
     */
    public void addToCart(HttpServletRequest request, HttpServletResponse response, String skuId, String userId, int skuNum) {

        // 获取cookie中的数据,即购物车
        String cookieValue = CookieUtil.getCookieValue(request, cookieCartName, true);

        List<CartInfo> cartInfoList = new ArrayList<>();
        boolean isMatch = false;
        if (StringUtils.isNotEmpty(cookieValue)){
            // 格式转换  此时是集合
            cartInfoList = JSON.parseArray(cookieValue, CartInfo.class);
            // 遍历
            for (CartInfo cartInfo : cartInfoList) {
                // 购物车中有该商品
                if (cartInfo.getSkuId().equals(skuId)){

                    cartInfo.setSkuNum(cartInfo.getSkuNum() + skuNum);
                    cartInfo.setSkuPrice(cartInfo.getSkuPrice());
                    isMatch = true;
                    break;
                }
            }
        }
        // 表示第一次添加该商品
        if (!isMatch){

            SkuInfo skuInfo = manageService.getSkuInfo(skuId);
            CartInfo cartInfo = new CartInfo();
            cartInfo.setSkuId(skuInfo.getId());
            cartInfo.setSkuPrice(skuInfo.getPrice());
            cartInfo.setUserId(userId);
            cartInfo.setSkuName(skuInfo.getSkuName());
            cartInfo.setImgUrl(skuInfo.getSkuDefaultImg());
            cartInfo.setCartPrice(skuInfo.getPrice());
            cartInfo.setSkuNum(skuNum);

            //添加进集合中
            cartInfoList.add(cartInfo);

        }

        // 存入cookie
        CookieUtil.setCookie(request,response,cookieCartName,JSON.toJSONString(cartInfoList),COOKIE_CART_MAXAGE,true);

    }

    /**
     * 获取购物车
     * @param request
     * @return
     */
    public List<CartInfo> getCartList(HttpServletRequest request) {

        List<CartInfo> cartInfoList = new ArrayList<>();

        String cookieValue = CookieUtil.getCookieValue(request, cookieCartName, true);

        if (StringUtils.isNotEmpty(cookieValue)){
            cartInfoList = JSON.parseArray(cookieValue, CartInfo.class);
            return cartInfoList;
        }

        return null;

    }

    /**
     * 删除cookie中的购物车数据
     * @param request
     * @param response
     */
    public void deleteCartCookie(HttpServletRequest request, HttpServletResponse response) {

        CookieUtil.deleteCookie(request,response,cookieCartName);
    }

    /**
     * 更改购物车的选中状态
     * @param request
     * @param response
     * @param isChecked
     * @param skuId
     */
    public void checkCart(HttpServletRequest request, HttpServletResponse response, String isChecked, String skuId) {

        List<CartInfo> cartList = getCartList(request);

        if (cartList != null && cartList.size() > 0){
            for (CartInfo cartInfo : cartList) {
                if (cartInfo.getSkuId().equals(skuId)) {
                    cartInfo.setIsChecked(isChecked);
                    break;
                }
            }
        }

        CookieUtil.setCookie(request,response,cookieCartName,JSON.toJSONString(cartList),COOKIE_CART_MAXAGE,true);

    }
}
















