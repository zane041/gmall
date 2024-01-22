package com.atguigu.gmall.cart.service.impl;

import com.atguigu.gmall.cart.service.CartService;
import com.atguigu.gmall.model.cart.CartInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.context.annotation.Import;

import java.util.ArrayList;
import java.util.List;

@SpringBootTest
@Import({ FeignAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class })
//@EnableFeignClients(basePackages = "com.atguigu.gmall")
class CartServiceImplTest {

    @Autowired
    private CartService cartService;

    @Test
    void addToCart() {
        /*
            测试一：添加数据比如skuid = 33。查看redis， mysql是否都添加了数据
                发现 redis中 cartInfo的id为空 -> 正常，因为异步进行，没关系因为再添加一次该sku又会更新缓存给其赋值的。
            测试二：更改已加入购物车的一条数据，看其 skuNum有没有更新。
            测试三：测试事务
            测试四：如果缓存key过期即缓存没有数据，执行后数据库和缓存数据是否正确
         */
        Long skuId = 1L;
        Integer skuNum = 1;
        String userId = "1";
        cartService.addToCart(skuId, skuNum, userId);
    }

    @Test
    void cartList() {
        String userId = "3";
        String tempId = "3f79ca269ea94aa49e81c66505c92062";
//        String userId = "1";
//        String tempId = "2";
        List<CartInfo> cartInfoList = cartService.cartList(userId, tempId);
        for (CartInfo cartInfo : cartInfoList) {
            System.out.println(cartInfo.toString() + "__" + cartInfo.getUpdateTime().toString());
        }
    }

    @Test
    void checkCart() {

        cartService.checkCart("1", 2L, 0);
    }

    @Test
    void test() {
        ArrayList<Integer> list = new ArrayList();
        list.sort((o1, o2) -> {
            return (int) (o1 - o2);
        });
    }
}