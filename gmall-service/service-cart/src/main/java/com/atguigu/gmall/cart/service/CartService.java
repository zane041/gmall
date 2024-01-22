package com.atguigu.gmall.cart.service;

import com.atguigu.gmall.model.cart.CartInfo;

import java.util.List;

/**
 * 购物车业务
 */
public interface CartService {

    /**
     * 添加购物车
     * @param skuId
     * @param skuNum
     * @param userId
     */
    void addToCart(Long skuId, Integer skuNum, String userId);

    /**
     * 查询用户购物车列表
     *  未登录：查询未登录购物车列表
     *  已登录：查询登录购物车列表与未登录购物车列表合并后，删除未登录购物车
     * @param userId
     * @param userTempId
     * @return
     */
    List<CartInfo> cartList(String userId, String userTempId);

    /**
     * 更改购物车商品选中状态
     * @param userId
     * @param skuId
     * @param isChecked
     */
    void checkCart(String userId, Long skuId, Integer isChecked);

    /**
     * 清空购物车
     * @param userId
     */
    void clearCart(String userId);

    /**
     * 删除购物车商品
     * @param skuId
     * @param userId
     */
    void deleteCart(Long skuId, String userId);

    /**
     * 查询用户购物车中已勾选的商品列表
     * @param userId
     * @return
     */
    List<CartInfo> getCartCheckedList(Long userId);
}
