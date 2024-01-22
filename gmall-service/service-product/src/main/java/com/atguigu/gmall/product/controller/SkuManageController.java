package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.product.service.SkuInfoService;
import com.atguigu.gmall.product.service.SkuManageService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Api(tags = "Sku商品管理控制器")
@RestController
@RequestMapping("/admin/product")
public class SkuManageController {

    @Autowired
    private SkuManageService skuManageService;

    @Autowired
    private SkuInfoService skuInfoService;

    /**
     * 保存Sku
     * @param skuInfo
     * @return
     */
    @PostMapping("/saveSkuInfo")
    public Result saveSkuInfo(@RequestBody SkuInfo skuInfo) {
        skuManageService.saveSkuInfo(skuInfo);
        return Result.ok();
    }

    /**
     * 获取Sku分页列表
     * @param page
     * @param limit
     * @return
     */
    @GetMapping("/list/{page}/{limit}")
    public Result getSkuListByPage(@PathVariable("page") Long page,
                                   @PathVariable("limit") Long limit,
                                   @RequestParam("category3Id") Long category3Id) {
        Page<SkuInfo> skuInfoPage = new Page<SkuInfo>(page, limit);
        IPage<SkuInfo> iPage = skuManageService.getSkuListByPage(skuInfoPage, category3Id);
        return Result.ok(iPage);
    }

    /**
     * sku上架
     * @param skuId
     * @return
     */
    @GetMapping("/onSale/{skuId}")
    public Result onSale(@PathVariable Long skuId) {
        skuManageService.onsale(skuId);
        return Result.ok();
    }

    /**
     * sku下架
     * @param skuId
     * @return
     */
    @GetMapping("/cancelSale/{skuId}")
    public Result cancelSale(@PathVariable Long skuId) {
        skuManageService.cancelSale(skuId);
        return Result.ok();
    }

    /**
     * 回显sku信息
     * @param skuId
     * @return
     */
    @GetMapping("getSkuInfo/{skuId}")
    public Result getSkuInfo(@PathVariable Long skuId) {
        SkuInfo skuInfo = skuInfoService.getById(skuId);
        return Result.ok(skuInfo);
    }

    @PostMapping("/updateSkuInfo")
    public Result updateSkuInfo(@RequestBody SkuInfo skuInfo) {
        skuManageService.saveSkuInfo(skuInfo);
        return Result.ok();
    }

}
