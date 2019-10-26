package com.cumt.gmall.order.mq;

import com.alibaba.dubbo.config.annotation.Reference;
import com.cumt.gmall.bean.enums.ProcessStatus;
import com.cumt.gmall.service.OrderService;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.MapMessage;

@Component
public class OrderConsumer {

    @Reference
    private OrderService orderService;

    @JmsListener(destination = "PAYMENT_RESULT_QUEUE",containerFactory = "jmsQueueListener")
    public void consumerPaymentResult(MapMessage mapMessage){

        try {
            String orderId = mapMessage.getString("orderId");
            String result = mapMessage.getString("result");

            // 表示支付成功，修改订单状态
            if("success".equals(result)){
                orderService.updateOrderStatus(orderId, ProcessStatus.PAID);

                // 然后发送消息给库存,通知减库存
                orderService.sendOrderStatus(orderId);
                orderService.updateOrderStatus(orderId, ProcessStatus.NOTIFIED_WARE);
            }

        } catch (JMSException e) {
            e.printStackTrace();
        }

    }

    @JmsListener(destination = "SKU_DEDUCT_QUEUE",containerFactory = "jmsQueueListener")
    public void consumeSkuDeduct(MapMessage mapMessage){

        try {
            String orderId = mapMessage.getString("orderId");
            String status = mapMessage.getString("status");

            if("DEDUCTED".equals(status)){
                orderService.updateOrderStatus(orderId, ProcessStatus.DELEVERED);
            } else {
                orderService.updateOrderStatus(orderId, ProcessStatus.STOCK_EXCEPTION);
            }


        } catch (JMSException e) {
            e.printStackTrace();
        }

    }



}
