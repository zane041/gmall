package com.atguigu.gmall.order.mapper;


import com.atguigu.gmall.model.order.OrderDetail;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
* @author admin
* @description 针对表【order_detail(订单明细表)】的数据库操作Mapper
* @createDate 2023-12-26 23:11:50
* @Entity com.atguigu.gmall.order.OrderDetail
*/
@Mapper
public interface OrderDetailMapper extends BaseMapper<OrderDetail> {

}




