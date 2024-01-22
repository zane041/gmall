package com.atguigu.gmall.item.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.item.service.ItemService;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Api(tags = "商品详情页内部数据接口")
@RestController
@RequestMapping("/api/item")
public class ItemApiController {

    @Autowired
    private ItemService itemService;

    /**
     * 获取商品详情页数据 （web-all使用）
     * @param skuId
     * @return
     */
    @GetMapping("/{skuId}")
    public Result getItem(@PathVariable Long skuId) {
        Map map = itemService.getItem(skuId);
        return Result.ok(map);
    }
}
