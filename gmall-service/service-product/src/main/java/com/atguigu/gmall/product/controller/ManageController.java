package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.BaseAttrInfo;
import io.swagger.annotations.Api;
import com.atguigu.gmall.product.service.ManageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Api(tags = "后台管理控制器")
@RestController//@Controller{控制器}+@ResponseBody{返回数据变为Json}
@RequestMapping("/admin/product")
public class ManageController {

    @Autowired
    private ManageService manageService;

    /**
     * 查询所有一级分类数据
     * @return
     */
    @GetMapping("getCategory1")
    public Result getCategory1() {
        return Result.ok(manageService.getBaseCategory1());
    }

    /**
     * 根据一级分类Id  查询二级分类数据
     * @param category1Id
     * @return
     */
    @GetMapping("getCategory2/{category1Id}")
    public Result getCategory2(@PathVariable("category1Id") Long category1Id) {
        return Result.ok(manageService.getBaseCategory2(category1Id));
    }

    /**
     * 根据二级分类Id 查询三级分类数据
     * @param category2Id
     * @return
     */
    @GetMapping("getCategory3/{category2Id}")
    public Result getCategory3(@PathVariable("category2Id") Long category2Id) {
        return Result.ok(manageService.getBaseCategory3(category2Id));
    }

    /**
     * 根据分类Id 查询平台属性数据。
     * @param category1Id
     * @param category2Id
     * @param category3Id
     * @return
     */
    @GetMapping("attrInfoList/{category1Id}/{category2Id}/{category3Id}")
    public Result getAttrInfoList(@PathVariable("category1Id") Long category1Id,
                                  @PathVariable("category2Id") Long category2Id,
                                  @PathVariable("category3Id") Long category3Id) {
        return Result.ok(manageService.getBaseAttrInfo(category1Id, category2Id, category3Id));
    }

    /**
     * 添加、修改平台属性
     * @param baseAttrInfo
     * @return
     */
    @PostMapping("saveAttrInfo")
    public Result saveAttrInfo(@RequestBody BaseAttrInfo baseAttrInfo) {
        manageService.saveAttrInfo(baseAttrInfo);
        return Result.ok();
    }

    /**
     * 根据属性id获取属性值列表
     * @param attrId
     * @return
     */
    @GetMapping("getAttrValueList/{attrId}")
    public Result getAttrValueList(@PathVariable("attrId") Long attrId) {
        return Result.ok(manageService.getAttrValueList(attrId));
    }

    /**
    * 获取所有销售属性
    * @return
    */
    @GetMapping("baseSaleAttrList")
    public Result getBaseSaleAttrList() {
    return Result.ok(manageService.getBaseSaleAttrList());
    }

}

