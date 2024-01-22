package com.atguigu.gmall.payment.service;

import com.alipay.api.AlipayApiException;

public interface AlipayService {

    /**
     * 生成二维码
     * @param orderId
     * @return
     */
    String createAliPay(Long orderId) throws AlipayApiException;

    /**
     * 根据订单Id 发起退款
     * @param orderId
     * @return
     */
    Boolean refund(Long orderId);

    /**
     * 根据订单Id 关闭交易记录
     * @param orderId
     * @return
     */
    Boolean closePay(Long orderId);

    /**
     * 根据订单查看支付宝是否有交易记录
     * @param orderId
     * @return
     */
    Boolean checkPayment(Long orderId);
}
