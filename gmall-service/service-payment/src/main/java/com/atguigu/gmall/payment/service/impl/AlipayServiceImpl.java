package com.atguigu.gmall.payment.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradeCloseRequest;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeCloseResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.model.enums.PaymentStatus;
import com.atguigu.gmall.model.enums.PaymentType;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.order.client.OrderFeignClient;
import com.atguigu.gmall.payment.config.AlipayConfig;
import com.atguigu.gmall.payment.service.AlipayService;
import com.atguigu.gmall.payment.service.PaymentInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Calendar;

@Slf4j
@Service
public class AlipayServiceImpl implements AlipayService {

    @Autowired
    private AlipayClient alipayClient;
    @Autowired
    private OrderFeignClient orderFeignClient;

    @Autowired
    private PaymentInfoService paymentInfoService;

    @Autowired
    private RabbitService rabbitService;

    @Override
    public String createAliPay(Long orderId) throws AlipayApiException {
        try {
            //  根据订单Id 获取到订单对象
            OrderInfo orderInfo = orderFeignClient.getOrderInfo(orderId);
            //  使用支付宝支付，将数据保存到数据库.
            paymentInfoService.savePaymentInfo(orderInfo, PaymentType.ALIPAY.name());
            //  判断 当前订单状态是什么!
            if ("CLOSED".equals(orderInfo.getOrderStatus()) || "PAID".equals(orderInfo.getOrderStatus())){
                return "当前订单已支付或已关闭";
            }
            //    官方给的代码： AlipayClient 放入spring 容器中！
            //    支付请求对象
            AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
            //    异步回调地址  http://9949xa.natappfree.cc/api/payment/alipay/callback/notify 支付宝服务器主动发送给电商服务器的通知！
            request.setNotifyUrl(AlipayConfig.notify_payment_url);
            //    同步回调地址
            request.setReturnUrl(AlipayConfig.return_payment_url);
            //    封装业务参数
            JSONObject bizContent = new JSONObject();
            bizContent.put("out_trade_no", orderInfo.getOutTradeNo()); // 这个值是支付宝用来判断是否重复付款的依据
            bizContent.put("total_amount", orderInfo.getTotalAmount());
//            bizContent.put("total_amount", 0.01); //测试用
            bizContent.put("subject", orderInfo.getTradeBody());
            bizContent.put("product_code", "FAST_INSTANT_TRADE_PAY");
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            //  给二维码设置有效期？自己想;  二维码默认的刷新时间是2m
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.MINUTE,10);
            bizContent.put("time_expire", simpleDateFormat.format(calendar.getTime())); //    绝对超时时间
            //bizContent.put("time_expire", "2022-08-01 22:00:00"); 绝对超时时间
            //bizContent.put("timeout_express", "10m"); 相对超时时间
            //  timeout_express 相对超时时间 10m
            request.setBizContent(bizContent.toString());
            //  获取到表单
            String form = alipayClient.pageExecute(request).getBody();
            //  返回表单
            return form;
        } catch (AlipayApiException e) {
            log.error("生成支付二维码失败: {}" + orderId + "失败原因：{}"+e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public Boolean refund(Long orderId) {
        //  需要使用商户订单编号:
        OrderInfo orderInfo = this.orderFeignClient.getOrderInfo(orderId);
        //  如果当前订单已关闭，无需退款！
        if ("CLOSED".equals(orderInfo.getOrderStatus())) {
            return false;
        }
        //  退款请求对象
        AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", orderInfo.getOutTradeNo());
        bizContent.put("refund_amount", orderInfo.getTotalAmount());
        //  部分退款必须传递： bizContent.put("out_request_no", "HZ01RF001");

        request.setBizContent(bizContent.toString());
        AlipayTradeRefundResponse response = null;
        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            throw new RuntimeException(e);
        }
        if(response.isSuccess()){
            //  表示退款成功.
            if ("Y".equals(response.getFundChange())){
                //  更改交易记录的状态，订单状态!
                //                HashMap<String, String> map = new HashMap<>();
                //                map.put("trade_no",response.getOutTradeNo());
                PaymentInfo paymentInfo = new PaymentInfo();
                paymentInfo.setPaymentStatus(PaymentStatus.CLOSED.name());
                paymentInfo.setTradeNo(response.getTradeNo());
                //  交易记录状态——改为关闭
                //  this.paymentService.updatePaymentInfoStatus(orderInfo.getOutTradeNo(),PaymentType.ALIPAY.name(),map);
                paymentInfoService.updatePaymentInfoStatus(orderInfo.getOutTradeNo(),PaymentType.ALIPAY.name(),paymentInfo);
                //  订单状态——改为关闭，异步：mq
                rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_ORDER_CLOSED,MqConst.ROUTING_ORDER_CLOSED,orderId);
                return true;
            }else {
                return false;
            }
        } else {
            return false;
        }
    }

    @Override
    public Boolean checkPayment(Long orderId) {
        //  获取订单对象
        OrderInfo orderInfo = this.orderFeignClient.getOrderInfo(orderId);
        //  判断订单对象
        if (orderInfo==null){
            return false;
        }
        //  声明查询交易对象
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();

        //  封装请求参数
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", orderInfo.getOutTradeNo());
        request.setBizContent(bizContent.toJSONString());
        AlipayTradeQueryResponse response = null;
        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            throw new RuntimeException(e);
        }
        if(response.isSuccess()){
            //            如何你想判断是否支付成功，需要添加这个业务逻辑。如果需要判断是否支付成功，只需要返回true;
            //            String tradeStatus = response.getTradeStatus();
            //            if ("WAIT_BUYER_PAY".equals(tradeStatus)){
            //                log.info("等待付款");
            //            }else if ("TRADE_SUCCESS".equals(tradeStatus)){
            //                log.info("支付成功");
            //            }else {
            //                log.info("扫码未支付超时关闭");
            //            }
            //  有交易记录
            return true;
        } else {
            //  没有交易记录
            return false;
        }
    }

    @Override
    public Boolean closePay(Long orderId) {
        //  获取订单对象
        OrderInfo orderInfo = this.orderFeignClient.getOrderInfo(orderId);
        //  判断订单对象
        if (orderInfo==null){
            return false;
        }
        //  创建关闭交易对象
        AlipayTradeCloseRequest request = new AlipayTradeCloseRequest();
        //  封装请求参数
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", orderInfo.getOutTradeNo());
        request.setBizContent(bizContent.toString());
        AlipayTradeCloseResponse response = null;
        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            throw new RuntimeException(e);
        }
        if(response.isSuccess()){
            //  表示关闭成功
            return true;
        } else {
            //  关闭失败
            return false;
        }
    }
}
