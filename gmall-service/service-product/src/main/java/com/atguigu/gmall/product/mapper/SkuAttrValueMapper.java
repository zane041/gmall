package com.atguigu.gmall.product.mapper;

import com.atguigu.gmall.model.product.BaseAttrInfo;
import com.atguigu.gmall.model.product.SkuAttrValue;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SkuAttrValueMapper extends BaseMapper<SkuAttrValue> {

    /**
     * 根据skuId查询平台属性带属性值
     * @param skuId
     * @return
     */
    List<BaseAttrInfo> getAttrList(@Param("skuId") Long skuId);
}
