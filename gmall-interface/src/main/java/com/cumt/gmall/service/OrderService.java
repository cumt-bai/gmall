package com.cumt.gmall.service;

import com.cumt.gmall.bean.OrderInfo;
import com.cumt.gmall.bean.enums.ProcessStatus;

public interface OrderService {

    /**
     * 保存订单，返回orderId供支付使用
     * @param orderInfo
     * @return
     */
    String saveOrder(OrderInfo orderInfo);

    /**
     * 生成流水号，防止表单重复提交。
     * @param userId
     * @return
     */
    String getOutTradeNo(String userId);

    /**
     * 验证流水号，即表单一份，redis一份比较
     * @param tradeCode
     * @param userId
     * @return
     */
    boolean checkTradeNo(String tradeCode,String userId);

    /**
     * 删除流水号，第一次提交成功的时候删除
     * @param userId
     */
    void delOutTradeNo(String userId);

    /**
     * 验库存
     * @param skuId
     * @param skuNum
     * @return
     */
    boolean checkStock(String skuId, Integer skuNum);

    /**
     * 获取订单信息
     * @param orderId
     * @return
     */
    OrderInfo getOrderInfo(String orderId);

    /**
     * 修改订单状态
     * @param orderId
     * @param processStatus
     */
    void updateOrderStatus(String orderId, ProcessStatus processStatus);

    /**
     * 发送消息通知减库存
     * @param orderId
     */
    void sendOrderStatus(String orderId);
}
