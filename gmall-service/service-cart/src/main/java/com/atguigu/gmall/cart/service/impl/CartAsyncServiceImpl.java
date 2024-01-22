package com.atguigu.gmall.cart.service.impl;

import com.atguigu.gmall.cart.mapper.CartInfoMapper;
import com.atguigu.gmall.cart.service.CartAsyncService;
import com.atguigu.gmall.cart.service.CartInfoService;
import com.atguigu.gmall.model.cart.CartInfo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CartAsyncServiceImpl implements CartAsyncService {

    @Autowired
    private CartInfoMapper cartInfoMapper;

    @Autowired
    private CartInfoService cartInfoService;

    @Override
    @Async
    public void saveOrUpdateBatch(List<CartInfo> list) {
        cartInfoService.saveOrUpdateBatch(list);
    }

    @Override
    @Async
    public void remove(String userId) {
        cartInfoService.remove(new LambdaQueryWrapper<CartInfo>().eq(CartInfo::getUserId, userId));
    }

    @Override
    @Async
    public void removeCartInfo(String userId, Long skuId) {
        cartInfoService.remove(new LambdaQueryWrapper<CartInfo>()
                .eq(CartInfo::getUserId, userId)
                .eq(CartInfo::getSkuId, skuId));
    }

    @Override
    @Async
    public void updateCartCheck(String userId, Long skuId, Integer isChecked) {
        cartInfoService.update(new UpdateWrapper<CartInfo>()
                .eq("user_id", userId)
                .eq("sku_id", skuId)
                .set("is_checked", isChecked));
    }
}
