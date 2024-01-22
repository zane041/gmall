package com.atguigu.gmall.product.mapper;

import com.atguigu.gmall.model.product.SpuSaleAttr;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SpuSaleAttrMapper extends BaseMapper<SpuSaleAttr> {

    /**
     * 根据spuId查询销售属性带有销售属性值
     * @param spuId
     * @return
     */
    List<SpuSaleAttr> selectBySpuId(Long spuId);

    /**
     * 查询当前商品所有的销售属性,判断为当前SKU拥有销售属性增加选中效果
     * @param skuId
     * @param spuId
     * @return
     */
    List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(@Param("skuId") Long skuId,
                                                   @Param("spuId") Long spuId);
}
