package com.cumt.gmall.payment.config;

import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:alipay.properties")
public class AlipayConfig {

    @Value("${alipay_url}")
    private String alipay_url; //支付宝网关（固定）

    @Value("${app_private_key}")
    private String app_private_key; //开发者私钥，由开发者自己生成

    @Value("${app_id}")
    private String app_id; //APPID 即创建应用后生成


    public final static String format="json";  //参数返回格式，只支持 json
    public final static String charset="utf-8"; //编码集，支持 GBK/UTF-8
    public final static String sign_type="RSA2"; //商户生成签名字符串所使用的签名算法类型，目前支持 RSA2 和 RSA，推荐使用 RSA2


    public static String return_payment_url;

    public static String notify_payment_url;

    public static String return_order_url;

    public static String alipay_public_key;

    @Value("${alipay_public_key}")
    public void setAlipay_public_key(String alipay_public_key) {
        AlipayConfig.alipay_public_key = alipay_public_key;
    }

    @Value("${return_payment_url}")
    public void setReturn_url(String return_payment_url) {
        AlipayConfig.return_payment_url = return_payment_url;
    }

    @Value("${notify_payment_url}")
    public void setNotify_url(String notify_payment_url) {
        AlipayConfig.notify_payment_url = notify_payment_url;
    }

    @Value("${return_order_url}")
    public void setReturn_order_url(String return_order_url) {
        AlipayConfig.return_order_url = return_order_url;
    }

    @Bean
    public AlipayClient alipayClient(){
        AlipayClient alipayClient=new DefaultAlipayClient(alipay_url,app_id,app_private_key,format,charset,alipay_public_key,sign_type);
        return alipayClient;
    }

}
