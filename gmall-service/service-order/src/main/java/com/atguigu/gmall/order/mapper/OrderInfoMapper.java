package com.atguigu.gmall.order.mapper;


import com.atguigu.gmall.model.order.OrderInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;

/**
* @author admin
* @description 针对表【order_info(订单表 订单表)】的数据库操作Mapper
* @createDate 2023-12-26 22:56:52
* @Entity com.atguigu.gmall.order.OrderInfo
*/
@Mapper
public interface OrderInfoMapper extends BaseMapper<OrderInfo> {

    /**
     * 查询订单列表
     * @param orderInfoPage
     * @param userId
     * @param orderStatus
     * @return
     */
    IPage<OrderInfo> selectOrder(Page<OrderInfo> orderInfoPage, String userId, String orderStatus);
}




