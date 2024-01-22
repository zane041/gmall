package com.atguigu.gmall.order.service;


import com.atguigu.gmall.model.enums.ProcessStatus;
import com.atguigu.gmall.model.order.OrderInfo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

/**
* @author admin
* @description 针对表【order_info(订单表 订单表)】的数据库操作Service
* @createDate 2023-12-26 22:56:52
*/
public interface OrderInfoService extends IService<OrderInfo> {

    /**
     * 保存订单
     * @param orderInfo
     * @return
     */
    Long saveOrderInfo(OrderInfo orderInfo);

    /**
     * 生成流水号-防止订单重复提交
     * @param userId
     * @return
     */
    String getTradeCode(String userId);

    /**
     *  验证流水号是否正确
     * @param userId
     * @param tradeCode
     * @return
     */
    Boolean checkTradeCode(String userId, String tradeCode);

    /**
     *  删除流水号
     * @param userId
     */
    void deleteTradeCode(String userId);

    /**
     * 验证库存
     * @param skuId
     * @param skuNum
     * @return
     */
    boolean checkStock(Long skuId, Integer skuNum);

    /**
     * 关闭订单
     * @param orderId
     */
    void execExpiredOrder(Long orderId);

    /**
     * 按照指定状态修改订单
     * @param orderId
     * @param processStatus
     */
    void updateOrderStatus(Long orderId, ProcessStatus processStatus);

    /**
     * 根据订单Id 查询订单信息
     * @param orderId
     * @return
     */
    OrderInfo getOrderInfo(Long orderId);

    /**
     *  发送消息给库存系统
     * @param orderId
     */
    void sendDeductStockMsg(Long orderId);

    /**
     * 将orderInfo 转换为map集合
     * @param orderInfo
     * @return
     */
    Map<String, Object> initWare(OrderInfo orderInfo);

    /**
     * 拆单方法。
     * @param orderId
     * @param wareSkuMap
     * @return
     */
    List<OrderInfo> orderSplit(String orderId, String wareSkuMap);

    /**
     * 根据订单Id 与 标识更新数据
     * @param orderId
     * @param flag 1表示只有orderInfo，2表示orderInfo和paymentInfo
     */
    void execExpiredOrder(Long orderId, String flag);

    /**
     * 获取订单列表
     * @param orderInfoPage
     * @param userId
     * @param orderStatus
     * @return
     */
    IPage<OrderInfo> getOrder(Page<OrderInfo> orderInfoPage, String userId, String orderStatus);
}
