package com.atguigu.gmall.item.service;

import java.util.Map;

public interface ItemService {

    /**
     * 获取商品详情页数据。（商品信息，平台分类，最新价格，海报信息，平台属性，销售属性，切换功能数据）
     * @param skuId
     * @return
     */
    Map getItem(Long skuId);
}
