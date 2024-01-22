package com.atguigu.gmall.product.service;

import com.alibaba.fastjson.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
class ManageServiceTest {

    @Autowired
    private ManageService manageService;

    @Test
    void getBaseCategoryList() {
        List<JSONObject> baseCategoryList = manageService.getBaseCategoryList();
        baseCategoryList.stream().forEach(jsonObject -> {
            System.out.println(jsonObject.toString());
        });
    }
}