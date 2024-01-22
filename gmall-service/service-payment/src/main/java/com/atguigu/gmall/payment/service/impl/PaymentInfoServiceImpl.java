package com.atguigu.gmall.payment.service.impl;

import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.model.enums.PaymentStatus;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.payment.mapper.PaymentInfoMapper;
import com.atguigu.gmall.payment.service.PaymentInfoService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;

/**
* @author admin
* @description 针对表【payment_info(支付信息表)】的数据库操作Service实现
* @createDate 2024-01-04 14:35:04
*/
@Service
public class PaymentInfoServiceImpl extends ServiceImpl<PaymentInfoMapper, PaymentInfo>
    implements PaymentInfoService{

    @Autowired
    private PaymentInfoMapper paymentInfoMapper;

    @Autowired
    private RabbitService rabbitService;

    @Override
    public void savePaymentInfo(OrderInfo orderInfo, String paymentType) {
        //  细节处理. order_id and payment_type; ===> 类似于联合主键！
		/*
			细节处理：如下情景
				1. 用户使用支付宝支付，取消支付
				2. 再使用微信支付，取消支付
				3. 再使用支付宝字符，支付成功
			这时我们数据库中应该有几条数据？
			答：同一个订单Id中，可以有不同支付类型的支付记录，但同一支付类型应该只有一条记录
		*/
        LambdaQueryWrapper<PaymentInfo> paymentInfoLambdaQueryWrapper = new LambdaQueryWrapper<>();
        paymentInfoLambdaQueryWrapper.eq(PaymentInfo::getOrderId,orderInfo.getId());
        paymentInfoLambdaQueryWrapper.eq(PaymentInfo::getPaymentType,paymentType);
        PaymentInfo paymentInfoQuery = paymentInfoMapper.selectOne(paymentInfoLambdaQueryWrapper);
        //  判断交易记录表中是否有这条数据
        if (paymentInfoQuery != null){
            return;
        }

        //  创建对象
        PaymentInfo paymentInfo = new PaymentInfo();
        //  给paymentInfo 赋值
        paymentInfo.setOrderId(orderInfo.getId());
        paymentInfo.setUserId(orderInfo.getUserId());
        paymentInfo.setPaymentStatus(PaymentStatus.UNPAID.name());
        paymentInfo.setPaymentType(paymentType);
        paymentInfo.setTotalAmount(orderInfo.getTotalAmount());
        paymentInfo.setCreateTime(new Date());
        paymentInfo.setUpdateTime(new Date());
        paymentInfo.setOutTradeNo(orderInfo.getOutTradeNo());
        paymentInfo.setSubject(orderInfo.getTradeBody());
        paymentInfoMapper.insert(paymentInfo);
    }

    @Override
    public PaymentInfo getPaymentInfo(String outTradeNo, String paymentType) {
        //  创建查询条件
        LambdaQueryWrapper<PaymentInfo> paymentInfoLambdaQueryWrapper = new LambdaQueryWrapper<>();
        paymentInfoLambdaQueryWrapper.eq(PaymentInfo::getOutTradeNo,outTradeNo).eq(PaymentInfo::getPaymentType,paymentType);
        PaymentInfo paymentInfo = paymentInfoMapper.selectOne(paymentInfoLambdaQueryWrapper);
        return paymentInfo;
    }

    /**
     * 更新交易记录状态.
     * @param outTradeNo
     * @param paymentType
     * @param paramsMap
     */
    @Override
    public void updatePaymentInfoStatus(String outTradeNo, String paymentType, HashMap<String, String> paramsMap) {
        //  创建更新/查询条件条件
        LambdaUpdateWrapper<PaymentInfo> paymentInfoLambdaUpdateWrapper = new LambdaUpdateWrapper<>();
        paymentInfoLambdaUpdateWrapper.eq(PaymentInfo::getOutTradeNo,outTradeNo).eq(PaymentInfo::getPaymentType,paymentType);

        //  查询paymentInfo
        PaymentInfo paymentInfoQuery = paymentInfoMapper.selectOne(paymentInfoLambdaUpdateWrapper);

        if (paymentInfoQuery==null || "CLOSED".equals(paymentInfoQuery.getPaymentStatus())
                || "PAID".equals(paymentInfoQuery.getPaymentStatus())) {
            return;
        }

        //  设置更新的内容：payment_status ; callback_time; callback_content; trade_no;
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setPaymentStatus(PaymentStatus.PAID.name());
        paymentInfo.setCallbackTime(new Date());
        paymentInfo.setCallbackContent(paramsMap.toString());
        paymentInfo.setTradeNo(paramsMap.get("trade_no"));
        paymentInfoMapper.update(paymentInfo,paymentInfoLambdaUpdateWrapper);

        //  发送消息更改订单的状态.
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_PAYMENT_PAY,MqConst.ROUTING_PAYMENT_PAY,paymentInfoQuery.getOrderId());
    }

    @Override
    public void updatePaymentInfoStatus(String outTradeNo, String paymentType, PaymentInfo paymentInfo) {
        //  创建更新条件
        LambdaUpdateWrapper<PaymentInfo> paymentInfoLambdaUpdateWrapper = new LambdaUpdateWrapper<>();
        paymentInfoLambdaUpdateWrapper.eq(PaymentInfo::getOutTradeNo,outTradeNo).eq(PaymentInfo::getPaymentType,paymentType);
        //  调用更新方法。
        paymentInfoMapper.update(paymentInfo,paymentInfoLambdaUpdateWrapper);
    }

    /**
     * 根据主键修改交易状态
     * @param id
     */
    @Override
    public void closePaymentInfo(Long id) {
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setId(id);
        paymentInfo.setPaymentStatus(PaymentStatus.CLOSED.name());
        //  调用mapper 关闭交易记录
        paymentInfoMapper.updateById(paymentInfo);
    }
}




