package com.atguigu.gmall.payment.client;

import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.payment.client.impl.PaymentDegradeFeignClient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(value = "service-payment",fallback = PaymentDegradeFeignClient.class)
public interface PaymentFeignClient {

    /**
     * 查询本地交易记录
     * @param outTradeNo
     * @return
     */
    @GetMapping("/api/payment/alipay/getPaymentInfo/{outTradeNo}")
    public PaymentInfo getPaymentInfo(@PathVariable String outTradeNo);



    /**
     * 根据订单ID关闭支付宝交易记录
     * @param orderId 订单ID
     * @return
     */
    @GetMapping("/api/payment/alipay/closePay/{orderId}")
    public Boolean closePay(@PathVariable Long orderId);



    /**
     * 查看是否有交易记录
     * @param orderId
     * @return
     */
    @GetMapping("/api/payment/alipay/checkPayment/{orderId}")
    public Boolean checkPayment(@PathVariable Long orderId);

}