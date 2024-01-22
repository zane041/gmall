package com.atguigu.gmall.product.client;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.product.client.impl.ProductDegradeFeignClient;
import com.atguigu.gmall.model.product.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * value：所调用的微服务的名称
 * fallback：当远程调用失败的时候会走熔断类
 */
@FeignClient(value = "service-product",fallback = ProductDegradeFeignClient.class)
public interface ProductFeignClient {

    /**
     * 根据SkuID查询SKU商品信息包含图片列表
     * @param skuId
     * @return
     */
    // 注意这里的映射路径要补全
    @GetMapping("/api/product/inner/getSkuInfo/{skuId}")
    SkuInfo getSkuInfo(@PathVariable Long skuId);

    /**
     * 根据三级分类id 查询分类数据
     * @param category3Id
     * @return
     */
    @GetMapping("/api/product/inner/getCategoryView/{category3Id}")
    BaseCategoryView getCategoryView(@PathVariable Long category3Id);

    /**
     * 根据SKUID查询商品最新价格
     * @param skuId
     * @return
     */
    @GetMapping("/api/product/inner/getSkuPrice/{skuId}")
    BigDecimal getSkuPrice(@PathVariable Long skuId);

    /**
     * 根据spuId 获取海报信息
     * @param spuId
     * @return
     */
    @GetMapping("/api/product/inner/findSpuPosterBySpuId/{spuId}")
    List<SpuPoster> getSpuPosterBySpuId(@PathVariable Long spuId);

    /**
     * 根据SkuID查询当前商品包含平台属性以及属性值
     * @param skuId
     * @return
     */
    @GetMapping("/api/product/inner/getAttrList/{skuId}")
    List<BaseAttrInfo> getAttrList(@PathVariable Long skuId);

    /**
     * 根据spuId-skuId 查询销售属性数据
     * @param skuId
     * @param spuId
     * @return
     */
    @GetMapping("/api/product/inner/getSpuSaleAttrListCheckBySku/{skuId}/{spuId}")
    List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(@PathVariable("skuId") Long skuId,
                                                          @PathVariable("spuId") Long spuId);
    /**
     * 提供商品切换所需的对照表
     * @param spuId
     * @return
     */
    @GetMapping("/api/product/inner/getSkuValueIdsMap/{spuId}")
    Map getSkuValueIdsMap(@PathVariable Long spuId);

    /**
     * 获取全部分类信息
     * @return
     */
    @GetMapping("/api/product/getBaseCategoryList")
    List<JSONObject> getBaseCategoryList();

    /**
     * 根据品牌ID查询品牌信息
     *
     * @param tmId 品牌ID
     * @return
     */
    @GetMapping("/api/product/inner/getTrademark/{tmId}")
    public BaseTrademark getTrademarkById(@PathVariable("tmId") Long tmId);
}
