package com.atguigu.gmall.list.receiver;

import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.list.service.SearchService;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 消费消息
 */
@Component
@Slf4j
public class ListReceiver {

    @Autowired
    private SearchService searchService;

    /**
     * 监听商品上架消息
     * @param skuId
     * @param message
     * @param channel
     */
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_GOODS_UPPER, durable = "true", autoDelete = "false"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_GOODS),
            key = {MqConst.ROUTING_GOODS_UPPER}
    ))
    public void upperGoods(Long skuId, Message message, Channel channel) {
        try {
            //  判断消息
            if (skuId!=null){
                searchService.upperGoods(skuId);
            }
        } catch (Exception e) {
            //  消息没有被正确处理：1.消息标识 2.是否批量签收 3.是否重回队列
            //  channel.basicNack(message.getMessageProperties().getDeliveryTag(),false,true);
            log.error("商品上架失败 {}" + skuId);
            //  insert into value (id ... ); 消息记录表 ---> 人工处理！
            throw new RuntimeException(e);
        }
        //  手动确认
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }

    /**
     * 监听商品下架消息
     */
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_GOODS_LOWER,durable = "true",autoDelete = "false"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_GOODS),
            key = {MqConst.ROUTING_GOODS_LOWER}
    ))
    public void lowerGoods(Long skuId, Message message, Channel channel){
        try {
            //  判断消息
            if (skuId!=null){
                searchService.lowerGoods(skuId);
            }
        } catch (Exception e) {
            //  消息没有被正确处理：1.消息标识 2.是否批量签收 3.是否重回队列
            //  channel.basicNack(message.getMessageProperties().getDeliveryTag(),false,true);
            log.error("商品上架失败 {}" + skuId);
            //  insert into value (id ... ); 消息记录表 ---> 人工处理！
            throw new RuntimeException(e);
        }
        //  手动确认
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }
}
