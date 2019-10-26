package com.cumt.gmall.order.controller;


import com.alibaba.dubbo.config.annotation.Reference;
import com.cumt.gmall.bean.*;
import com.cumt.gmall.config.LoginRequire;
import com.cumt.gmall.service.CartService;
import com.cumt.gmall.service.ManageService;
import com.cumt.gmall.service.OrderService;
import com.cumt.gmall.service.UserInfoService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

@Controller
public class OrderController {

    @Reference
    private UserInfoService userInfoService;

    @Reference
    private CartService cartService;

    @Reference
    private OrderService orderService;

    @Reference
    private ManageService manageService;

    @RequestMapping("trade")
    @LoginRequire
    public String trade(HttpServletRequest request){

        String userId = (String) request.getAttribute("userId");
        List<UserAddress> userAddressList = userInfoService.getUserAddressByUserId(userId);
        request.setAttribute("userAddressList",userAddressList);

        List<OrderDetail> orderDetailList = new ArrayList<>();
        // 获取清单，即购物车被选中的商品
        List<CartInfo> cartCheckedList  = cartService.getCartCheckedList(userId);
        for (CartInfo cartInfo : cartCheckedList) {
            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setImgUrl(cartInfo.getImgUrl());
            orderDetail.setOrderPrice(cartInfo.getSkuPrice());
            orderDetail.setSkuId(cartInfo.getSkuId());
            orderDetail.setSkuName(cartInfo.getSkuName());
            orderDetail.setSkuNum(cartInfo.getSkuNum());

            orderDetailList.add(orderDetail);
        }
        request.setAttribute("orderDetailList",orderDetailList);

        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderDetailList(orderDetailList);
        //计算总金额
        orderInfo.sumTotalAmount();
        request.setAttribute("totalAmount",orderInfo.getTotalAmount());

        //生成订单的时候生成流水号，存入页面供提交时比对，防止表单重复提交
        String outTradeNo = orderService.getOutTradeNo(userId);
        request.setAttribute("outTradeNo",outTradeNo);

        return "trade";
    }

    //http://trade.gmall.com/submitOrder
    @RequestMapping("submitOrder")
    @LoginRequire
    public String submitOrder(HttpServletRequest request,OrderInfo orderInfo){

        String userId = (String) request.getAttribute("userId");
        orderInfo.setUserId(userId);

        String tradeNo = request.getParameter("tradeNo");
        // 比较流水号
        boolean result = orderService.checkTradeNo(tradeNo, userId);
        if (!result){
            request.setAttribute("errMsg","请勿重复提交订单！");
            return "tradeFail";
        }
        // 若比对一致，则表示第一次提交，再删除流水号，使下次比对不成功，无法提交
        orderService.delOutTradeNo(userId);

        //验库存
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            boolean res = orderService.checkStock(orderDetail.getSkuId(), orderDetail.getSkuNum());
            if (!res){
                request.setAttribute("errMsg",orderDetail.getSkuName() +"库存不足！");
                return "tradeFail";
            }

            //再次检验价格
            SkuInfo skuInfo = manageService.getSkuInfo(orderDetail.getSkuId());
            int i = skuInfo.getPrice().compareTo(orderDetail.getOrderPrice());
            if (i != 0){
                request.setAttribute("errMsg",orderDetail.getSkuName() +"价格有变动！");
                // 查询最新价格
                // cartService.loadCartCache(userId);
                return "tradeFail";
            }

        }

        String orderId = orderService.saveOrder(orderInfo);
        //重定向
        return "redirect://payment.gmall.com/index?orderId="+orderId;
    }

}
