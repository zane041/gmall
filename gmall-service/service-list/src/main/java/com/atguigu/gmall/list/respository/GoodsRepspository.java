package com.atguigu.gmall.list.respository;

import com.atguigu.gmall.model.list.Goods;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface GoodsRepspository extends ElasticsearchRepository<Goods, Long> {
}
