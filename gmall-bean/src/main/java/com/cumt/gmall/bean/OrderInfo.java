package com.cumt.gmall.bean;

import com.cumt.gmall.bean.enums.OrderStatus;
import com.cumt.gmall.bean.enums.PaymentWay;
import com.cumt.gmall.bean.enums.ProcessStatus;
import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
public class OrderInfo implements Serializable {

    @Column
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String id;

    @Column
    private String consignee; //收货人

    @Column
    private String consigneeTel;


    @Column
    private BigDecimal totalAmount; //总金额，计算

    @Column
    private OrderStatus orderStatus; //订单状态，用于显示给用户查看

    @Column
    private ProcessStatus processStatus; //订单进程状态


    @Column
    private String userId;

    @Column
    private PaymentWay paymentWay;

    @Column
    private Date expireTime; //过期时间

    @Column
    private String deliveryAddress;

    @Column
    private String orderComment; //订单状态。页面获取

    @Column
    private Date createTime;

    @Column
    private String parentOrderId;

    @Column
    private String trackingNo; //物流编号


    @Transient
    private List<OrderDetail> orderDetailList;


    @Transient
    private String wareId;

    @Column
    private String outTradeNo; // 第三方交易编号

    // 计算总金额
    public void sumTotalAmount(){
        BigDecimal totalAmount=new BigDecimal("0");
        for (OrderDetail orderDetail : orderDetailList) {
            totalAmount= totalAmount.add(orderDetail.getOrderPrice().multiply(new BigDecimal(orderDetail.getSkuNum())));
        }
        this.totalAmount=  totalAmount;
    }
}
