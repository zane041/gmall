package com.atguigu.gmall.product.service.impl;

import com.atguigu.gmall.model.product.BaseCategoryTrademark;
import com.atguigu.gmall.model.product.BaseTrademark;
import com.atguigu.gmall.model.product.CategoryTrademarkVo;
import com.atguigu.gmall.product.mapper.BaseCategoryTrademarkMapper;
import com.atguigu.gmall.product.mapper.BaseTrademarkMapper;
import com.atguigu.gmall.product.service.BaseCategoryTrademarkService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class BaseCategoryTrademarkServiceImpl extends ServiceImpl<BaseCategoryTrademarkMapper, BaseCategoryTrademark> implements BaseCategoryTrademarkService {

    @Autowired
    private BaseCategoryTrademarkMapper baseCategoryTrademarkMapper;

    @Autowired
    private BaseTrademarkMapper baseTrademarkMapper;

    @Override
    public List<BaseTrademark> getByCategory3Id(Long category3Id) {
        if (Objects.isNull(category3Id) || category3Id <= 0) return null;
        List<BaseCategoryTrademark> baseCategoryTrademarkList = baseCategoryTrademarkMapper.selectList(new QueryWrapper<BaseCategoryTrademark>().eq("category3_id", category3Id));
        if (!baseCategoryTrademarkList.isEmpty()) {
            List<Long> trademarkIds = new ArrayList<>();
            for (BaseCategoryTrademark baseCategoryTrademark : baseCategoryTrademarkList) {
                trademarkIds.add(baseCategoryTrademark.getTrademarkId());
            }
            return baseTrademarkMapper.selectBatchIds(trademarkIds);
        }
        return null;
    }

    @Override
    public List<BaseTrademark> findCurrentTrademarkList(Long category3Id) {
        if (Objects.isNull(category3Id) || category3Id <= 0) return null;
        List<BaseCategoryTrademark> baseCategoryTrademarkList = baseCategoryTrademarkMapper.selectList(new QueryWrapper<BaseCategoryTrademark>().eq("category3_id", category3Id));
        if (!baseCategoryTrademarkList.isEmpty()) {
            List<Long> tmIds = baseCategoryTrademarkList.stream().map(BaseCategoryTrademark::getTrademarkId).collect(Collectors.toList());
            return baseTrademarkMapper.selectList(null).stream().filter(baseTrademark -> {
                return !tmIds.contains(baseTrademark.getId());
            }).collect(Collectors.toList());
        }
        return baseTrademarkMapper.selectList(null);//三级分类id 没有与品牌绑定的时候
    }

    @Override
    public void save(CategoryTrademarkVo categoryTrademarkVo) {
        List<Long> trademarkIdList = categoryTrademarkVo.getTrademarkIdList();
        if (Objects.nonNull(trademarkIdList)) {
            List<BaseCategoryTrademark> baseCategoryTrademarkList = trademarkIdList.stream().map(trademarkId -> {
                BaseCategoryTrademark baseCategoryTrademark = new BaseCategoryTrademark();
                baseCategoryTrademark.setTrademarkId(trademarkId);
                baseCategoryTrademark.setCategory3Id(categoryTrademarkVo.getCategory3Id());
                return baseCategoryTrademark;
            }).collect(Collectors.toList());
            this.saveBatch(baseCategoryTrademarkList);
        }
    }
}
