package com.atguigu.gmall.list.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.list.service.SearchService;
import com.atguigu.gmall.model.list.Goods;
import com.atguigu.gmall.model.list.SearchParam;
import com.atguigu.gmall.model.list.SearchResponseVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/list")
@RestController
public class ListApiController {

    @Autowired
    private ElasticsearchRestTemplate restTemplate;

    @Autowired
    private SearchService searchService;

    /**
     * 创建 商品检索ES库的index和mapping
     * @return
     */
    @GetMapping("inner/createIndex")
    public Result createIndex() {
        restTemplate.createIndex(Goods.class);
        restTemplate.putMapping(Goods.class);
        return Result.ok();
    }

    /**
     * 上架商品将商品文档对象录入索引
     * @param skuId
     * @return
     */
    @GetMapping("/inner/upperGoods/{skuId}")
    public Result upperGoods(@PathVariable("skuId") Long skuId){
        searchService.upperGoods(skuId);
        return Result.ok();
    }


    /**
     * 下架商品将商品文档删除
     * @param skuId
     * @return
     */
    @GetMapping("/inner/lowerGoods/{skuId}")
    public Result lowerGoods(@PathVariable("skuId") Long skuId){
        searchService.lowerGoods(skuId);
        return Result.ok();
    }

    /**
     * 更新商品的热度排名分值
     * @param skuId
     * @return
     */
    @GetMapping("/inner/incrHotScore/{skuId}")
    public Result incrHotScore(@PathVariable("skuId") Long skuId){
        searchService.incrHotScore(skuId);
        return Result.ok();
    }

    /**
     * 商品检索
     *
     * @param searchParam
     * @return
     */
    @PostMapping
    public Result search(@RequestBody SearchParam searchParam) { //@RequestBody作用：将JSON数据转换为Java对象
        SearchResponseVo responseVo = searchService.search(searchParam);
        return Result.ok(responseVo);
    }
}
