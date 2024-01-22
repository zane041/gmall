package com.atguigu.gmall.activity.service;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.activity.SeckillGoods;
import com.atguigu.gmall.model.activity.UserRecode;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

/**
* @author admin
* @description 针对表【seckill_goods】的数据库操作Service
* @createDate 2024-01-07 21:46:20
*/
public interface SeckillGoodsService extends IService<SeckillGoods> {

    /**
     * 查询当日参与秒杀商品列表
     * @return
     */
    List<SeckillGoods> findAll();

    /**
     * 查询指定秒杀商品信息
     * @param skuId
     * @return
     */
    SeckillGoods getSeckillGoods(Long skuId);

    /**
     * 预下单处理
     * @param skuId
     * @param skuIdStr
     * @param userId
     * @return
     */
    Result seckillOrder(Long skuId, String skuIdStr, String userId);

    /**
     * 监听秒杀用户列表
     * @param userRecode
     */
    void seckillUser(UserRecode userRecode);

    /**
     * 实现减库存
     * @param skuId
     */
    void seckillStock(Long skuId);

    /**
     * 检查订单状态
     * @param skuId
     * @param userId
     * @return
     */
    Result checkOrder(Long skuId, String userId);

    /**
     * 获取订单结算页数据
     * @param userId
     * @return
     */
    Map<String, Object> seckillTradeData(String userId);
}
