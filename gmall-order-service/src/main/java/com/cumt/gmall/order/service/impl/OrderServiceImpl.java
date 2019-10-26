package com.cumt.gmall.order.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.cumt.gmall.bean.OrderDetail;
import com.cumt.gmall.bean.OrderInfo;
import com.cumt.gmall.bean.enums.OrderStatus;
import com.cumt.gmall.bean.enums.ProcessStatus;
import com.cumt.gmall.config.ActiveMQUtil;
import com.cumt.gmall.config.RedisUtil;
import com.cumt.gmall.order.mapper.OrderDetailMapper;
import com.cumt.gmall.order.mapper.OrderInfoMapper;
import com.cumt.gmall.service.OrderService;
import com.cumt.gmall.util.HttpClientUtil;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import redis.clients.jedis.Jedis;

import javax.jms.*;
import javax.jms.Queue;
import java.util.*;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private ActiveMQUtil activeMQUtil;

    @Override
    @Transactional
    public String saveOrder(OrderInfo orderInfo) {

        // 计算总金额
        orderInfo.sumTotalAmount();
        // 创建时间
        orderInfo.setCreateTime(new Date());
        // 过期时间
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE,1);
        orderInfo.setExpireTime(calendar.getTime());
        //第三方交易编号
        String outTradeNo = "CUMT"+System.currentTimeMillis()+""+new Random().nextInt(1000);
        orderInfo.setOutTradeNo(outTradeNo);
        //订单状态
        orderInfo.setOrderStatus(OrderStatus.UNPAID);
        //进程状态
        orderInfo.setProcessStatus(ProcessStatus.UNPAID);

        orderInfoMapper.insertSelective(orderInfo);

        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            // 设置orderId
            orderDetail.setOrderId(orderInfo.getId());
            orderDetailMapper.insertSelective(orderDetail);
        }
        // 返回订单编号
        return orderInfo.getId();
    }

    @Override
    public String getOutTradeNo(String userId) {

        String tradeCodeKey = "user:" + userId + ":tradeCode";
        Jedis jedis = redisUtil.getJedis();
        String tradeCode = UUID.randomUUID().toString().replace("-", "");
        jedis.set(tradeCodeKey, tradeCode);
        jedis.close();
        return tradeCode;
    }

    @Override
    public boolean checkTradeNo(String tradeCode, String userId) {

        String tradeCodeKey = "user:" + userId + ":tradeCode";
        Jedis jedis = redisUtil.getJedis();
        String tradeNo = jedis.get(tradeCodeKey);
        jedis.close();
        if (tradeCode.equals(tradeNo)){
            return true;
        }else {
            return false;
        }
    }

    @Override
    public void delOutTradeNo(String userId) {
        String tradeCodeKey = "user:" + userId + ":tradeCode";
        Jedis jedis = redisUtil.getJedis();
        jedis.del(tradeCodeKey);
        jedis.close();
    }

    @Override
    public boolean checkStock(String skuId, Integer skuNum) {

        String result = HttpClientUtil.doGet("http://www.gware.com/hasStock?skuId=" + skuId + "&num=" + skuNum);

        return "1".equals(result);
    }

    @Override
    public OrderInfo getOrderInfo(String orderId) {
        OrderInfo orderInfo = orderInfoMapper.selectByPrimaryKey(orderId);

        OrderDetail orderDetail = new OrderDetail();
        orderDetail.setOrderId(orderId);
        List<OrderDetail> orderDetailList = orderDetailMapper.select(orderDetail);

        orderInfo.setOrderDetailList(orderDetailList);

        return orderInfo;
    }

    @Override
    public void updateOrderStatus(String orderId, ProcessStatus processStatus) {

        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setId(orderId);
        orderInfo.setProcessStatus(processStatus);
        orderInfo.setOrderStatus(processStatus.getOrderStatus());
        orderInfoMapper.updateByPrimaryKeySelective(orderInfo);

    }

    @Override
    public void sendOrderStatus(String orderId) {

        Connection connection = activeMQUtil.getConnection();
        String orderJson = initWareOrder(orderId);

        try {
            connection.start();
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
            Queue orderResultQueue = session.createQueue("ORDER_RESULT_QUEUE");
            MessageProducer producer = session.createProducer(orderResultQueue);

            ActiveMQTextMessage textMessage = new ActiveMQTextMessage();
            textMessage.setText(orderJson);

            producer.send(textMessage);

            session.commit();

            producer.close();
            session.close();
            connection.close();

        } catch (JMSException e) {
            e.printStackTrace();
        }


    }

    /**
     * 根据orderId获取order字符串
     * @param orderId
     * @return
     */
    private String initWareOrder(String orderId) {

        OrderInfo orderInfo = getOrderInfo(orderId);

        //将部分orderInfo数据制作成map！
        Map map = initWareOrder(orderInfo);

        return JSON.toJSONString(map);

    }

    private Map initWareOrder(OrderInfo orderInfo) {

        Map<String, Object> map = new HashMap();
        map.put("orderId",orderInfo.getId());
        map.put("consignee", orderInfo.getConsignee());
        map.put("consigneeTel",orderInfo.getConsigneeTel());
        map.put("orderComment",orderInfo.getOrderComment());
        map.put("orderBody","买东西");
        map.put("deliveryAddress",orderInfo.getDeliveryAddress());
        map.put("paymentWay","2");

        List<Map> list = new ArrayList<>();
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            Map<String,Object> detailMap = new HashMap<>();
            detailMap.put("skuId",orderDetail.getSkuId());
            detailMap.put("skuNum",orderDetail.getSkuNum());
            detailMap.put("skuName",orderDetail.getSkuName());

            list.add(detailMap);
        }

        map.put("details",list);

        return map;

    }


}
