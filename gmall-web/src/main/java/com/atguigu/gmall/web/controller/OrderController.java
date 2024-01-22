package com.atguigu.gmall.web.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.order.client.OrderFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

/**
 * @author: atguigu
 * @create: 2023-01-12 10:56
 */
@Controller
public class OrderController {

    @Autowired
    private OrderFeignClient orderFeignClient;

    /**
     * 订单确认页面渲染
     * @param model
     * @return
     */
    @GetMapping("/trade.html")
    public String trade(Model model){
        Result<Map<String, Object>> result = orderFeignClient.trade();
        model.addAllAttributes(result.getData());
        return "order/trade";
    }

    /**
     * 我的订单
     * @return
     */
    @GetMapping("myOrder.html")
    public String myOrder() {
        return "order/myOrder";
    }
}