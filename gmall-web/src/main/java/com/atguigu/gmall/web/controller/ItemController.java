package com.atguigu.gmall.web.controller;

import com.atguigu.gmall.item.client.ItemFeignClient;
import com.atguigu.gmall.common.result.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@Controller
public class ItemController {

    @Autowired
    private ItemFeignClient itemFeignClient;

    /**
     * 渲染商品详情页面
     *
     * @param skuId
     * @return
     */
    @GetMapping("/{skuId}.html")
    public String getItem(@PathVariable Long skuId, Model model) {
        Result<Map> result = itemFeignClient.getItem(skuId);
        model.addAllAttributes(result.getData());
        return "item/item";
    }
}
