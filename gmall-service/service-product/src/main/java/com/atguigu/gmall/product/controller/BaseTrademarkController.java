package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.BaseTrademark;
import com.atguigu.gmall.product.service.BaseTrademarkService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@Api(tags = "品牌表控制器")
@RestController
@RequestMapping("/admin/product")
public class BaseTrademarkController {

    @Autowired
    private BaseTrademarkService baseTrademarkService;

    /**
     * 分页查询品牌信息
     * @param page
     * @param limit
     * @return
     */
    @GetMapping("/baseTrademark/{page}/{limit}")
    public Result getBaseTrademarkByPage(@PathVariable("page") Long page,
                                         @PathVariable("limit") Long limit) {
        IPage<BaseTrademark> baseTrademarkPage = new Page<>(page, limit);
        IPage<BaseTrademark> ipage = baseTrademarkService.page(baseTrademarkPage, new QueryWrapper<BaseTrademark>().orderByDesc("id"));
        return Result.ok(ipage);
    }

    /**
     * 保存品牌
     * @param baseTrademark
     * @return
     */
    @PostMapping("/baseTrademark/save")
    public Result saveBaseTrademark(@RequestBody BaseTrademark baseTrademark) {
        boolean isSaved = baseTrademarkService.save(baseTrademark);
        if (isSaved) return Result.ok();
        else return Result.fail();
    }

    /**
     * 更新品牌
     * @param baseTrademark
     * @return
     */
    @PutMapping("/baseTrademark/update")
    public Result updateBaseTrademark(@RequestBody BaseTrademark baseTrademark) {
        if (Objects.isNull(baseTrademark) || Objects.isNull(baseTrademark.getId()) || baseTrademark.getId() <= 0) {
            return Result.fail().message("内容不能为空且id不能为空");
        }
        boolean isSucceeded = baseTrademarkService.updateById(baseTrademark);
        if (isSucceeded) return Result.ok();
        else return Result.fail();
    }

    /**
     * 删除品牌
     * @param id
     * @return
     */
    @DeleteMapping("/baseTrademark/remove/{id}")
    public Result deleteBaseTrademark(@PathVariable("id") Long id) {
        boolean isSucceeded = baseTrademarkService.removeById(id);
        if (isSucceeded) return Result.ok();
        else return Result.fail();
    }

    /**
     * 根据品牌id查询品牌信息
     * @param id
     * @return
     */
    @GetMapping("/baseTrademark/get/{id}")
    public Result getBaseTrademarkById(@PathVariable("id") Long id) {
        return Result.ok(baseTrademarkService.getById(id));
    }


}
