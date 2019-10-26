package com.cumt.gmall.payment.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.cumt.gmall.bean.OrderInfo;
import com.cumt.gmall.bean.PaymentInfo;
import com.cumt.gmall.bean.enums.PaymentStatus;
import com.cumt.gmall.config.LoginRequire;
import com.cumt.gmall.payment.config.AlipayConfig;
import com.cumt.gmall.service.OrderService;
import com.cumt.gmall.service.PaymentService;
import groovy.time.BaseDuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.alipay.api.AlipayConstants.SIGN_TYPE;
import static org.apache.catalina.manager.Constants.CHARSET;

@Controller
public class PaymentController {

    @Reference
    private OrderService orderService;

    @Reference
    private PaymentService paymentService;

    @Autowired
    private AlipayClient alipayClient;

    @RequestMapping("index")
    @LoginRequire
    public String index(HttpServletRequest request){

        String orderId = request.getParameter("orderId");

        OrderInfo orderInfo = orderService.getOrderInfo(orderId);
        BigDecimal totalAmount = orderInfo.getTotalAmount();
        request.setAttribute("orderId",orderId);
        request.setAttribute("totalAmount",totalAmount);
        return "index";
    }

    @RequestMapping("/alipay/submit")
    @ResponseBody
    public String submitPayment(HttpServletRequest request, HttpServletResponse response){

        //获取订单编号
        String orderId = request.getParameter("orderId");
        //1、 保存交易记录
        OrderInfo orderInfo = orderService.getOrderInfo(orderId);
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setCreateTime(new Date());
        paymentInfo.setOrderId(orderId);
        paymentInfo.setOutTradeNo(orderInfo.getOutTradeNo());
        paymentInfo.setPaymentStatus(PaymentStatus.UNPAID);
        paymentInfo.setSubject("买手机");
        paymentInfo.setTotalAmount(orderInfo.getTotalAmount());

        paymentService.savePaymentInfo(paymentInfo);

        //2、 生成二维码
        // 将AlipayClient做成工具类 注入spring容器直接调用
        //AlipayClient alipayClient = new DefaultAlipayClient("https://openapi.alipay.com/gateway.do", APP_ID, APP_PRIVATE_KEY, FORMAT, CHARSET, ALIPAY_PUBLIC_KEY, SIGN_TYPE); //获得初始化的AlipayClient
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();//创建API对应的request
        // 设置同步
        // alipayRequest.setReturnUrl("http://domain.com/CallBack/return_url.jsp");
        alipayRequest.setReturnUrl(AlipayConfig.return_payment_url);
        // 设置异步
        alipayRequest.setNotifyUrl("http://domain.com/CallBack/notify_url.jsp");//在公共参数中设置回跳和通知地址
        // 设置参数
        Map<String,Object> map = new HashMap<>();
        map.put("out_trade_no",paymentInfo.getOutTradeNo());
        map.put("product_code","FAST_INSTANT_TRADE_PAY");
        map.put("total_amount",paymentInfo.getTotalAmount());
        map.put("subject",paymentInfo.getSubject());
        alipayRequest.setBizContent(JSON.toJSONString(map));
        /*alipayRequest.setBizContent("{" +
                "    \"out_trade_no\":\"20150320010101001\"," +
                "    \"product_code\":\"FAST_INSTANT_TRADE_PAY\"," +
                "    \"total_amount\":88.88," +
                "    \"subject\":\"Iphone6 16G\"," +
                "    \"body\":\"Iphone6 16G\"," +
                "    \"passback_params\":\"merchantBizType%3d3C%26merchantBizNo%3d2016010101111\"," +
                "    \"extend_params\":{" +
                "    \"sys_service_provider_id\":\"2088511833207846\"" +
                "    }"+
                "  }");//填充业务参数*/
        String form="";
        try {
            form = alipayClient.pageExecute(alipayRequest).getBody(); //调用SDK生成表单
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        response.setContentType("text/html;charset=" + CHARSET);
        //response.getWriter().write(form);//直接将完整的表单html输出到页面
        //response.getWriter().flush();
        //response.getWriter().close();
        //System.out.println(form);

        // 生成二维码的时候发送延迟队列
        paymentService.sendDelayPaymentResult(paymentInfo.getOutTradeNo(),15,3);

        return form;
    }

    // 同步回调  给商家看的返回页面
    //http://payment.gmall.com/alipay/callback/return
    @RequestMapping("/alipay/callback/return")
    public String callbackReturn(){

        return "redirect:"+AlipayConfig.return_order_url;
    }

    // 异步回调
    //http://60.205.215.91/alipay/callback/notify
    @RequestMapping("/callback/notify")
    @ResponseBody
    public String paymentNotify(@RequestParam Map<String,String> paramMap, HttpServletResponse response) throws AlipayApiException {
        // Map<String, String> paramsMap = ... //将异步通知中收到的所有参数都存放到 map 中
        boolean sign = AlipaySignature.rsaCheckV1(paramMap, AlipayConfig.alipay_public_key, CHARSET, SIGN_TYPE); //调用SDK验证签名

        // 获取交易通知状态
        String tradeStatus = paramMap.get("trade_status");
        // 获取第三方交易编号 查询paymentInfo表中的支付状态
        String outTradeNo = paramMap.get("out_trade_no");

        PaymentInfo paymentInfo = paymentService.getPaymentInfo(outTradeNo);

        if(sign){
            // TODO 验签成功后，按照支付结果异步通知中的描述，对支付结果中的业务内容进行二次校验，校验成功后在response中返回success并继续商户自身业务处理，校验失败返回failure

            if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)){

                if(paymentInfo.getPaymentStatus() == PaymentStatus.PAID || paymentInfo.getPaymentStatus() == PaymentStatus.ClOSED){

                    paymentService.sendPaymentResult(paymentInfo,"failure");
                    return "failure";
                }

                PaymentInfo paymentInfoUPD = new PaymentInfo();
                paymentInfoUPD.setPaymentStatus(PaymentStatus.PAID);
                paymentInfoUPD.setCallbackTime(new Date());
                //更新payment状态
                paymentService.updatePaymentInfo(outTradeNo,paymentInfoUPD);

                //通过消息队列发送消息给订单模块
                paymentService.sendPaymentResult(paymentInfo,"success");

                return "success";
            }
            return "failure";
        }else{
            // TODO 验签失败则记录异常日志，并在response中返回failure.
            paymentService.sendPaymentResult(paymentInfo,"failure");

            return "failure";
        }

    }

    @RequestMapping("refund")
    @ResponseBody
    public String refund(String orderId){

        boolean result = paymentService.refund(orderId);
        return ""+result;
    }


    @RequestMapping("wx/submit")
    public Map createNative(String orderId){

        //OrderInfo orderInfo = orderService.getOrderInfo(orderId);
        //BigDecimal totalAmount = orderInfo.getTotalAmount();  参数为总金额
        Map map = paymentService.createNative(orderId,"1"); // 测试数据一分钱
        System.out.println(map.get("code_url=" + map.get("code_url")));
        return map;
    }

    // 测试消息队列
    @RequestMapping("sendPaymentResult")
    @ResponseBody
    public String sendPaymentResult(PaymentInfo paymentInfo, String result){
        paymentService.sendPaymentResult(paymentInfo,result);
        return "ok";
    }

    // 测试查询支付状态
    @RequestMapping("checkPayment")
    @ResponseBody
    public String checkPayment(String orderId){

        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOrderId(orderId);
        PaymentInfo paymentInfoQuery = paymentService.getPaymentInfo(paymentInfo);
        boolean res = paymentService.checkPayment(paymentInfoQuery);

        return "" + res;
    }



}
