package com.atguigu.gmall.activity.service.impl;

import com.atguigu.gmall.activity.mapper.SeckillGoodsMapper;
import com.atguigu.gmall.activity.service.SeckillGoodsService;
import com.atguigu.gmall.activity.util.CacheHelper;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.result.ResultCodeEnum;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.common.util.MD5;
import com.atguigu.gmall.model.activity.OrderRecode;
import com.atguigu.gmall.model.activity.SeckillGoods;
import com.atguigu.gmall.model.activity.UserRecode;
import com.atguigu.gmall.model.order.OrderDetail;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
* @author admin
* @description 针对表【seckill_goods】的数据库操作Service实现
* @createDate 2024-01-07 21:46:20
*/
@Service
@Slf4j
public class SeckillGoodsServiceImpl extends ServiceImpl<SeckillGoodsMapper, SeckillGoods>
    implements SeckillGoodsService{

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RabbitService rabbitService;

    @Autowired
    private SeckillGoodsService seckillGoodsService;

    @Override
    public List<SeckillGoods> findAll() {
        return redisTemplate.boundHashOps(RedisConst.SECKILL_GOODS).values();
    }

    @Override
    public SeckillGoods getSeckillGoods(Long skuId) {
        return (SeckillGoods) redisTemplate.boundHashOps(RedisConst.SECKILL_GOODS).get(skuId.toString());
    }

    /**
     * 预下单校验
     * @param skuId
     * @param skuIdStr
     * @param userId
     * @return
     */
    @Override
    public Result seckillOrder(Long skuId, String skuIdStr, String userId) {
        //  校验抢购码
        if (!skuIdStr.equals(MD5.encrypt(userId))){
            return Result.fail().message("校验抢购码失败");
        }
        //  校验状态位 存储map 中
        String status = (String) CacheHelper.get(skuId.toString());
        if (StringUtils.isEmpty(status)){
            return Result.fail().message("非法请求");
        }else if ( "0".equals(status)){
            return Result.fail().message("商品已售罄");
        }else {
            //  可以秒杀
            //  创建对象存储谁购买的商品
            UserRecode userRecode = new UserRecode();
            userRecode.setSkuId(skuId);
            userRecode.setUserId(userId);
            //  将这个对象放入到队列中.
            this.rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_SECKILL_USER,MqConst.ROUTING_SECKILL_USER,userRecode);
            //  初步预下单成功
            return Result.ok();
        }
    }

    /**
     * 监听预下单数据处理
     * @param userRecode
     */
    @Override
    public void seckillUser(UserRecode userRecode) {
        // 1. 校验状态位
        String status = (String) CacheHelper.get(userRecode.getSkuId().toString());
        if (StringUtils.isEmpty(status) || "0".equals(status)){
            return;
        }
        // 2. 判断用户是否下过订单。通过redis setnx key value;
        /* 规则
          key = seckill:user:userId = 一个用户只能秒杀一件商品
          key = seckill:user:userId:skuId = 一个用户能秒杀不同的商品
         */
        String userKey = RedisConst.SECKILL_USER+userRecode.getUserId()+userRecode.getSkuId();
        Boolean exist = this.redisTemplate.opsForValue().setIfAbsent(userKey, userRecode.getUserId(), RedisConst.SECKILL__TIMEOUT, TimeUnit.SECONDS);
        if (!exist){
            //  表示当前用户已经购买过这个商品了.
            return;
        }
        // 3. 校验redis 中是否有足够的库存！ 通过redis-list; rpop key
        //  this.redisTemplate.opsForList().leftPush(RedisConst.SECKILL_STOCK_PREFIX+seckillGoods.getSkuId(),seckillGoods.getSkuId().toString());
        String existSkuId = (String) this.redisTemplate.opsForList().rightPop(RedisConst.SECKILL_STOCK_PREFIX + userRecode.getSkuId());
        if (StringUtils.isEmpty(existSkuId)){
            //  说明当前商品已经售罄，发布消息更新状态码
            this.redisTemplate.convertAndSend("seckillpush",userRecode.getSkuId()+":0");
            return;
        }
        // 4. 上述校验都成功，保存预下单数据.
        OrderRecode orderRecode = new OrderRecode();
        orderRecode.setUserId(userRecode.getUserId());
        //  防止超卖每次给1件
        orderRecode.setNum(1);
        orderRecode.setSeckillGoods(this.getSeckillGoods(userRecode.getSkuId()));
        //  下单码
        orderRecode.setOrderStr(MD5.encrypt(userRecode.getUserId()+userRecode.getSkuId()));
        //  将上述对象存到缓存中 数据类型： hset key field value;
        this.redisTemplate.opsForHash().put(RedisConst.SECKILL_ORDERS,userRecode.getUserId(),orderRecode);

        // 5. 修改剩余库存数量. 异步：发送消息！ redis + mysql
        this.rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_SECKILL_STOCK,MqConst.ROUTING_SECKILL_STOCK,userRecode.getSkuId());
    }

    /**
     * 秒杀减库存
     * @param skuId
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void seckillStock(Long skuId) {
        try {
            //  mysql + redis
            //  1. 获取当前剩余库存 （hget key field;）
            Long stockCount = this.redisTemplate.opsForList().size(RedisConst.SECKILL_STOCK_PREFIX + skuId);
            SeckillGoods seckillGoods = (SeckillGoods) this.redisTemplate.opsForHash().get(RedisConst.SECKILL_GOODS, skuId.toString());
            //  2. 修改剩余库存
            seckillGoods.setStockCount(stockCount.intValue());
            //  3. 修改数据库：
            seckillGoodsService.updateById(seckillGoods);
            //  hset key field value;
            this.redisTemplate.opsForHash().put(RedisConst.SECKILL_GOODS, skuId.toString(),seckillGoods);
            log.info("修改库存成功 {}" , skuId );
        } catch (Exception e) {
            log.error("修改库存失败 {}" , skuId );
            throw new RuntimeException(e);
        }
    }

    /**
     * 检查订单状态
     * @param skuId
     * @param userId
     * @return
     */
    @Override
    public Result checkOrder(Long skuId, String userId) {
        //  1.  判断用户是否在缓存中存在
        String userKey = RedisConst.SECKILL_USER+userId+skuId;
        Boolean exist = this.redisTemplate.hasKey(userKey);
        //  exist = true; 说明用户在缓存中存在;
        if (exist){
            //  2.  判断用户是否抢单成功  --> 有预下单数据
            //  this.redisTemplate.opsForHash().put(RedisConst.SECKILL_ORDERS,userRecode.getUserId(),orderRecode);
            OrderRecode orderRecode = (OrderRecode) this.redisTemplate.opsForHash().get(RedisConst.SECKILL_ORDERS, userId);
            if (orderRecode!=null){
                //  说明抢购成功.
                return Result.build(orderRecode, ResultCodeEnum.SECKILL_SUCCESS);
            }
        }

        //  3.  判断用户是否下过订单 用户已经提交过订单！ 存储一个真正下单的key
        //  key = seckill:orders:users field = userId  value = orderId  hget key field;
        String orderKey = RedisConst.SECKILL_ORDERS_USERS;
        String orderIdStr = (String) this.redisTemplate.opsForHash().get(orderKey, userId);
        //  有下过订单记录
        if (!StringUtils.isEmpty(orderIdStr)){
            //  说明已经下过订单
            return Result.build(orderIdStr, ResultCodeEnum.SECKILL_ORDER_SUCCESS);
        }

        //  4.  判断状态位
        String status = (String) CacheHelper.get(skuId.toString());
        if (StringUtils.isEmpty(status) || "0".equals(status)){
            //  返回下单失败信息提示
            return Result.build(orderIdStr, ResultCodeEnum.SECKILL_FAIL);
        }
        //  默认
        return Result.build(orderIdStr, ResultCodeEnum.SECKILL_RUN);
    }

    /**
     * 汇总秒杀结算页面信息数据
     * @param userId
     * @return
     */
    @Override
    public Map<String, Object> seckillTradeData(String userId) {
        // 需要获取：detailArrayList: 订单明细 OrderDetail  totalAmount totalNum
        Map<String, Object> map = new HashMap<>();
        //  通过userId 可以获取到预下单数据记录.
        OrderRecode orderRecode = (OrderRecode) this.redisTemplate.opsForHash().get(RedisConst.SECKILL_ORDERS, userId);
        //  判断
        if (orderRecode == null){
            throw new RuntimeException("缓存中不存在预下单数据.");
        }
        //  获取秒杀商品对象
        SeckillGoods seckillGoods = orderRecode.getSeckillGoods();
        //  需要将秒杀商品变为 OrderDetail;
        OrderDetail orderDetail = new OrderDetail();
        orderDetail.setSkuId(seckillGoods.getSkuId());
        orderDetail.setSkuName(seckillGoods.getSkuName());
        orderDetail.setSkuNum(orderRecode.getNum());
        orderDetail.setOrderPrice(seckillGoods.getCostPrice());
        orderDetail.setImgUrl(seckillGoods.getSkuDefaultImg());
        //  声明一个集合来存储订单明细
        ArrayList<OrderDetail> detailArrayList = new ArrayList<>();
        detailArrayList.add(orderDetail);
        //  map 存储数据
        map.put("detailArrayList",detailArrayList);
        //  存储总价格 每次秒杀商品只有1个; 一个商品的总价
        map.put("totalAmount",seckillGoods.getCostPrice());
        map.put("totalNum",orderRecode.getNum());
        //  返回map 数据
        return map;
    }
}
