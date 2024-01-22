package com.atguigu.gmall.product.service.impl;

import com.atguigu.gmall.model.product.SpuSaleAttr;
import com.atguigu.gmall.product.mapper.SpuSaleAttrMapper;
import com.atguigu.gmall.product.service.SpuSaleAttrService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class SpuSaleAttrServiceImpl extends ServiceImpl<SpuSaleAttrMapper, SpuSaleAttr> implements SpuSaleAttrService {

    @Autowired
    private SpuSaleAttrMapper spuSaleAttrMapper;

    @Override
    public List<SpuSaleAttr> listBySpuId(Long spuId) {
        if (Objects.isNull(spuId) || spuId <= 0) return null;
        return spuSaleAttrMapper.selectBySpuId(spuId);
    }
}
