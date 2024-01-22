package com.atguigu.gmall.order.service.impl;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.common.util.HttpClientUtil;
import com.atguigu.gmall.model.enums.OrderStatus;
import com.atguigu.gmall.model.enums.ProcessStatus;
import com.atguigu.gmall.model.order.OrderDetail;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.order.mapper.OrderInfoMapper;
import com.atguigu.gmall.order.service.OrderDetailService;
import com.atguigu.gmall.order.service.OrderInfoService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
* @author admin
* @description 针对表【order_info(订单表 订单表)】的数据库操作Service实现
* @createDate 2023-12-26 22:56:52
*/
@Service
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo>
    implements OrderInfoService {

    @Autowired
    private OrderDetailService orderDetailService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Value("${ware.url}")
    private String WARE_URL;

    @Autowired
    private RabbitService rabbitService;

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Override
    public Long saveOrderInfo(OrderInfo orderInfo) {
        // 完善orderInfo数据
        // 要赋值的字段 total_amount order_status user_id out_trade_no trade_body operate_time expire_time process_status
        orderInfo.sumTotalAmount();
        orderInfo.setOrderStatus(OrderStatus.UNPAID.name());
        // out_trade_no 第三方支付的交易编号. 不能重复，必须唯一。
        String outTradeNo = "ATGUIGU"+System.currentTimeMillis()+new Random().nextInt(1000);
        orderInfo.setOutTradeNo(outTradeNo);
        orderInfo.setTradeBody("这是订单描述！");
        orderInfo.setOperateTime(new Date());
        //  设置过期时间 +1天;  Calendar 线程不安全的。
        //        Calendar calendar = Calendar.getInstance();
        //        calendar.add(Calendar.DATE,1);
        //        orderInfo.setExpireTime(calendar.getTime());
        ZonedDateTime zonedDateTime = LocalDateTime.now().plusDays(1).atZone(ZoneId.systemDefault());
        Date date = Date.from(zonedDateTime.toInstant());
        orderInfo.setExpireTime(date);
        orderInfo.setProcessStatus(ProcessStatus.UNPAID.name());

        // 保存数据 - 数据库表：order_info，order_detail
        save(orderInfo);
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        if (!CollectionUtils.isEmpty(orderDetailList)) {
            orderDetailList.stream().forEach(orderDetail -> {
                orderDetail.setOrderId(orderInfo.getId());
            });
        }
        orderDetailService.saveBatch(orderDetailList);

        //发送延迟队列，如果定时未支付，取消订单
        rabbitService.sendDealyMessage(MqConst.EXCHANGE_DIRECT_ORDER_CANCEL, MqConst.ROUTING_ORDER_CANCEL, orderInfo.getId(), MqConst.DELAY_TIME);

        return orderInfo.getId();
    }

    @Override
    public String getTradeCode(String userId) {
        String tradeKey = "user:" + userId + ":tradeCode";
        String uuid = UUID.randomUUID().toString().replaceAll("-", "");
        redisTemplate.opsForValue().set(tradeKey, uuid);
        return uuid;
    }

    @Override
    public Boolean checkTradeCode(String userId, String tradeCode) {
        String tradeKey = "user:" + userId + ":tradeCode";
        String redisTradeCode = (String) redisTemplate.opsForValue().get(tradeKey);
        return redisTradeCode.equals(tradeCode);
    }

    @Override
    public void deleteTradeCode(String userId) {
        redisTemplate.delete("user:" + userId + ":tradeCode");
    }

    @Override
    public boolean checkStock(Long skuId, Integer skuNum) {
        // 远程调用http://localhost:9001/hasStock?skuId=10221&num=2
        String stock = HttpClientUtil.doGet(WARE_URL + "/hasStock?skuId=" + skuId + "&num=" + skuNum);
        return "1".equals(stock);
    }

    /**
     * 关闭订单
     *
     * @param orderId
     */
    @Override
    public void execExpiredOrder(Long orderId) {
        this.updateOrderStatus(orderId, ProcessStatus.CLOSED);
    }

    /**
     * 修改订单为指定状态
     *
     * @param orderId
     * @param processStatus
     */
    @Override
    public void updateOrderStatus(Long orderId, ProcessStatus processStatus) {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setId(orderId);
        //订单处理状态-工作人员
        orderInfo.setProcessStatus(processStatus.name());
        //订单状态-消费者
        orderInfo.setOrderStatus(processStatus.getOrderStatus().name());
        this.updateById(orderInfo);
        // 发送关闭交易消息
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_PAYMENT_CLOSE, MqConst.ROUTING_PAYMENT_CLOSE, orderId.toString());
    }

    @Override
    public OrderInfo getOrderInfo(Long orderId) {
        OrderInfo orderInfo = getById(orderId);

        if (orderInfo!=null){
            List<OrderDetail> orderDetailList = orderDetailService.list(new LambdaQueryWrapper<OrderDetail>().eq(OrderDetail::getOrderId, orderId));
            orderInfo.setOrderDetailList(orderDetailList);
        }
        //  返回orderInfo
        return orderInfo;
    }

    @Override
    public void sendDeductStockMsg(Long orderId) {
        //  更新当前订单状态.
        this.updateOrderStatus(orderId,ProcessStatus.NOTIFIED_WARE);
        //  构建发送消息的内容 Json; orderInfo + orderDetail;
        OrderInfo orderInfo = this.getOrderInfo(orderId);
        //  将orderInfo 中的部分字段，封装到Map中！
        Map<String,Object> map = this.initWare(orderInfo);
        //  map对象就是我们想要的json 数据
        this.rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_WARE_STOCK,MqConst.ROUTING_WARE_STOCK, JSON.toJSONString(map));
    }

    /**
     * 将orderInfo 中的部分数据转换为map集合
     * @param orderInfo
     * @return
     */
    public Map<String, Object> initWare(OrderInfo orderInfo) {
        //  声明一个map 集合
        Map<String, Object> mapResult = new HashMap<>();
        mapResult.put("orderId",orderInfo.getId());
        mapResult.put("consignee", orderInfo.getConsignee());
        mapResult.put("consigneeTel", orderInfo.getConsigneeTel());
        mapResult.put("orderComment", orderInfo.getOrderComment());
        mapResult.put("orderBody", orderInfo.getTradeBody());
        mapResult.put("deliveryAddress", orderInfo.getDeliveryAddress());
        mapResult.put("paymentWay", "2");
        //  专门给拆单使用。
        mapResult.put("wareId", orderInfo.getWareId());

        /*
        details:[{skuId:101,skuNum:1,skuName:’小米手64G’},{skuId:201,skuNum:1,skuName:’索尼耳机’}]
         */
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        List<HashMap<String, Object>> hashMapList = orderDetailList.stream().map(orderDetail -> {
            //  构建一个map 集合
            HashMap<String, Object> map = new HashMap<>();
            map.put("skuId", orderDetail.getSkuId());
            map.put("skuNum", orderDetail.getSkuNum());
            map.put("skuName", orderDetail.getSkuName());
            //  返回这个集合
            return map;
        }).collect(Collectors.toList());
        //  赋值订单明细：
        mapResult.put("details",hashMapList);
        //  返回map 集合
        return mapResult;
    }

    /**
     * 获取到orderInfo 子订单
     * @param orderId
     * @param wareSkuMap
     * @return
     */
    @Override
    public List<OrderInfo> orderSplit(String orderId, String wareSkuMap) {
        //  创建子订单集合
        List<OrderInfo> orderInfoList = new ArrayList<>();
        /* 步骤：
        1.  获取原始订单
        2.  wareSkuMap = [{"wareId":"1","skuIds":["22"]},{"wareId":"2","skuIds":["23","24"]}] 转化为JavaObject
        3.  创建一个子订单并给子订单赋值。
        4.  保存子订单
        5.  将子订单添加到集合中
        6.  修改原始订单状态.
         */
        OrderInfo orderInfoOrigin = this.getOrderInfo(Long.parseLong(orderId));
        //  转换数据
        List<Map> mapList = JSONObject.parseArray(wareSkuMap, Map.class);
        //  循环遍历集合
        if (!CollectionUtils.isEmpty(mapList)){
            mapList.forEach(map -> {
                //  获取仓库Id
                String wareId = (String) map.get("wareId");
                List<String> skuIdList = (List<String>) map.get("skuIds");
                //  创建子订单
                OrderInfo subOrderInfo = new OrderInfo();

                BeanUtils.copyProperties(orderInfoOrigin,subOrderInfo);
                //  赋值仓库Id
                subOrderInfo.setWareId(wareId);
                //  指定父级订单Id
                subOrderInfo.setParentOrderId(Long.parseLong(orderId));
                //  属性拷贝：
                //  子订单Id 设置为null 防止主键冲突
                subOrderInfo.setId(null);
                //  计算子订单的金额; 获取到子订单明细即可.
                //  这个集合中 22,23,24
                List<OrderDetail> orderDetailList = orderInfoOrigin.getOrderDetailList();
                //  利用filter 获取到子订单明细集合
                List<OrderDetail> detailList = orderDetailList.stream().filter(orderDetail -> skuIdList.contains(orderDetail.getSkuId().toString())).collect(Collectors.toList());
                subOrderInfo.setOrderDetailList(detailList);
                subOrderInfo.sumTotalAmount();

                //  保证子订单
                this.saveOrderInfo(subOrderInfo);
                //  将子订单添加到集合
                orderInfoList.add(subOrderInfo);
            });
        }
        //  修改原始订单状态.
        this.updateOrderStatus(Long.parseLong(orderId),ProcessStatus.SPLIT);
        //  返回子订单集合
        return orderInfoList;
    }

    @Override
    public void execExpiredOrder(Long orderId, String flag) {

        //  根据订单Id 修改订单状态！
        this.updateOrderStatus(orderId,ProcessStatus.CLOSED);
        //  判断flag
        if ("2".equals(flag)){
            //  应该也会关闭交易记录状态. paymentInfo; 使用mq 异步形式关闭交易记录.
            this.rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_PAYMENT_CLOSE,MqConst.ROUTING_PAYMENT_CLOSE,orderId);
        }
    }

    @Override
    public IPage<OrderInfo> getOrder(Page<OrderInfo> orderInfoPage, String userId, String orderStatus) {
        //  有订单，就需要有订单明细.
        IPage<OrderInfo> infoIPage = orderInfoMapper.selectOrder(orderInfoPage,userId,orderStatus);
        //  需要编写一个订单的状态名称. order.orderStatusName ,循环遍历订单，根据订单的状态获取订单的名称.
        infoIPage.getRecords().forEach(orderInfo -> {
            String statusNameByStatus = OrderStatus.getStatusNameByStatus(orderInfo.getOrderStatus());
            orderInfo.setOrderStatusName(statusNameByStatus);
        });
        return infoIPage;
    }
}




