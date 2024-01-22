package com.atguigu.gmall.product.service;

import com.atguigu.gmall.model.product.BaseAttrInfo;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.model.product.SpuSaleAttr;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface SkuManageService {

    /**
     * 保存或者更新Sku
     * @param skuInfo
     */
    void saveSkuInfo(SkuInfo skuInfo);

    /**
     * 获取Sku分页列表
     * @param skuInfoPage
     * @param category3Id
     * @return
     */
    IPage<SkuInfo> getSkuListByPage(Page<SkuInfo> skuInfoPage, Long category3Id);

    /**
     * 获取SkuInfo 包含图片列表
     * @param skuId
     * @return
     */
    SkuInfo getSkuInfo(Long skuId);

    /**
     * 根据SKUID查询商品最新价格
     * @param skuId
     * @return
     */
    BigDecimal getSkuPrice(Long skuId);

    /**
     * 根据SkuID查询当前商品包含平台属性以及属性值
     * @param skuId
     * @return
     */
    List<BaseAttrInfo> getAttrList(Long skuId);

    /**
     * 查询当前商品所有的销售属性,判断为当前SKU拥有销售属性增加选中效果
     * @param skuId
     * @param spuId
     * @return
     */
    List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(Long skuId, Long spuId);

    Map getSkuValueIdsMap(Long spuId);

    /**
     * 商品上架
     * @param skuId
     */
    void onsale(Long skuId);

    /**
     * 商品下架
     * @param skuId
     */
    void cancelSale(Long skuId);
}
