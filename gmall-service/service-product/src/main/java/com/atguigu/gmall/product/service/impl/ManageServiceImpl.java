package com.atguigu.gmall.product.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.cache.GmallCache;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.mapper.*;
import com.atguigu.gmall.product.service.ManageService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ManageServiceImpl implements ManageService {

    @Autowired
    private BaseCategory1Mapper baseCategory1Mapper;

    @Autowired
    private BaseCategory2Mapper baseCategory2Mapper;

    @Autowired
    private BaseCategory3Mapper baseCategory3Mapper;

    @Autowired
    private BaseAttrInfoMapper baseAttrInfoMapper;

    @Autowired
    private BaseAttrValueMapper baseAttrValueMapper;

    @Autowired
    private BaseSaleAttrMapper baseSaleAttrMapper;

    @Autowired
    private BaseCategoryViewMapper baseCategoryViewMapper;


    @Override
    public List<BaseCategory1> getBaseCategory1() {
        //select * from base_category1;
        return baseCategory1Mapper.selectList(null);
    }

    @Override
    public List<BaseCategory2> getBaseCategory2(Long category1Id) {
        return baseCategory2Mapper.selectList(new QueryWrapper<BaseCategory2>().eq("category1_id", category1Id));
    }

    @Override
    public List<BaseCategory3> getBaseCategory3(Long category2Id) {
        return baseCategory3Mapper.selectList(new QueryWrapper<BaseCategory3>().eq("category2_id", category2Id));
    }

    @Override
    public List<BaseAttrInfo> getBaseAttrInfo(Long category1Id, Long category2Id, Long category3Id) {
        // 涉及到多表查询，因为捎带着把AttrValue也查出来用了。涉及动态SQL操作,编写复杂SQL语句，使用 mapper.xml
        return baseAttrInfoMapper.selectBaseAttrInfoList(category1Id, category2Id, category3Id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveAttrInfo(BaseAttrInfo baseAttrInfo) {
        if (Objects.nonNull(baseAttrInfo.getId()) && baseAttrInfo.getId() > 0) {
            baseAttrInfoMapper.updateById(baseAttrInfo);
            baseAttrValueMapper.delete(new QueryWrapper<BaseAttrValue>().eq("attr_id", baseAttrInfo.getId()));
        } else {
            baseAttrInfoMapper.insert(baseAttrInfo);
        }
        List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
        if (!CollectionUtils.isEmpty(attrValueList)) {
            for (BaseAttrValue baseAttrValue : attrValueList) {
                baseAttrValue.setAttrId(baseAttrInfo.getId());
                baseAttrValueMapper.insert(baseAttrValue);
            }
        }
    }

    @Override
    public List<BaseAttrValue> getAttrValueList(Long attrId) {
        return baseAttrValueMapper.selectList(new QueryWrapper<BaseAttrValue>().eq("attr_id", attrId));
    }

    @Override
    public List<BaseSaleAttr> getBaseSaleAttrList() {
        return baseSaleAttrMapper.selectList(null);
    }

    @Override
    @GmallCache(prefix = "categoryView:")
    public BaseCategoryView getCategoryView(Long category3Id) {
        if (Objects.isNull(category3Id) || category3Id <= 0) return null;
        return baseCategoryViewMapper.selectById(category3Id);
    }

    @Override
    public List<JSONObject> getBaseCategoryList() {
        List<JSONObject> result = new ArrayList<>();
        // 1. 查询所有分类视图表（base_category_view）记录
        List<BaseCategoryView> baseCategoryViews = baseCategoryViewMapper.selectList(null);
        if (CollectionUtils.isEmpty(baseCategoryViews)) return result;
        // 2. 封装一级分类数据。
        int index = 1;
        // 2.1 对所有base_category_view记录根据category1Id进行分组得到所有一级分类数据列表
        Map<Long, List<BaseCategoryView>> category1MapList = baseCategoryViews.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory1Id));
        // 2.2 遍历category1MapList封装一级分类数据
        for (Map.Entry<Long, List<BaseCategoryView>> category1Entry : category1MapList.entrySet()) {
            JSONObject category1 = new JSONObject();//一级分类对象
            Long category1Id = category1Entry.getKey();
            List<BaseCategoryView> category1List = category1Entry.getValue();
            String category1Name = category1List.get(0).getCategory1Name();
            category1.put("categoryName", category1Name);
            category1.put("categoryId", category1Id);
            category1.put("index", index);
            // 3. 封装二级分类数据，也就是一级分类数据里的categoryChild
            List<JSONObject> category2List = new ArrayList<>();
            Map<Long, List<BaseCategoryView>> category2MapList = category1List.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory2Id));
            for (Map.Entry<Long, List<BaseCategoryView>> category2Entry : category2MapList.entrySet()) {
                JSONObject category2 = new JSONObject();
                Long category2Id = category2Entry.getKey();
                List<BaseCategoryView> category2ArrayList = category2Entry.getValue();
                String category2Name = category2ArrayList.get(0).getCategory2Name();
                category2.put("categoryName", category2Name);
                category2.put("categoryId", category2Id);
                // 4. 封装三级分类数据，也就是二级分类数据里的categoryChild
                List<JSONObject> category3List = new ArrayList<>();
                Map<Long, List<BaseCategoryView>> category3MapList = category2ArrayList.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory3Id));
                for (Map.Entry<Long, List<BaseCategoryView>> category3Entry : category3MapList.entrySet()) {
                    JSONObject category3 = new JSONObject();
                    Long category3Id = category3Entry.getKey();
                    List<BaseCategoryView> category3ArrayList = category3Entry.getValue();
                    String category3Name = category3ArrayList.get(0).getCategory3Name();
                    category3.put("categoryName", category3Name);
                    category3.put("categoryId", category3Id);
                    category3List.add(category3);
                }
                category2.put("categoryChild", category3List);
                category2List.add(category2);
            }
            category1.put("categoryChild", category2List);
            // 5. 封装数据到result
            result.add(category1);
        }
        // 6. 返回结果集
        return result;
    }

}
