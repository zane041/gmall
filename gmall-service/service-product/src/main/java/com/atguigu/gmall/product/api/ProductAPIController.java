package com.atguigu.gmall.product.api;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.service.BaseTrademarkService;
import com.atguigu.gmall.product.service.ManageService;
import com.atguigu.gmall.product.service.SkuManageService;
import com.atguigu.gmall.product.service.SpuManageService;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Api(tags = "商品模块面向微服务接口")
@RestController
@RequestMapping("/api/product")
public class ProductAPIController {

    @Autowired
    private SkuManageService skuManageService;

    @Autowired
    private ManageService manageService;

    @Autowired
    private SpuManageService spuManageService;

    @Autowired
    private BaseTrademarkService baseTrademarkService;

    /**
     * 根据SkuID查询SKU商品信息包含图片列表
     * @param skuId
     * @return
     */
    @GetMapping("/inner/getSkuInfo/{skuId}")
    public SkuInfo getSkuInfo(@PathVariable Long skuId) {
        return skuManageService.getSkuInfo(skuId);
    }

    /**
     * 根据三级分类id 查询分类数据
     * @param category3Id
     * @return
     */
    @GetMapping("/inner/getCategoryView/{category3Id}")
    public BaseCategoryView getCategoryView(@PathVariable Long category3Id) {
        return manageService.getCategoryView(category3Id);
    }

    /**
     * 根据SKUID查询商品最新价格
     * @param skuId
     * @return
     */
    @GetMapping("/inner/getSkuPrice/{skuId}")
    public BigDecimal getSkuPrice(@PathVariable Long skuId) {
        return skuManageService.getSkuPrice(skuId);
    }

    /**
     * 根据spuId 获取海报信息
     * @param spuId
     * @return
     */
    @GetMapping("/inner/findSpuPosterBySpuId/{spuId}")
    public List<SpuPoster> getSpuPosterBySpuId(@PathVariable Long spuId){
        //  调用服务层方法.
        return spuManageService.getSpuPosterBySpuId(spuId);
    }

    /**
     * 根据SkuID查询当前商品包含平台属性以及属性值
     * @param skuId
     * @return
     */
    @GetMapping("/inner/getAttrList/{skuId}")
    public List<BaseAttrInfo> getAttrList(@PathVariable Long skuId) {
        return skuManageService.getAttrList(skuId);
    }

    /**
     * 根据spuId-skuId 查询销售属性数据
     * @param skuId
     * @param spuId
     * @return
     */
    @GetMapping("/inner/getSpuSaleAttrListCheckBySku/{skuId}/{spuId}")
    public List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(@PathVariable("skuId") Long skuId,
                                                          @PathVariable("spuId") Long spuId) {
        return skuManageService.getSpuSaleAttrListCheckBySku(skuId, spuId);
    }

    /**
     * 提供商品切换所需的对照表
     * @param spuId
     * @return
     */
    @GetMapping("/inner/getSkuValueIdsMap/{spuId}")
    public Map getSkuValueIdsMap(@PathVariable Long spuId) {
        return skuManageService.getSkuValueIdsMap(spuId);
    }

    /**
     * 获取首页分类数据
     * @return
     */
    @GetMapping("/getBaseCategoryList")
    public List<JSONObject> getBaseCategoryList(){
        //  调用服务层方法
        return manageService.getBaseCategoryList();
    }

    /**
     * 根据品牌ID查询品牌信息
     * @param tmId
     * @return
     */
    @GetMapping("/inner/getTrademark/{tmId}")
    public BaseTrademark getTrademarkById(@PathVariable Long tmId) {
        return baseTrademarkService.getById(tmId);
    }
}
