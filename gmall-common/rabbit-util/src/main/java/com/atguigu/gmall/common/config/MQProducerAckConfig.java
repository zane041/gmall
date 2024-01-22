package com.atguigu.gmall.common.config;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.model.GmallCorrelationData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

/**
 * @Description 消息发送确认-确保生产者消息不丢失
 * <p>
 * ConfirmCallback  只确认消息是否正确到达 Exchange 中
 * ReturnCallback   消息没有正确到达队列时触发回调，如果正确到达队列不执行
 * <p>
 * 1. 如果消息没有到exchange,则confirm回调,ack=false
 * 2. 如果消息到达exchange,则confirm回调,ack=true
 * 3. exchange到queue成功,则不回调return
 * 4. exchange到queue失败,则回调return
 */
@Slf4j
@Component
public class MQProducerAckConfig implements RabbitTemplate.ConfirmCallback, RabbitTemplate.ReturnCallback {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 应用启动后触发一次
     */
    @PostConstruct // 在服务器加载Servlet的时候执行，并且只会被服务器加载构造函数之后，init()方法之前执行
    public void init() {
        rabbitTemplate.setConfirmCallback(this);
        rabbitTemplate.setReturnCallback(this);
    }

    /**
     * 只确认消息是否正确到达 Exchange 中,成功与否都会回调
     *
     * @param correlationData 相关数据  非消息本身业务数据
     * @param ack             应答结果
     * @param cause           如果发送消息到交换器失败，错误原因
     */
    @Override
    public void confirm(CorrelationData correlationData, boolean ack, String cause) {
        if (ack) {
            //消息到交换器成功
            log.info("消息发送到Exchange成功：{}", correlationData);
        } else {
            //消息到交换器失败
            log.error("消息发送到Exchange失败：{}", cause);
            //执行消息重发
            this.retrySendMsg(correlationData);
        }
    }

    /**
     * 消息没有正确到达队列时触发回调，如果正确到达队列不执行
     *
     * @param message    消息对象，包含相关对象唯一标识
     * @param replyCode  应答码
     * @param replyText  应答提示信息
     * @param exchange   交换器
     * @param routingKey 路由键
     */
    @Override
    public void returnedMessage(Message message, int replyCode, String replyText, String exchange, String routingKey) {
        log.error("消息路由queue失败，应答码={}，原因={}，交换机={}，路由键={}，消息={}",
                replyCode, replyText, exchange, routingKey, message.toString());
        //当路由队列失败 也需要重发
        //1.构建相关数据对象
        String redisKey = message.getMessageProperties().getHeader("spring_returned_message_correlation");
        String correlationDataStr = (String) redisTemplate.opsForValue().get(redisKey);
        GmallCorrelationData gmallCorrelationData = JSON.parseObject(correlationDataStr, GmallCorrelationData.class);
        //todo 方式一:如果不考虑延迟消息重发 直接返回
        if(gmallCorrelationData != null && gmallCorrelationData.isDelay()){
            return;
        }
        //2.调用消息重发方法
        this.retrySendMsg(gmallCorrelationData);
    }


    /**
     * 消息重新发送
     *
     * @param correlationData
     */
    private void retrySendMsg(CorrelationData correlationData) {
        //获取相关数据
        GmallCorrelationData gmallCorrelationData = (GmallCorrelationData) correlationData;

        //获取redis中存放重试次数
        //先重发，在写会到redis中次数
        int retryCount = gmallCorrelationData.getRetryCount();
        if (retryCount >= 3) {
            //超过最大重试次数
            log.error("生产者超过最大重试次数，将失败的消息存入数据库用人工处理；给管理员发送邮件；给管理员发送短信；");
            return;
        }
        //重发次数+1
        retryCount += 1;
        gmallCorrelationData.setRetryCount(retryCount);
        redisTemplate.opsForValue().set(gmallCorrelationData.getId(), JSON.toJSONString(gmallCorrelationData), 10, TimeUnit.MINUTES);
        log.info("进行消息重发！");
        //重发消息
        //todo 方式二：如果是延迟消息，依然需要设置消息延迟时间
        if (gmallCorrelationData.isDelay()) {
            //延迟消息
            rabbitTemplate.convertAndSend(gmallCorrelationData.getExchange(), gmallCorrelationData.getRoutingKey(), gmallCorrelationData.getMessage(), message -> {
                message.getMessageProperties().setDelay(gmallCorrelationData.getDelayTime() * 1000);
                return message;
            }, gmallCorrelationData);
        } else {
            //普通消息
            rabbitTemplate.convertAndSend(gmallCorrelationData.getExchange(), gmallCorrelationData.getRoutingKey(), gmallCorrelationData.getMessage(), gmallCorrelationData);
        }
    }
}