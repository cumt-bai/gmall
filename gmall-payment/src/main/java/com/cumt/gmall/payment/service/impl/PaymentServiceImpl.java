package com.cumt.gmall.payment.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.cumt.gmall.bean.OrderInfo;
import com.cumt.gmall.bean.PaymentInfo;
import com.cumt.gmall.bean.enums.PaymentStatus;
import com.cumt.gmall.config.ActiveMQUtil;
import com.cumt.gmall.payment.mapper.PaymentInfoMapper;
import com.cumt.gmall.service.OrderService;
import com.cumt.gmall.service.PaymentService;
import com.cumt.gmall.util.HttpClient;
import com.github.wxpay.sdk.WXPayUtil;
import org.apache.activemq.ScheduledMessage;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.annotation.Commit;
import tk.mybatis.mapper.entity.Example;

import javax.jms.*;
import java.util.HashMap;
import java.util.Map;

@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    private PaymentInfoMapper paymentInfoMapper;

    @Reference
    private OrderService orderService;

    @Autowired
    private AlipayClient alipayClient;

    @Autowired
    private ActiveMQUtil activeMQUtil;

    @Value("${appid}")
    private String appid;
    @Value("${partner}")
    private String partner;
    @Value("${partnerkey}")
    private String partnerkey;

    @Override
    public void savePaymentInfo(PaymentInfo paymentInfo) {

        paymentInfoMapper.insertSelective(paymentInfo);

    }

    @Override
    public PaymentInfo getPaymentInfo(String outTradeNo) {

        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOutTradeNo(outTradeNo);
        return paymentInfoMapper.selectOne(paymentInfo);
    }

    @Override
    public PaymentInfo getPaymentInfo(PaymentInfo paymentInfo) {
        return paymentInfoMapper.selectOne(paymentInfo);
    }


    @Override
    public void updatePaymentInfo(String outTradeNo, PaymentInfo paymentInfoUPD) {

        Example example = new Example(PaymentInfo.class);
        example.createCriteria().andEqualTo("outTradeNo",outTradeNo);
        paymentInfoMapper.updateByExampleSelective(paymentInfoUPD,example);
    }

    @Override
    public boolean refund(String orderId) {

        //AlipayClient alipayClient = new DefaultAlipayClient("https://openapi.alipay.com/gateway.do","app_id","your private_key","json","GBK","alipay_public_key");
        AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();

        OrderInfo orderInfo = orderService.getOrderInfo(orderId);
        Map<String,Object> paramMap = new HashMap<>();
        paramMap.put("out_trade_no",orderInfo.getOutTradeNo());
        paramMap.put("refund_amount",orderInfo.getTotalAmount());
        paramMap.put("refund_reason","正常退款");

        request.setBizContent(JSON.toJSONString(paramMap));
        /*request.setBizContent("{" +
                "    \"out_trade_no\":\"20150320010101001\"," +
                "    \"trade_no\":\"2014112611001004680073956707\"," +
                "    \"refund_amount\":200.12," +
                "    \"refund_reason\":\"正常退款\"," +
                "    \"out_request_no\":\"HZ01RF001\"," +
                "    \"operator_id\":\"OP001\"," +
                "    \"store_id\":\"NJ_S_001\"," +
                "    \"terminal_id\":\"NJ_T_001\"" +
                "  }");*/
        AlipayTradeRefundResponse response = null;
        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        if(response.isSuccess()){
            System.out.println("调用成功");
            return true;
        } else {
            System.out.println("调用失败");
            return false;
        }
    }

    /**
     * 微信生成二维码
     * @param orderId
     * @param totalAmount
     * @return
     */
    @Override
    public Map createNative(String orderId, String totalAmount) {

        // 创建参数
        Map<String,String> map = new HashMap<>();
        map.put("appid",appid); //公众号
        map.put("mch_id",partner); //商户号
        map.put("nonce_str", WXPayUtil.generateNonceStr()); //随机字符串
        //map.put("sign",); atguigu3b0kn9g5v426MKfHQH7X8rKwb
        map.put("body","买帽子"); //商品描述
        map.put("out_trade_no",orderId);//商户订单号
        map.put("total_fee",totalAmount); //总金额（分）
        map.put("spbill_create_ip","127.0.0.1"); //终端IP
        map.put("notify_url","http://trade.gmall.com/trade"); // 回调地址
        map.put("trade_type","NATIVE"); //交易类型

        // 生成要发送的xml
        try {
            String xmlParam  = WXPayUtil.generateSignedXml(map, partnerkey);
            HttpClient client = new HttpClient("https://api.mch.weixin.qq.com/pay/unifiedorder");

            //设置发送的方式
            client.setHttps(true);
            client.setXmlParam(xmlParam);
            client.post();

            // 获得结果
            String content = client.getContent();
            Map<String, String> contentMap = WXPayUtil.xmlToMap(content);
            Map<String,String> resultMap = new HashMap<>();
            resultMap.put("code_url",contentMap.get("code_url"));
            resultMap.put("total_fee", totalAmount);
            resultMap.put("out_trade_no",orderId);

            return resultMap;

        } catch (Exception e) {
            e.printStackTrace();
        }


        return null;
    }

    @Override
    public void sendPaymentResult(PaymentInfo paymentInfo, String result) {

        Connection connection = activeMQUtil.getConnection();
        try {
            connection.start();
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
            // 创建队列
            Queue paymentResultQueue = session.createQueue("PAYMENT_RESULT_QUEUE");
            // 创建消息提供者
            MessageProducer producer = session.createProducer(paymentResultQueue);
            // 创建消息
            ActiveMQMapMessage mapMessage = new ActiveMQMapMessage();
            mapMessage.setString("result",result);
            mapMessage.setString("orderId",paymentInfo.getOrderId());
            producer.send(mapMessage);

            session.commit();

            producer.close();
            session.close();
            connection.close();

        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean checkPayment(PaymentInfo paymentInfoQuery) {

        if (paymentInfoQuery == null){
            return false;
        }
        /*if (paymentInfo.getPaymentStatus() == PaymentStatus.PAID || paymentInfo.getPaymentStatus() == PaymentStatus.ClOSED){
            return true;
        }*/
        //AlipayClient alipayClient = new DefaultAlipayClient("https://openapi.alipay.com/gateway.do","app_id","your private_key","json","GBK","alipay_public_key","RSA2");
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        Map<String,String> map = new HashMap<>();
        map.put("out_trade_no",paymentInfoQuery.getOutTradeNo());
        request.setBizContent(JSON.toJSONString(map));
        AlipayTradeQueryResponse response = null;
        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        // 说明已经支付了
        if(response.isSuccess()){
            System.out.println("调用成功");

            // 有没有支付成功再看回调的支付状态
            String tradeStatus = response.getTradeStatus();
            if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)){
                // 若查询成功则修改订单状态
                PaymentInfo paymentInfoUPD = new PaymentInfo();
                paymentInfoUPD.setPaymentStatus(PaymentStatus.PAID);
                updatePaymentInfo(paymentInfoQuery.getOutTradeNo(),paymentInfoUPD);

                // 发送消息队列
                sendPaymentResult(paymentInfoQuery,"success");
                return true;
            }
            return false;

        } else {
            //System.out.println("调用失败");
            return false;
        }
    }

    @Override
    public void sendDelayPaymentResult(String outTradeNo, int delaySec, int checkCount) {

        Connection connection = activeMQUtil.getConnection();

        try {
            connection.start();
            Session session = connection.createSession(true,Session.SESSION_TRANSACTED);
            Queue paymentResultCheckQueue = session.createQueue("PAYMENT_RESULT_CHECK_QUEUE");
            MessageProducer producer = session.createProducer(paymentResultCheckQueue);
            MapMessage mapMessage = new ActiveMQMapMessage();
            mapMessage.setString("outTradeNo",outTradeNo);
            mapMessage.setInt("delaySec",delaySec);
            mapMessage.setInt("checkCount",checkCount);
            // 设置延迟时间
            mapMessage.setLongProperty(ScheduledMessage.AMQ_SCHEDULED_DELAY, delaySec*1000);

            producer.send(mapMessage);
            session.commit();

            producer.close();
            session.close();
            connection.close();

        } catch (JMSException e) {
            e.printStackTrace();
        }


    }

}
