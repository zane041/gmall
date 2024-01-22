package com.atguigu.gmall.order.receiver;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.model.enums.OrderStatus;
import com.atguigu.gmall.model.enums.ProcessStatus;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.order.service.OrderInfoService;
import com.atguigu.gmall.payment.client.PaymentFeignClient;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
public class OrderReceiver {

    @Autowired
    private OrderInfoService orderInfoService;

    @Autowired
    private PaymentFeignClient paymentFeignClient;


    /**
     * 取消订单 目的为了防止用户在过期一瞬间进行支付！
     * @param orderId
     * @param message
     * @param channel
     */
    @SneakyThrows
    @RabbitListener(queues = MqConst.QUEUE_ORDER_CANCEL)
    public void orderCancel(Long orderId, Message message, Channel channel){
        try {
            //  判断
            if (orderId!=null){
                //  根据订单Id 获取订单状态
                OrderInfo orderInfo = orderInfoService.getById(orderId);
                //  判断
                if (orderInfo!=null && "UNPAID".equals(orderInfo.getOrderStatus())
                        && "UNPAID".equals(orderInfo.getProcessStatus())){
                    //  判断你是否有交易记录
                    PaymentInfo paymentInfoQuery = paymentFeignClient.getPaymentInfo(orderInfo.getOutTradeNo());
                    if (paymentInfoQuery!=null && "UNPAID".equals(paymentInfoQuery.getPaymentStatus())){
                        //  判断是否有alipay交易记录
                        Boolean exist = paymentFeignClient.checkPayment(orderId);
                        //  判断是否存在交易记录
                        if (exist){
                            //  调用关闭交易记录方法判断
                            Boolean result = paymentFeignClient.closePay(orderId);
                            //  关闭成功！
                            if (result){
                                log.info("这个订单没有支付：{} " , orderId);
                                //  说明用还未支付 ，关闭支付宝+paymentInfo+orderInfo;
                                orderInfoService.execExpiredOrder(orderId,"2");
                            }else {
                                //  关闭失败！支付成功.
                                log.info("这个订单支付成功：{} ", orderId);
                            }

                        } else {
                            log.info("关闭 orderInfo paymentInfo 信息 {}：", orderId);
                            //   不存在支付宝交易记录.
                            //  可能需要关闭orderInfo+paymentInfo;
                            orderInfoService.execExpiredOrder(orderId,"2");
                        }
                    } else {
                        log.info("关闭 orderInfo 信息 {}：", orderId);
                        //  没有本地交易记录.只有orderInfo
                        //  调用取消订单方法
                        //  orderService.execExpiredOrder(orderId); // 更改订单状态+更改本地交易记录状态。
                        orderInfoService.execExpiredOrder(orderId,"1");
                    }
                }
            }
        } catch (Exception e) {
            log.error("取消订单异常 {} 将消息插入数据库" , orderId);
            throw new RuntimeException(e);
        }
        //  消息确认
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }

    /**
     * 监听到支付成功后消息，修改订单状态为已支付；发送扣减库存消息到MQ
     *
     * @param orderId
     * @param message
     * @param channel
     */
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            exchange = @Exchange(MqConst.EXCHANGE_DIRECT_PAYMENT_PAY),
            value = @Queue(MqConst.QUEUE_PAYMENT_PAY),
            key = {MqConst.ROUTING_PAYMENT_PAY}
    ))
    public void paySuccessUpdateOrder(Long orderId, Message message, Channel channel) {
        try {
            //  判断订单Id
            if (orderId != null) {
                log.info("【订单微服务】监听到支付成功的订单：ID为：{}", orderId);
                OrderInfo orderInfo = orderInfoService.getById(orderId);
                //通过业务字段判消息幂等性.
                if (orderInfo != null && !OrderStatus.PAID.name().equals(orderInfo.getOrderStatus())) {
                    //更新订单状态
                    orderInfoService.updateOrderStatus(orderId, ProcessStatus.PAID);
                    //  发送消息给库存系统
                    orderInfoService.sendDeductStockMsg(orderId);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }

    //  监听减库存队列
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_WARE_ORDER,durable = "true",autoDelete = "false"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_WARE_ORDER),
            key = {MqConst.ROUTING_WARE_ORDER}
    ))
    public void updateOrder(String jsonStr , Message message, Channel channel){
        try {
            //  判断消息不为空
            if (!StringUtils.isEmpty(jsonStr)){
                //  将json 转换为map
                Map map = JSONObject.parseObject(jsonStr, Map.class);
                String orderId = (String) map.get("orderId");
                String status = (String) map.get("status");

                //  判断当前状态
                if ("DEDUCTED".equals(status)){
                    //  已减库存 更新订单状态.
                    orderInfoService.updateOrderStatus(Long.parseLong(orderId),ProcessStatus.WAITING_DELEVER);
                }else {
                    //  已减库存 更新订单状态.就需要从其他地方调用商品. 及时补货！如果补货成功，那就再更新一次订单状态。保证的数据最终一致性！
                    orderInfoService.updateOrderStatus(Long.parseLong(orderId),ProcessStatus.STOCK_EXCEPTION);
                }
            }
        } catch (NumberFormatException e) {
            log.error("减库存异常 {}"+ jsonStr);
            throw new RuntimeException(e);
        }
        //  手动确认：
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }
}