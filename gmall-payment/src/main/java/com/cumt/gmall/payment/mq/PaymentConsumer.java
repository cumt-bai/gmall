package com.cumt.gmall.payment.mq;


import com.alibaba.dubbo.config.annotation.Reference;
import com.cumt.gmall.bean.PaymentInfo;
import com.cumt.gmall.service.PaymentService;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.MapMessage;

@Component
public class PaymentConsumer {

    @Reference
    private PaymentService paymentService;

    @JmsListener(destination = "PAYMENT_RESULT_CHECK_QUEUE",containerFactory = "jmsQueueListener")
    public void consumerPaymentResult(MapMessage mapMessage){

        try {
            String outTradeNo = mapMessage.getString("outTradeNo");
            int delaySec = mapMessage.getInt("delaySec");
            int checkCount = mapMessage.getInt("checkCount");
            PaymentInfo paymentInfoQuery = paymentService.getPaymentInfo(outTradeNo);
            // 支付宝检查payment
            boolean flag = paymentService.checkPayment(paymentInfoQuery);
            System.out.println("支付结果" + flag);
            if (!flag && checkCount!=0){
                System.out.println("次数" + checkCount);
                // 继续查询
                paymentService.sendDelayPaymentResult(outTradeNo,delaySec,checkCount -1);

            }

        } catch (JMSException e) {
            e.printStackTrace();
        }

    }

}
