package com.atguigu.gmall.activity.client.impl;

import com.atguigu.gmall.activity.client.ActivityFeignClient;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.activity.SeckillGoods;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class ActivityDegradeFeignClient implements ActivityFeignClient {
    @Override
    public Result<List<SeckillGoods>> findAll() {
        return Result.fail();
    }

    @Override
    public Result<SeckillGoods> getSeckillGoods(Long skuId) {
        return Result.fail();
    }

    @Override
    public Result<Map<String, Object>> seckillTradeData() {
        return Result.fail();
    }
}
