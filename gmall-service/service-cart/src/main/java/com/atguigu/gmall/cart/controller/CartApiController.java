package com.atguigu.gmall.cart.controller;

import com.atguigu.gmall.cart.service.CartService;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.util.AuthContextHolder;
import com.atguigu.gmall.model.cart.CartInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/api/cart")
public class CartApiController {

    @Autowired
    private CartService cartService;

    /**
     * 添加购物车
     * @param skuId
     * @param skuNum
     * @param request
     * @return
     */
    @GetMapping("/addToCart/{skuId}/{skuNum}")
    public Result addToCart(@PathVariable Long skuId,
                            @PathVariable Integer skuNum,
                            HttpServletRequest request){
        String userId = getUserId(request);
        cartService.addToCart(skuId, skuNum, userId);
        return Result.ok();
    }

    /**
     * 获取用户id。已登录——用户id，未登录——临时用户id
     * @param request
     * @return
     */
    private static String getUserId(HttpServletRequest request) {
        // 获取用户id
        String userId = AuthContextHolder.getUserId(request);
        // 如果没有 userId，则获取临时id userTempId
        if (StringUtils.isEmpty(userId)) userId = AuthContextHolder.getUserTempId(request);
        return userId;
    }

    /**
     * 查询用户购物车列表
     * 版本1：分别查询未登录购物车列表，以及登录的购物车列表
     * 版本2：将两个购物车中商品合并
     * @param request
     * @return
     */
    @GetMapping("/cartList")
    public Result<List<CartInfo>> cartList(HttpServletRequest request){
        String userId = AuthContextHolder.getUserId(request);
        String userTempId = AuthContextHolder.getUserTempId(request);
        List<CartInfo> cartInfoList = cartService.cartList(userId, userTempId);
        return Result.ok(cartInfoList);
    }

    /**
     * 更新购物车商品选中状态
     * @param skuId
     * @param isChecked
     * @param request
     * @return
     */
    @GetMapping("checkCart/{skuId}/{isChecked}")
    public Result checkCart(@PathVariable Long skuId,
                            @PathVariable Integer isChecked,
                            HttpServletRequest request){
        String userId = getUserId(request);
        cartService.checkCart(userId, skuId, isChecked);
        return Result.ok();
    }

    /**
     * 清空购物车.
     * @return
     */
    @GetMapping("clearCart")
    public Result clearCart(HttpServletRequest request){
        String userId = getUserId(request);
        //  调用服务层方法.
        this.cartService.clearCart(userId);
        return Result.ok();
    }

    /**
     * 删除单个购物车商品
     * @param request
     * @return
     */
    @DeleteMapping("deleteCart/{skuId}")
    public Result deleteCart(@PathVariable("skuId") Long skuId,
                             HttpServletRequest request) {
        String userId = getUserId(request);
        cartService.deleteCart(skuId, userId);
        return Result.ok();
    }

    /**
     * 查询用户购物车中已勾选的商品列表
     * @param userId
     * @return
     */
    @GetMapping("/getCartCheckedList/{userId}")
    public List<CartInfo> getCartCheckedList(@PathVariable("userId") Long userId){
        List<CartInfo> list = cartService.getCartCheckedList(userId);
        return list;
    }

}
