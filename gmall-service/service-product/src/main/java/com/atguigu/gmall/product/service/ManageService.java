package com.atguigu.gmall.product.service;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.model.product.*;

import java.util.List;

/**
 * 后台管理服务
 */
public interface ManageService {

    /**
     * 加载所有的一级分类数据
     * @return
     */
    List<BaseCategory1> getBaseCategory1();

    /**
     * 通过选择一级分类id加载二级分类数据
     * @param category1Id
     * @return
     */
    List<BaseCategory2> getBaseCategory2(Long category1Id);


    /**
     * 通过选择二级分类id加载三级分类数据
     * @param category2Id
     * @return
     */
    List<BaseCategory3> getBaseCategory3(Long category2Id);

    /**
     * 根据分类id加载平台属性列表
     * @param category1Id
     * @param category2Id
     * @param category3Id
     * @return
     */
    List<BaseAttrInfo> getBaseAttrInfo(Long category1Id, Long category2Id, Long category3Id);

    /**
     * 添加平台属性
     * @param baseAttrInfo
     */
    void saveAttrInfo(BaseAttrInfo baseAttrInfo);

    /**
     * 根据属性id获取属性值列表
     * @param attrId
     * @return
     */
    List<BaseAttrValue> getAttrValueList(Long attrId);

    /**
     * 获取所有销售属性
     * @return
     */
    List<BaseSaleAttr> getBaseSaleAttrList();

    /**
     * 根据三级分类id 查询分类数据
     * @param category3Id
     * @return
     */
    BaseCategoryView getCategoryView(Long category3Id);

    /**
     * 获取全部分类信息
     * @return
     */
    List<JSONObject> getBaseCategoryList();
}

