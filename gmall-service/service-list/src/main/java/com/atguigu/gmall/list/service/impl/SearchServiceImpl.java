package com.atguigu.gmall.list.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.list.respository.GoodsRepspository;
import com.atguigu.gmall.list.service.SearchService;
import com.atguigu.gmall.model.list.*;
import com.atguigu.gmall.model.product.BaseAttrInfo;
import com.atguigu.gmall.model.product.BaseCategoryView;
import com.atguigu.gmall.model.product.BaseTrademark;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class SearchServiceImpl implements SearchService {

    @Autowired
    private GoodsRepspository goodsRepspository;

    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    @Override
    public void upperGoods(Long skuId) {
        Goods goods = new Goods();

        // 1. 查询商品信息 —— sku_info
        CompletableFuture<SkuInfo> skuInfoCompletableFuture = CompletableFuture.supplyAsync(() -> {
            SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
            if (Objects.nonNull(skuInfo)) {
                goods.setId(skuId);
                goods.setTitle(skuInfo.getSkuName());
                // 商品价格 skuInfo.getPrice() 可能来自于缓存 -> 可能会存在某一瞬间数据不一致的问题！
                // goods.setPrice(skuInfo.getPrice().doubleValue());
                // 直接查询数据库
                goods.setPrice(productFeignClient.getSkuPrice(skuId).doubleValue());
                goods.setCreateTime(skuInfo.getCreateTime());
                goods.setDefaultImg(skuInfo.getSkuDefaultImg());
            }
            return skuInfo;
        });

        // 2. 查询品牌数据
        CompletableFuture<Void> tmCompletableFuture = skuInfoCompletableFuture.thenAcceptAsync(skuInfo -> {
            if (Objects.nonNull(skuInfo)) {
                BaseTrademark trademark = productFeignClient.getTrademarkById(skuInfo.getTmId());
                if (Objects.isNull(trademark)) return;
                goods.setTmId(trademark.getId());
                goods.setTmName(trademark.getTmName());
                goods.setTmLogoUrl(trademark.getLogoUrl());
            }
        });

        // 3. 查询分类数据
        CompletableFuture<Void> categoryCompletableFuture = skuInfoCompletableFuture.thenAcceptAsync(skuInfo -> {
            if (Objects.nonNull(skuInfo)) {
                BaseCategoryView categoryView = productFeignClient.getCategoryView(skuInfo.getCategory3Id());
                if (Objects.isNull(categoryView)) return;
                goods.setCategory1Id(categoryView.getCategory1Id());
                goods.setCategory1Name(categoryView.getCategory1Name());
                goods.setCategory2Id(categoryView.getCategory2Id());
                goods.setCategory2Name(categoryView.getCategory2Name());
                goods.setCategory3Id(categoryView.getCategory3Id());
                goods.setCategory3Name(categoryView.getCategory3Name());
            }
        });

        // 4. 获取平台属性以及平台属性值
        CompletableFuture<Void> attrCompletableFuture = CompletableFuture.runAsync(() -> {
            List<BaseAttrInfo> attrList = productFeignClient.getAttrList(skuId);
            if (CollectionUtils.isEmpty(attrList)) return;
            List<SearchAttr> searchAttrList = attrList.stream().map(baseAttrInfo -> {
                SearchAttr searchAttr = new SearchAttr();
                searchAttr.setAttrId(baseAttrInfo.getId());
                searchAttr.setAttrName(baseAttrInfo.getAttrName());
                searchAttr.setAttrValue(baseAttrInfo.getAttrValueList().get(0).getValueName());//因为根据skuId查出来每个属性只有一个属性值
                return searchAttr;
            }).collect(Collectors.toList());
            goods.setAttrs(searchAttrList);
        });

        // 多任务并行执行
        CompletableFuture.allOf(
                skuInfoCompletableFuture,
                tmCompletableFuture,
                categoryCompletableFuture,
                attrCompletableFuture
        ).join();

        // 保存数据到ES
        goodsRepspository.save(goods);
    }

    @Override
    public void lowerGoods(Long skuId) {
        goodsRepspository.deleteById(skuId);
    }

    @Override
    public void incrHotScore(Long skuId) {
        // 1. 使用缓存缓冲热度更新。
        // 缓存的key：hotScore
        String key = "hotScore";
        // 存储类型为 SortedSet，使用命令 ZINCRBY key increment member
        Double score = redisTemplate.opsForZSet().incrementScore(key, "skuId:" + skuId, 1);
        // 2. 缓冲10次后更新ES库
        if (score % 10 == 0) {
            Optional<Goods> optional = goodsRepspository.findById(skuId);
            Goods goods = optional.get();
            goods.setHotScore(score.longValue());
            goodsRepspository.save(goods);
        }
    }

    @Override
    public SearchResponseVo search(SearchParam searchParam) {
        // 1. 根据用户的检索条件生成DSL语句
        SearchRequest searchRequest = buildDsl(searchParam);
        // 2. 执行DSL语句
        SearchResponse searchResponse = null;
        try {
            searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        // 3. 将获取到的执行结果转换为返回对象(SearchResponseVo实体)
        SearchResponseVo searchResponseVo = parseResult(searchResponse);
        // 4. 封装分页数据
        searchResponseVo.setPageNo(searchParam.getPageNo());
        searchResponseVo.setPageSize(searchParam.getPageSize());
        // 总页数 = (总记录数 + 每页记录数 - 1)  / 每页记录数
        searchResponseVo.setTotalPages((searchResponseVo.getTotal() + searchParam.getPageSize() - 1) / searchParam.getPageSize());
        // 5. 返回数据
        return searchResponseVo;
    }

    /**
     * 封装返回结果集对象
     * @param searchResponse
     * @return
     */
    private SearchResponseVo parseResult(SearchResponse searchResponse) {
        SearchResponseVo searchResponseVo = new SearchResponseVo();

        /**
         * 需要赋值有：
         *          private List<SearchResponseTmVo> trademarkList;
         *         private List<SearchResponseAttrVo> attrsList = new ArrayList<>();
         *         private List<Goods> goodsList = new ArrayList<>();
         *         private Long total;//总记录数
         */

        SearchHits hits = searchResponse.getHits(); // 获取查询结果数据

        // 1. 赋值total
        searchResponseVo.setTotal(hits.getTotalHits().value);

        // 2. 赋值goodsList
        List<Goods> goodsList = new ArrayList<>();
        SearchHit[] subHits = hits.getHits();
        for (SearchHit subHit : subHits) {
            //获取到商品的JSON字符串, 并转换为Goods实体类对象
            Goods goods = JSON.parseObject(subHit.getSourceAsString(), Goods.class);
            // 如果是分词查询，则商品名称必须要高亮 —— 即不能获取 _source 下的title 数据！而要highlight 下的title 数据！
            // 高亮里的数据不为空则用高亮的title，因为通过关键词查询进来才会放title值进高亮里
            if (subHit.getHighlightFields().get("title") != null) {
                String title = subHit.getHighlightFields().get("title").getFragments()[0].toString();
                goods.setTitle(title);
            }
            goodsList.add(goods);
        }
        searchResponseVo.setGoodsList(goodsList);

        // 获取聚合数据。里面有品牌和平台属性的数据
        Map<String, Aggregation> aggregationMap = searchResponse.getAggregations().asMap();

        // 3. 赋值trademarkList。
        ParsedLongTerms tmIdAgg = (ParsedLongTerms) aggregationMap.get("tmIdAgg");
        List<SearchResponseTmVo> trademarkList = tmIdAgg.getBuckets().stream().map(bucket -> {
            SearchResponseTmVo searchResponseTmVo = new SearchResponseTmVo();
            // 获取品牌ID
            searchResponseTmVo.setTmId(Long.parseLong(bucket.getKeyAsString()));
            // 获取品牌名称
            ParsedStringTerms tmNameAgg = bucket.getAggregations().get("tmNameAgg");
            String tmName = tmNameAgg.getBuckets().get(0).getKeyAsString();
            searchResponseTmVo.setTmName(tmName);
            // 获取品牌logo
            ParsedStringTerms tmLogoUrlAgg = bucket.getAggregations().get("tmLogoUrlAgg");
            String tmLogoUrl = tmLogoUrlAgg.getBuckets().get(0).getKeyAsString();
            searchResponseTmVo.setTmLogoUrl(tmLogoUrl);
            return searchResponseTmVo;
        }).collect(Collectors.toList());
        searchResponseVo.setTrademarkList(trademarkList);

        // 4. 赋值attrsList。 ES中平台属性attrs的数据类型是nested
        ParsedNested attrsAgg = (ParsedNested ) aggregationMap.get("attrsAgg");
        ParsedLongTerms attrIdAgg = attrsAgg.getAggregations().get("attrIdAgg");
        List<SearchResponseAttrVo> attrVoList = attrIdAgg.getBuckets().stream().map(bucket -> {
            SearchResponseAttrVo searchResponseAttrVo = new SearchResponseAttrVo();
            // 平台属性ID
            searchResponseAttrVo.setAttrId(Long.valueOf(bucket.getKeyAsString()));
            // 平台属性名
            ParsedStringTerms attrNameAgg = bucket.getAggregations().get("attrNameAgg");
            searchResponseAttrVo.setAttrName(attrNameAgg.getBuckets().get(0).getKeyAsString());
            // 平台属性值
            ParsedStringTerms attrValueAgg = bucket.getAggregations().get("attrValueAgg");
            List<String> valueNameList = attrValueAgg.getBuckets().stream().map(bucket1 -> bucket1.getKeyAsString()).collect(Collectors.toList());
            searchResponseAttrVo.setAttrValueList(valueNameList);
            return searchResponseAttrVo;
        }).collect(Collectors.toList());
        searchResponseVo.setAttrsList(attrVoList);

        return searchResponseVo;
    }

    /**
     * 生成DSL语句
     * @param searchParam
     * @return
     */
    private SearchRequest buildDsl(SearchParam searchParam) {
        // 创建一个查询器：相当于 DSL 语句中最外面那个{}
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        // query -- bool
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

        // 判断用户是否根据分类ID查询 —— 分类ID查询入口
        if (!StringUtils.isEmpty(searchParam.getCategory1Id())) {
            // query -- bool -- filter -- term
            boolQueryBuilder.filter(QueryBuilders.termQuery("category1Id", searchParam.getCategory1Id()));
        }
        if (!StringUtils.isEmpty(searchParam.getCategory2Id())) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("category2Id", searchParam.getCategory2Id()));
        }
        if (!StringUtils.isEmpty(searchParam.getCategory3Id())) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("category3Id", searchParam.getCategory3Id()));
        }

        // 如果是根据关键词(keyword)查询 —— 关键词查询入口
        if (!StringUtils.isEmpty(searchParam.getKeyword())) {
            // query -- bool -- must -- match
            boolQueryBuilder.must(QueryBuilders.matchQuery("title", searchParam.getKeyword()).operator(Operator.AND));
            // 设置高亮的字段以及格式
            HighlightBuilder highlightBuilder = new HighlightBuilder();
            highlightBuilder.preTags("<span style=color:red>");
            highlightBuilder.postTags("</span>");
            highlightBuilder.field("title");
            searchSourceBuilder.highlighter(highlightBuilder);
        }

        // 如果是根据品牌进行过滤。 前端传递品牌数据的格式：trademark=1:小米
        if (!StringUtils.isEmpty(searchParam.getTrademark())) {
            // 坑：注意不用用spring框架的 StringUtils.split()进行分割，结果会错
            String[] trademarkSplit = searchParam.getTrademark().split(":");
            if (trademarkSplit.length == 2) {
                // query -- bool -- filter -- term
                boolQueryBuilder.filter(QueryBuilders.termQuery("tmId", trademarkSplit[0]));
            }
        }

        // 如果是根据平台属性过滤。前端传递格式： props=24:128G:机身内存&props=23:4G:运行内存
        if (!StringUtils.isEmpty(searchParam.getProps())) {
            String[] props = searchParam.getProps();
            for (String prop : props) {
                String[] propSplit = prop.split(":");
                if (propSplit.length == 3) {
                    // DSL语句通过： query -- bool -- filter -- nested -- bool -- filter
                    BoolQueryBuilder innerBoolQueryBuilder = QueryBuilders.boolQuery();
                    innerBoolQueryBuilder.filter(QueryBuilders.termQuery("attrs.attrId", propSplit[0])); // 平台属性ID
                    innerBoolQueryBuilder.filter(QueryBuilders.termQuery("attrs.attrValue", propSplit[1])); // 平台属性值
                    boolQueryBuilder.filter(QueryBuilders.nestedQuery("attrs", innerBoolQueryBuilder, ScoreMode.None));
                }
            }
        }

        // 设置分页
        searchSourceBuilder.from((searchParam.getPageNo() - 1) * searchParam.getPageSize());
        searchSourceBuilder.size(searchParam.getPageSize());

        // 如果设置排序。 前端传递数据格式：order=1:asc ( 1表示综合排序-hotScore 2表示价格排序price，asc是升序desc是降序)
        if (!StringUtils.isEmpty(searchParam.getOrder())) {
            String[] orderSplit = searchParam.getOrder().split(":");
            if (orderSplit.length == 2) {
                String orderField = ""; // 排序字段
                switch (orderSplit[0]) {
                    case "1":
                        orderField = "hotScore";
                        break;
                    case "2":
                        orderField = "price";
                        break;
                }
                //设置排序规则
                searchSourceBuilder.sort(orderField, "asc".equals(orderSplit[1]) ? SortOrder.ASC : SortOrder.DESC);
            } else {
                // 用户没有设置排序则默认按热度降序排序
                searchSourceBuilder.sort("hotScore", SortOrder.DESC);
            }
        }

        // query -- bool （以上就是query下最外层的bool的部分了）
        searchSourceBuilder.query(boolQueryBuilder);

        // 品牌聚合
        searchSourceBuilder.aggregation(AggregationBuilders.terms("tmIdAgg").field("tmId")
                .subAggregation(AggregationBuilders.terms("tmNameAgg").field("tmName"))
                .subAggregation(AggregationBuilders.terms("tmLogoUrlAgg").field("tmLogoUrl")));
        //平台属性聚合
        searchSourceBuilder.aggregation(AggregationBuilders.nested("attrsAgg","attrs")
                .subAggregation(AggregationBuilders.terms("attrIdAgg").field("attrs.attrId")
                        .subAggregation(AggregationBuilders.terms("attrNameAgg").field("attrs.attrName"))
                        .subAggregation(AggregationBuilders.terms("attrValueAgg").field("attrs.attrValue"))));

        // 设置哪些字段显示，哪些字段不显示 （我们可以只给我们想给的数据出去）
        searchSourceBuilder.fetchSource(new String[] {"id", "defaultImg","title","price"},null);

        // 查询请求：GET /goods/_search
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.source(searchSourceBuilder);

        //打印dsl语句
        System.out.println("dsl:\t"+searchSourceBuilder.toString());

        return searchRequest;
    }
}
