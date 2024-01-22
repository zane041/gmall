package com.atguigu.gmall.web.controller;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class IndexController {

    @Autowired
    private ProductFeignClient productFeignClient;

    @GetMapping({"/", "/index.html"})
    public String index(Model model) {
        List<JSONObject> list = productFeignClient.getBaseCategoryList();
        model.addAttribute("list", list);
        return "/index/index.html";
    }
}
