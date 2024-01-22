package com.atguigu.gmall.cart.service;

import com.atguigu.gmall.model.cart.CartInfo;

import java.util.List;

/**
 * 异步操作
 */
public interface CartAsyncService {

    /**
     * 异步 添加或修改
     * @param list
     */
    void saveOrUpdateBatch(List<CartInfo> list);

    /**
     * 异步操作——删除
     * @param userId
     */
    void remove(String userId);

    /**
     * 异步操作——删除单个购物车商品
     * @param userId
     * @param skuId
     */
    void removeCartInfo(String userId, Long skuId);

    /**
     *  异步操作——修改购物车商品选中状态
     * @param userId
     * @param skuId
     * @param isChecked
     */
    void updateCartCheck(String userId, Long skuId, Integer isChecked);
}
