package com.cumt.gmall.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.cumt.gmall.bean.CartInfo;
import com.cumt.gmall.bean.SkuInfo;
import com.cumt.gmall.config.LoginRequire;
import com.cumt.gmall.service.CartService;
import com.cumt.gmall.service.ManageService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Controller
public class CartController {

    @Reference
    private CartService cartService;

    @Reference
    private ManageService manageService;

    @Autowired
    private CartCookieHandler cartCookieHandler;

    @RequestMapping("addToCart")
    @LoginRequire(autoRedirect = false)  // 要用到userId，userId在拦截器中，所以要加注解
    public String addToCart(HttpServletRequest request, HttpServletResponse response){

        String userId = (String) request.getAttribute("userId");
        String skuNum = request.getParameter("skuNum");
        String skuId = request.getParameter("skuId");

        if (!StringUtils.isEmpty(userId)){
            // 表示用户登陆时加入购物车
            cartService.addToCart(skuId,userId,Integer.parseInt(skuNum));
        } else {
            // 未登陆时,利用cookie保存购物车信息
            cartCookieHandler.addToCart(request,response,skuId,userId,Integer.parseInt(skuNum));

            // 也可以用redis进行保存购物车信息
            // 用uuid代替userId  先获取cookie中的uuid，若有则表示当前购物车有数据，携带uuid访问获取数据
            /*String uuid = "";
            boolean isHasCookie = false;
            Cookie[] cookies = request.getCookies();
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals("user_key")) {
                    uuid = cookie.getValue();
                    isHasCookie = true;
                }
            }
            // 说明cookie中没有uuid
            if (!isHasCookie){
                uuid = UUID.randomUUID().toString().replace("-","");
                Cookie cookie = new Cookie("user_key",uuid);
                response.addCookie(cookie);
            }

            cartService.addToCartRedis(skuId,uuid,Integer.parseInt(skuNum));*/
        }

        //保存添加的数量
        request.setAttribute("skuNum",skuNum);
        //保存skuInfo信息
        SkuInfo skuInfo = manageService.getSkuInfo(skuId);
        request.setAttribute("skuInfo",skuInfo);

        return "success";
    }


    @RequestMapping("cartList")
    @LoginRequire(autoRedirect = false)
    public String cartList(HttpServletRequest request, HttpServletResponse response){

        String userId = (String) request.getAttribute("userId");

        List<CartInfo> cartInfoList = new ArrayList<>();
        if (userId != null){

            //获取未登陆的购物车信息
            List<CartInfo> cartListCK = cartCookieHandler.getCartList(request);

            if (cartListCK != null && cartListCK.size() >0){
                // 合并购物车
                cartInfoList = cartService.mergeToCartList(cartListCK,userId);

                //删除未登陆的购物车
                cartCookieHandler.deleteCartCookie(request,response);
            } else {
                // 从redis-db中获取
                cartInfoList = cartService.getCartList(userId);
            }

        } else {
            //cookie
            cartInfoList = cartCookieHandler.getCartList(request);
        }

        request.setAttribute("cartInfoList",cartInfoList);
        return "cartList";
    }


    @RequestMapping("checkCart")
    @ResponseBody
    @LoginRequire(autoRedirect = false)
    public void checkCart(HttpServletRequest request, HttpServletResponse response){

        String userId = (String) request.getAttribute("userId");
        String isChecked = request.getParameter("isChecked");
        String skuId = request.getParameter("skuId");

        if (StringUtils.isNotEmpty(userId)){
            // 登陆状态
            cartService.checkCart(userId,isChecked,skuId);

        } else {
            // 未登陆状态
            cartCookieHandler.checkCart(request,response,isChecked,skuId);
        }

    }

    //http://cart.gmall.com/toTrade
    @RequestMapping("toTrade")
    @LoginRequire
    public String toTrade(HttpServletRequest request ,HttpServletResponse response){

        String userId = (String) request.getAttribute("userId");

        // 提交订单勾选状态合并
        List<CartInfo> cartList = cartCookieHandler.getCartList(request);
        if (cartList != null && cartList.size() > 0){

            cartService.mergeToCartList(cartList,userId);
            cartCookieHandler.deleteCartCookie(request,response);

        }

        return "redirect://trade.gmall.com/trade";
    }


}
