package com.atguigu.gmall.item.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.list.client.ListFeignClient;
import com.atguigu.gmall.product.client.ProductFeignClient;
import com.atguigu.gmall.item.service.ItemService;
import com.atguigu.gmall.model.product.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

@Service
public class ItemServiceImpl implements ItemService {

    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

    @Autowired
    private ListFeignClient listFeignClient;

    @Override
    public Map getItem(Long skuId) {
        // 创建响应结果map
        Map result = new HashMap<>();

        //1.根据skuId查询商品Sku信息包含商品图片 得到SkuInfo 创建带返回值异步对象
        CompletableFuture<SkuInfo> skuInfoCompletableFuture = CompletableFuture.supplyAsync(() -> {
            SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
            if (Objects.nonNull(skuInfo)) result.put("skuInfo", skuInfo);
            return skuInfo;
        }, threadPoolExecutor);

        //2.根据商品所属三级分类Id查询分类对象信息 创建任务该任务获取前面sku信息任务返回结果，当前任务不需要返回值
        CompletableFuture<Void> categoryViewCompletableFuture = skuInfoCompletableFuture.thenAcceptAsync(skuInfo -> {
            if (Objects.nonNull(skuInfo)) {
                BaseCategoryView categoryView = productFeignClient.getCategoryView(skuInfo.getCategory3Id());
                if (Objects.nonNull(categoryView)) result.put("categoryView", categoryView);
            }
        }, threadPoolExecutor);

        //3.根据商品skuId查询商品价格
        CompletableFuture<Void> priceCompletableFuture = CompletableFuture.runAsync(() -> {
            BigDecimal skuPrice = productFeignClient.getSkuPrice(skuId);
            if (Objects.nonNull(skuPrice)) result.put("price", skuPrice);
        }, threadPoolExecutor);

        //4.根据spuId查询商品海报图片列表
        CompletableFuture<Void> posterCompletableFuture = skuInfoCompletableFuture.thenAcceptAsync(skuInfo -> {
            if (Objects.nonNull(skuInfo)) {
                List<SpuPoster> spuPosterList = productFeignClient.getSpuPosterBySpuId(skuInfo.getSpuId());
                if (Objects.nonNull(spuPosterList)) result.put("spuPosterList", spuPosterList);
            }
        }, threadPoolExecutor);

        //5.根据skuId查询平台属性以及平台属性值
        CompletableFuture<Void> attrCompletableFuture = CompletableFuture.runAsync(() -> {
            List<BaseAttrInfo> attrList = productFeignClient.getAttrList(skuId);
            if (!CollectionUtils.isEmpty(attrList)) {
                // 前台只需要attrName, attrValue; 所以我们在返回数据之前要转化一下
                List<HashMap<String, String>> mapList = attrList.stream().map(baseAttrInfo -> {
                    HashMap<String, String> hashMap = new HashMap<>();
                    hashMap.put("attrName", baseAttrInfo.getAttrName());
                    hashMap.put("attrValue", baseAttrInfo.getAttrValueList().get(0).getValueName());
                    return hashMap;
                }).collect(Collectors.toList());
                result.put("skuAttrList", mapList);
            }
        }, threadPoolExecutor);

        //6.根据spuID,skuID查询所有销售属性，以及当前sku选中销售属性
        CompletableFuture<Void> saleAttrCompletableFuture = skuInfoCompletableFuture.thenAcceptAsync(skuInfo -> {
            if (Objects.isNull(skuInfo)) return;
            List<SpuSaleAttr> spuSaleAttrList = productFeignClient.getSpuSaleAttrListCheckBySku(skuId, skuInfo.getSpuId());
            if (!CollectionUtils.isEmpty(spuSaleAttrList)) result.put("spuSaleAttrList", spuSaleAttrList);
        }, threadPoolExecutor);

        //7.根据spuID查询销售属性属性值对应sku信息Map {"销售属性1|销售属性2":"skuId"} TODO 注意要将map转为JSON
        CompletableFuture<Void> skuMapCompletableFuture = skuInfoCompletableFuture.thenAcceptAsync(skuInfo -> {
            if (Objects.isNull(skuInfo)) return;
            Map skuValueIdsMap = productFeignClient.getSkuValueIdsMap(skuInfo.getSpuId());
            if (!CollectionUtils.isEmpty(skuValueIdsMap))
                result.put("valuesSkuJson", JSON.toJSONString(skuValueIdsMap));
        }, threadPoolExecutor);

        // 8.远程调用检索微服务，更新ES索引库中商品文档的热门分值
        CompletableFuture<Void> incrHotScoreCompletableFuture = CompletableFuture.runAsync(() -> {
            listFeignClient.incrHotScore(skuId);
        }, threadPoolExecutor);

        //9.将以上八个任务全部并行执行，执行完所有任务才返回
        CompletableFuture.allOf(
                skuInfoCompletableFuture,
                categoryViewCompletableFuture,
                priceCompletableFuture,
                posterCompletableFuture,
                attrCompletableFuture,
                saleAttrCompletableFuture,
                skuMapCompletableFuture,
                incrHotScoreCompletableFuture
        ).join();

        return result;
    }
}
