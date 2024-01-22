package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.SpuImage;
import com.atguigu.gmall.model.product.SpuInfo;
import com.atguigu.gmall.model.product.SpuSaleAttr;
import com.atguigu.gmall.product.service.SpuImageService;
import com.atguigu.gmall.product.service.SpuManageService;
import com.atguigu.gmall.product.service.SpuSaleAttrService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = "Spu商品管理控制器")
@RestController
@RequestMapping("/admin/product")
public class SpuManageController {

    @Autowired
    private SpuManageService spuManageService;

    @Autowired
    private SpuImageService spuImageService;

    @Autowired
    private SpuSaleAttrService spuSaleAttrService;

    /**
     * 分页查询商品Spu列表
     * @param page
     * @param size
     * @param category3Id
     * @return
     */
    @GetMapping("/{page}/{size}")
    public Result getSpuByPage(@PathVariable("page") Long page,
                               @PathVariable("size") Long size,
                               @RequestParam(value = "category3Id", required = false) Long category3Id) {
        IPage<SpuInfo> infoPage = new Page<>(page, size);
        infoPage = spuManageService.getSpuByPage(infoPage, category3Id);
        return Result.ok(infoPage);
    }

    /**
     * 保存商品Spu信息
     * @param spuInfo
     * @return
     */
    @PostMapping("/saveSpuInfo")
    public Result saveSpuInfo(@RequestBody SpuInfo spuInfo) {
        spuManageService.saveSpuInfo(spuInfo);
        return Result.ok();
    }

    /**
     * 更新SPU商品信息
     * @param spuInfo
     * @return
     */
    @PostMapping("/updateSpuInfo")
    public Result updateSpuInfo(@RequestBody SpuInfo spuInfo) {
        spuManageService.saveSpuInfo(spuInfo);
        return Result.ok();
    }

    /**
     * 根据spuId获取Spu完整信息
     * @param id
     * @return
     */
    @GetMapping("/getSpuInfo/{id}")
    public Result getSpuInfo(@PathVariable("id") Long id) {
        return Result.ok(spuManageService.getSpuInfo(id));
    }

    /**
     * 根据spuId获取销售属性及销售属性值
     * @param spuId
     * @return
     */
    @GetMapping("spuSaleAttrList/{spuId}")
    public Result spuSaleAttrList(@PathVariable Long spuId) {
        List<SpuSaleAttr> spuSaleAttrs = spuSaleAttrService.listBySpuId(spuId);
        return Result.ok(spuSaleAttrs);
    }

    /**
     * 根据spuId获取spu_image
     * @param spuId
     * @return
     */
    @GetMapping("/spuImageList/{spuId}")
    public Result spuImageList(@PathVariable Long spuId) {
        return Result.ok(spuImageService.list(new LambdaQueryWrapper<SpuImage>().eq(SpuImage::getSpuId, spuId)));
    }

}
