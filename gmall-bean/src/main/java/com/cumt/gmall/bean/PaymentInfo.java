package com.cumt.gmall.bean;

import com.cumt.gmall.bean.enums.PaymentStatus;
import lombok.Data;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class PaymentInfo implements Serializable {

    @Column
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String  id;

    @Column
    private String outTradeNo; //订单中已生成的对外交易编号。订单中获取

    @Column
    private String orderId;

    @Column
    private String alipayTradeNo; //订单编号  初始为空，支付宝回调时生成

    @Column
    private BigDecimal totalAmount; //订单金额。订单中获取

    @Column
    private String Subject; //交易内容。利用商品名称拼接。

    @Column
    private PaymentStatus paymentStatus; //支付状态，默认值未支付。

    @Column
    private Date createTime;//创建时间，当前时间。

    @Column
    private Date callbackTime; //回调时间，初始为空，支付宝异步回调时记录

    @Column
    private String callbackContent; //回调信息，初始为空，支付宝异步回调时记录
}
