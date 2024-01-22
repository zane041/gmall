package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.BaseCategoryTrademark;
import com.atguigu.gmall.model.product.CategoryTrademarkVo;
import com.atguigu.gmall.product.service.BaseCategoryTrademarkService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Api(tags = "分类品牌控制器")
@RestController
@RequestMapping("/admin/product/baseCategoryTrademark")
public class BaseCategoryTrademarkController {

    @Autowired
    private BaseCategoryTrademarkService baseCategoryTrademarkService;

    /**
     * 根据三级分类Id获取品牌列表
     * @param category3Id
     * @return
     */
    @GetMapping("/findTrademarkList/{category3Id}")
    public Result findTrademarkList(@PathVariable("category3Id") Long category3Id) {
        return Result.ok(baseCategoryTrademarkService.getByCategory3Id(category3Id));
    }

    /**
     * 获取未被该三级分类绑定的品牌列表
     * @param category3Id
     * @return
     */
    @GetMapping("/findCurrentTrademarkList/{category3Id}")
    public Result findCurrentTrademarkList(@PathVariable("category3Id") Long category3Id) {
        return Result.ok(baseCategoryTrademarkService.findCurrentTrademarkList(category3Id));
    }

    /**
     * 保存分类id与品牌的关系
     * @param categoryTrademarkVo
     * @return
     */
    @PostMapping("/save")
    public Result saveCategoryTrademarkList(@RequestBody CategoryTrademarkVo categoryTrademarkVo) {
        baseCategoryTrademarkService.save(categoryTrademarkVo);
        return Result.ok();
    }

    /**
     * 删除分类与品牌的关系
     * @param category3Id
     * @param trademarkId
     * @return
     */
    @DeleteMapping("/remove/{category3Id}/{trademarkId}")
    public Result removeCategoryTrademark(@PathVariable("category3Id") Long category3Id,
                                          @PathVariable("trademarkId") Long trademarkId) {
        baseCategoryTrademarkService.remove(new LambdaQueryWrapper<BaseCategoryTrademark>().eq(BaseCategoryTrademark::getCategory3Id, category3Id)
                .eq(BaseCategoryTrademark::getTrademarkId, trademarkId));
        return Result.ok();
    }

}
