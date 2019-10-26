package com.cumt.gmall.service;

import com.cumt.gmall.bean.PaymentInfo;

import java.util.Map;

public interface PaymentService {


    /**
     * 保存支付信息
     * @param paymentInfo
     */
    void savePaymentInfo(PaymentInfo paymentInfo);

    /**
     * 获取paymentInfo信息
     * @param outTradeNo
     * @return
     */
    PaymentInfo getPaymentInfo(String outTradeNo);

    /**
     * 更新支付状态信息
     * @param outTradeNo
     * @param paymentInfoUPD
     */
    void updatePaymentInfo(String outTradeNo, PaymentInfo paymentInfoUPD);

    /**
     * 退款
     * @param orderId
     * @return
     */
    boolean refund(String orderId);

    /**
     * 生成微信支付的map集合
     * @param orderId
     * @param totalAmount
     * @return
     */
    Map createNative(String orderId, String totalAmount);

    /**
     * 发送消息给订单模块
     * @param paymentInfo
     * @param result
     */
    void sendPaymentResult(PaymentInfo paymentInfo, String result);


    /**
     * 向支付宝查询支付状态
     * @param paymentInfoQuery
     * @return
     */
    boolean checkPayment(PaymentInfo paymentInfoQuery);

    /**
     * 查询
     * @param paymentInfo
     * @return
     */
    PaymentInfo getPaymentInfo(PaymentInfo paymentInfo);


    /**
     * 生成二维码的时候发送延迟队列向支付宝查询支付状态
     * @param outTradeNo 第三方交易编号
     * @param delaySec  延迟时间
     * @param checkCount 检查次数
     */
    void sendDelayPaymentResult(String outTradeNo,int delaySec ,int checkCount);
}
