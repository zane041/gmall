package com.atguigu.gmall.activity.receiver;

import com.atguigu.gmall.activity.service.SeckillGoodsService;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.util.DateUtil;
import com.atguigu.gmall.model.activity.SeckillGoods;
import com.atguigu.gmall.model.activity.UserRecode;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.Date;
import java.util.List;

@Slf4j
@Component
public class SeckillReceiver {

    @Autowired
    private SeckillGoodsService seckillGoodsService;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 将秒杀商品放入缓存
     * @param message
     * @param channel
     */
    @RabbitListener(bindings = @QueueBinding(
            exchange = @Exchange(MqConst.EXCHANGE_DIRECT_TASK),
            value = @Queue(MqConst.QUEUE_TASK_1),
            key = {MqConst.ROUTING_TASK_1}
    ))
    public void importSeckillGoodsToRedis(Message message, Channel channel) {
        try {
            // 1. 查询当天的秒杀商品。条件：当天、剩余库存>0，审核状态=1
            List<SeckillGoods> seckillGoodsList = seckillGoodsService.list(new LambdaQueryWrapper<SeckillGoods>()
                    .eq(SeckillGoods::getStatus, "1")
                    .gt(SeckillGoods::getStockCount, 0)
                    .apply("date_format(start_time,'%Y-%m-%d')<={0}", DateUtil.formatDate(new Date()))
                    .apply("date_format(end_time,'%Y-%m-%d')>={0}", DateUtil.formatDate(new Date()))
            );
            // 2. 将查询到的秒杀商品放入缓存！
            for (SeckillGoods seckillGoods : seckillGoodsList) {
                // 使用hash类型存储
                // 判断当前缓存key中是否有该秒杀商品，不重复放入
                Boolean flag = redisTemplate.boundHashOps(RedisConst.SECKILL_GOODS).hasKey(seckillGoods.getSkuId().toString());
                if (Boolean.TRUE.equals(flag)) {
                    continue;
                }
                redisTemplate.boundHashOps(RedisConst.SECKILL_GOODS).put(seckillGoods.getSkuId().toString(), seckillGoods);
                // 3. 将每个商品对应的库存剩余数，放入redis-list集合中！利于其原子性防止超卖！
                for (Integer i = 0; i < seckillGoods.getStockCount(); i++) {
                    String key = RedisConst.SECKILL_STOCK_PREFIX + seckillGoods.getSkuId();
                    redisTemplate.boundListOps(key).leftPush(seckillGoods.getSkuId().toString());
                }
                // 4. 初始化该秒杀商品的状态位
                // 状态位 1 表示秒杀中，状态位 0 表示秒杀完了
                redisTemplate.convertAndSend("seckillpush", seckillGoods.getSkuId() + ":1");
            }
            // 手动确认消息
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (IOException e) {
            e.printStackTrace();
            log.error("【秒杀服务】秒杀商品预热异常：{}", e);
        }
    }

    /**
     * 监听预下单队列中的信息
     * @param userRecode
     * @param message
     * @param channel
     */
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_SECKILL_USER,durable = "true",autoDelete = "false"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_SECKILL_USER),
            key = {MqConst.ROUTING_SECKILL_USER}
    ))
    public void seckillUser(UserRecode userRecode, Message message , Channel channel){
        try {
            //  判断
            if (userRecode!=null){
                log.info("处理队列中的数据信息。");
                //  业务处理：
                seckillGoodsService.seckillUser(userRecode);
            }
        } catch (Exception e) {
            log.error("处理队列中的数据处理失败."+e.getMessage());
            throw new RuntimeException(e);
        }
        //  手动签收
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }

    /**
     * 监听秒杀减库存消息
     * @param skuId
     * @param message
     * @param channel
     */
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_SECKILL_STOCK,durable = "true",autoDelete = "false"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_SECKILL_STOCK),
            key = {MqConst.ROUTING_SECKILL_STOCK}
    ))
    public void seckillStock(Long skuId, Message message ,Channel channel){
        try {
            //  判断
            if (skuId!=null){
                log.info("监听秒杀减库存。");
                //  业务处理：
                seckillGoodsService.seckillStock(skuId);
            }
        } catch (Exception e) {
            log.error("监听秒杀减库存."+e.getMessage());
            throw new RuntimeException(e);
        }
        //  手动签收
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }

    /**
     * 秒杀结束清空缓存数据
     * @param msg
     * @param message
     * @param channel
     */
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_TASK_18,durable = "true",autoDelete = "false"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_TASK),
            key = {MqConst.ROUTING_TASK_18}
    ))
    public void clearData(String msg, Message message, Channel channel){
        try {
            //  删除数据.   审核状态=1 and endTime<new Date();
            LambdaQueryWrapper<SeckillGoods> seckillGoodsLambdaQueryWrapper = new LambdaQueryWrapper<>();
            seckillGoodsLambdaQueryWrapper.eq(SeckillGoods::getStatus,"1");
            seckillGoodsLambdaQueryWrapper.le(SeckillGoods::getEndTime,new Date());
            //  查询当天秒杀结束的商品.
            List<SeckillGoods> seckillGoodsList = seckillGoodsService.list(seckillGoodsLambdaQueryWrapper);
            //  循环遍历
            if (!CollectionUtils.isEmpty(seckillGoodsList)){
                for (SeckillGoods seckillGoods : seckillGoodsList) {
                    //  删除商品的剩余库存;
                    this.redisTemplate.delete(RedisConst.SECKILL_STOCK_PREFIX+seckillGoods.getSkuId());
                }
            }
            //  删除秒杀商品key
            this.redisTemplate.delete(RedisConst.SECKILL_GOODS);
            //  删除预下单的key
            this.redisTemplate.delete(RedisConst.SECKILL_ORDERS);
            //  删除真正秒杀key
            this.redisTemplate.delete(RedisConst.SECKILL_ORDERS_USERS);

            //  更改数据库
            //  设置更新内容
            SeckillGoods seckillGoods = new SeckillGoods();
            seckillGoods.setStatus("0");
            this.seckillGoodsService.update(seckillGoods,seckillGoodsLambdaQueryWrapper);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        //  手动确认
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }
}
