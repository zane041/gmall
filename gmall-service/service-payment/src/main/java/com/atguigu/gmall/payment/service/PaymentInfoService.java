package com.atguigu.gmall.payment.service;

import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.HashMap;

/**
* @author admin
* @description 针对表【payment_info(支付信息表)】的数据库操作Service
* @createDate 2024-01-04 14:35:04
*/
public interface PaymentInfoService extends IService<PaymentInfo> {

    /***
     * 保存交易记录
     * @param orderInfo
     * @param paymentType
     */
    void savePaymentInfo(OrderInfo orderInfo, String paymentType);

    /**
     * 根据第三方交易编号查询paymentInfo
     * @param outTradeNo
     * @param paymentType
     * @return
     */
    PaymentInfo getPaymentInfo(String outTradeNo, String paymentType);

    /**
     * 更新交易状态
     * @param outTradeNo
     * @param paymentType
     * @param paramsMap
     */
    void updatePaymentInfoStatus(String outTradeNo, String paymentType, HashMap<String, String> paramsMap);


    /**
     * 更新交易记录状态。
     * @param outTradeNo
     * @param paymentType
     * @param paymentInfo
     */
    void updatePaymentInfoStatus(String outTradeNo, String paymentType, PaymentInfo paymentInfo);

    /**
     * 根据paymentInfo.id 关闭交易记录。
     * @param id
     */
    void closePaymentInfo(Long id);
}
