package com.atguigu.gmall.product.service.impl;

import com.atguigu.gmall.common.cache.GmallCache;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.mapper.SkuAttrValueMapper;
import com.atguigu.gmall.product.mapper.SkuSaleAttrValueMapper;
import com.atguigu.gmall.product.mapper.SpuSaleAttrMapper;
import com.atguigu.gmall.product.service.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Service
public class SkuManageServiceImpl implements SkuManageService {

    @Autowired
    private SkuImageService skuImageService;

    @Autowired
    private SkuInfoService skuInfoService;

    @Autowired
    private SkuAttrValueService skuAttrValueService;

    @Autowired
    private SkuSaleAttrValueService skuSaleAttrValueService;

    @Autowired
    private SkuAttrValueMapper skuAttrValueMapper;

    @Autowired
    private SpuSaleAttrMapper spuSaleAttrMapper;

    @Autowired
    private SkuSaleAttrValueMapper skuSaleAttrValueMapper;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private RabbitService rabbitService;

    @Autowired
    private RedisTemplate redisTemplate;


    // 更新方式：sku_image sku_attr_value  sku_sale_attr_value先删后增，sku_info更新
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveSkuInfo(SkuInfo skuInfo) {
        if (Objects.isNull(skuInfo)) return;
        // 更新四张表： sku_info sku_image sku_attr_value  sku_sale_attr_value
        if (Objects.nonNull(skuInfo.getId())) {
//            skuInfoService.removeById(skuInfo.getId());
            skuImageService.remove(new LambdaQueryWrapper<SkuImage>().eq(SkuImage::getSkuId, skuInfo.getId()));
            skuAttrValueService.remove(new LambdaQueryWrapper<SkuAttrValue>().eq(SkuAttrValue::getSkuId, skuInfo.getId()));
            skuSaleAttrValueService.remove(new LambdaQueryWrapper<SkuSaleAttrValue>().eq(SkuSaleAttrValue::getSkuId, skuInfo.getId()));
        }
        skuInfoService.saveOrUpdate(skuInfo);
        List<SkuImage> skuImageList = skuInfo.getSkuImageList();
        if (!skuImageList.isEmpty()) {
            skuImageList.forEach(skuImage -> skuImage.setSkuId(skuInfo.getId()));
            skuImageService.saveBatch(skuImageList);
        }
        List<SkuAttrValue> skuAttrValueList = skuInfo.getSkuAttrValueList();
        if (!skuAttrValueList.isEmpty()) {
            skuAttrValueList.forEach(skuAttrValue -> skuAttrValue.setSkuId(skuInfo.getId()));
            skuAttrValueService.saveBatch(skuAttrValueList);
        }
        List<SkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();
        if (!CollectionUtils.isEmpty(skuSaleAttrValueList)) {
            skuSaleAttrValueList.forEach(skuSaleAttrValue -> {
                skuSaleAttrValue.setSkuId(skuInfo.getId());
                skuSaleAttrValue.setSpuId(skuInfo.getSpuId());
            });
            skuSaleAttrValueService.saveBatch(skuSaleAttrValueList);
        }
    }

    @Override
    public IPage<SkuInfo> getSkuListByPage(Page<SkuInfo> skuInfoPage, Long category3Id) {
        return skuInfoService.page(skuInfoPage, new QueryWrapper<SkuInfo>().eq("category3_id", category3Id).orderByDesc("update_time"));
    }

    @Override
    @GmallCache(prefix = "sku:")
    public SkuInfo getSkuInfo(Long skuId) {
        if (Objects.isNull(skuId) || skuId <= 0) return null;
        SkuInfo skuInfo = skuInfoService.getById(skuId);
        if (Objects.isNull(skuInfo)) return null;
        List<SkuImage> skuImageList = skuImageService.list(new LambdaQueryWrapper<SkuImage>().eq(SkuImage::getSkuId, skuId));
        skuInfo.setSkuImageList(skuImageList);
        return skuInfo;
    }

    @Override
    public BigDecimal getSkuPrice(Long skuId) {
        String lockKey = RedisConst.SKUKEY_PREFIX + "price:" + skuId + ":lock";
        RLock lock = redissonClient.getLock(lockKey);
        try {
            boolean flag = lock.tryLock(RedisConst.SKULOCK_EXPIRE_PX1, RedisConst.SKULOCK_EXPIRE_PX2, TimeUnit.SECONDS);
            if (flag) {
                SkuInfo skuInfo = skuInfoService.getOne(new LambdaQueryWrapper<SkuInfo>().eq(SkuInfo::getId, skuId).select(SkuInfo::getPrice));
                if (Objects.nonNull(skuInfo)) return skuInfo.getPrice();
            } else {
                Thread.sleep(500);
                getSkuPrice(skuId);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
        return new BigDecimal(0);
    }

    @Override
    @GmallCache(prefix = "attrList:")
    public List<BaseAttrInfo> getAttrList(Long skuId) {
        if (Objects.isNull(skuId) || skuId <= 0) return null;
        return skuAttrValueMapper.getAttrList(skuId);
    }

    @Override
    @GmallCache(prefix = "spuSaleAttrListCheckBySku:")
    public List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(Long skuId, Long spuId) {
        if (Objects.isNull(skuId) || skuId <= 0 || Objects.isNull(spuId) || spuId <= 0) return null;
        return spuSaleAttrMapper.getSpuSaleAttrListCheckBySku(skuId, spuId);
    }

    @Override
    @GmallCache(prefix = "skuValueIdsMap:")
    public Map getSkuValueIdsMap(Long spuId) {
        List<Map> maps = skuSaleAttrValueMapper.getSkuValueIdsMap(spuId);
        HashMap<Object, Object> result = new HashMap<>();
        if (!CollectionUtils.isEmpty(maps)) {
            maps.forEach(map -> {
                result.put(map.get("values_ids"), map.get("sku_id"));
            });
        }
        return result;
    }

    @Override
    public void onsale(Long skuId) {
        //  数据库发生了变化；则需要保证缓存数据一致！  mysql 与 redis 数据同步！
        String skuKey = RedisConst.SKUKEY_PREFIX+"["+skuId+"]"+RedisConst.SKUKEY_SUFFIX;
        redisTemplate.delete(skuKey);
        //  update sku_info set is_sale = 1 where sku_id = ?;
        SkuInfo skuInfo = new SkuInfo();
        skuInfo.setId(skuId);
        skuInfo.setIsSale(1);
        skuInfoService.updateById(skuInfo);

        //  睡眠.
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        //  再删除.
        this.redisTemplate.delete(skuKey);
        //  商品上架：
        //  发送的消息内容是谁?  是由消费者决定的！
        this.rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_GOODS,MqConst.ROUTING_GOODS_UPPER,skuId);
    }

    @Override
    public void cancelSale(Long skuId) {
        //  组成缓存的key;
        String skuKey = RedisConst.SKUKEY_PREFIX+"["+skuId+"]"+RedisConst.SKUKEY_SUFFIX;
        this.redisTemplate.delete(skuKey);
        //  update sku_info set is_sale = 0 where sku_id = ?;
        SkuInfo skuInfo = new SkuInfo();
        skuInfo.setId(skuId);
        skuInfo.setIsSale(0);
        skuInfoService.updateById(skuInfo);
        //  睡眠.
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        //  再删除.
        this.redisTemplate.delete(skuKey);
        //  商品下架：
        //  发送的消息内容是谁?  是由消费者决定的！
        this.rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_GOODS,MqConst.ROUTING_GOODS_LOWER,skuId);
    }

}
