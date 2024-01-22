package com.atguigu.gmall.product.service;

import com.atguigu.gmall.model.product.SpuInfo;
import com.atguigu.gmall.model.product.SpuPoster;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;

public interface SpuManageService {


    /**
     * 分页查询商品SPU列表
     *
     * @param infoPage
     * @param category3Id
     * @return
     */
    IPage<SpuInfo> getSpuByPage(IPage<SpuInfo> infoPage, Long category3Id);

    /**
     * 保存或者更新Spu
     * @param spuInfo
     */
    void saveSpuInfo(SpuInfo spuInfo);

    /**
     * 根据id获取SpuInfo
     * @param id
     * @return
     */
    SpuInfo getSpuInfo(Long id);

    /**
     * 根据spuId 获取海报信息
     * @param spuId
     * @return
     */
    List<SpuPoster> getSpuPosterBySpuId(Long spuId);
}
