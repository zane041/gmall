package com.atguigu.gmall.product.service.impl;

import com.atguigu.gmall.common.cache.GmallCache;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.service.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Objects;

@Service
public class SpuManageServiceImpl implements SpuManageService {

    @Autowired
    private SpuInfoService spuInfoService;

    @Autowired
    private SpuImageService spuImageService;

    @Autowired
    private SpuSaleAttrService spuSaleAttrService;

    @Autowired
    private SpuSaleAttrValueService spuSaleAttrValueService;

    @Autowired
    private SpuPosterService spuPosterService;

    @Override
    public IPage<SpuInfo> getSpuByPage(IPage<SpuInfo> infoPage, Long category3Id) {
        if (category3Id == null || category3Id <= 0) return null;
        QueryWrapper<SpuInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("category3_id", category3Id)
                        .orderByDesc("id");
        return spuInfoService.page(infoPage, queryWrapper);
    }

    /**
     *  保存或者更新SPU商品信息
     *  如果是更新：采用先删后增的方式 （这样导致的问题就算主键id资源浪费导致id值很大）
     * @param spuInfo
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveSpuInfo(SpuInfo spuInfo) {
        if (Objects.isNull(spuInfo)) return;
        Boolean isSave = Objects.isNull(spuInfo.getId());
        if (Objects.nonNull(spuInfo.getId())) {
//            spuInfoService.removeById(spuInfo.getId()); //这里不删除，保留spu_id避免删掉了spu导致其下的sku一并没有的情况
            spuImageService.remove(new QueryWrapper<SpuImage>().eq("spu_id", spuInfo.getId()));
            spuPosterService.remove(new QueryWrapper<SpuPoster>().eq("spu_id", spuInfo.getId()));
            spuSaleAttrService.remove(new QueryWrapper<SpuSaleAttr>().eq("spu_id", spuInfo.getId()));
            spuSaleAttrValueService.remove(new QueryWrapper<SpuSaleAttrValue>().eq("spu_id", spuInfo.getId()));
        }
        spuInfoService.saveOrUpdate(spuInfo);// 因为SpuInfo里id属性加了自增主键的注解，所以这里插入完spuInfo里id就有值了
        List<SpuImage> spuImageList = spuInfo.getSpuImageList();
        if (!CollectionUtils.isEmpty(spuImageList)) {
            for (SpuImage spuImage : spuImageList) {
                spuImage.setSpuId(spuInfo.getId());
            }
            spuImageService.saveBatch(spuImageList);
        }
        List<SpuPoster> spuPosterList = spuInfo.getSpuPosterList();
        if (!CollectionUtils.isEmpty(spuPosterList)) {
            for (SpuPoster spuPoster : spuPosterList) {
                spuPoster.setSpuId(spuInfo.getId());
            }
            spuPosterService.saveBatch(spuPosterList);
        }
        List<SpuSaleAttr> spuSaleAttrList = spuInfo.getSpuSaleAttrList();
        if (!CollectionUtils.isEmpty(spuSaleAttrList)) {
            for (SpuSaleAttr spuSaleAttr : spuSaleAttrList) {
                spuSaleAttr.setSpuId(spuInfo.getId());
                List<SpuSaleAttrValue> spuSaleAttrValueList = spuSaleAttr.getSpuSaleAttrValueList();
                if (Objects.nonNull(spuSaleAttrValueList)) {
                    for (SpuSaleAttrValue spuSaleAttrValue : spuSaleAttrValueList) {
                        spuSaleAttrValue.setSpuId(spuInfo.getId());
                        spuSaleAttrValue.setSaleAttrName(spuSaleAttr.getSaleAttrName());
                    }
                    spuSaleAttrValueService.saveBatch(spuSaleAttrValueList);
                }
            }
            spuSaleAttrService.saveBatch(spuSaleAttrList);
        }
    }

    @Override
    public SpuInfo getSpuInfo(Long id) {
        if (Objects.isNull(id) || id  <= 0) return null;
        SpuInfo spuInfo = spuInfoService.getById(id);
        spuInfo.setSpuImageList(spuImageService.list(new QueryWrapper<SpuImage>().eq("spu_id", id)));
        spuInfo.setSpuPosterList(spuPosterService.list(new QueryWrapper<SpuPoster>().eq("spu_id", id)));
        spuInfo.setSpuSaleAttrList(spuSaleAttrService.listBySpuId(id));
        return spuInfo;
    }

    @Override
    @GmallCache(prefix = "spuPosterBySpuId:")
    public List<SpuPoster> getSpuPosterBySpuId(Long spuId) {
        if (Objects.isNull(spuId) || spuId <= 0) return null;
        return spuPosterService.list(new LambdaQueryWrapper<SpuPoster>().eq(SpuPoster::getSpuId, spuId));
    }
}
