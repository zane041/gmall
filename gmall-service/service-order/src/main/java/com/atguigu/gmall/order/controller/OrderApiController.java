package com.atguigu.gmall.order.controller;

import com.atguigu.gmall.cart.client.CartFeignClient;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.util.AuthContextHolder;
import com.atguigu.gmall.model.cart.CartInfo;
import com.atguigu.gmall.model.enums.ProcessStatus;
import com.atguigu.gmall.model.order.OrderDetail;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.order.service.OrderInfoService;
import com.atguigu.gmall.product.client.ProductFeignClient;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import org.apache.commons.lang.StringUtils;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/order")
public class OrderApiController {

    @Autowired
    private CartFeignClient cartFeignClient;

    @Autowired
    private OrderInfoService orderInfoService;

    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

    /**
     * 结算页面数据
     * @param request
     * @return
     */
    @GetMapping("/auth/trade")
    public Result trade(HttpServletRequest request) {
        String userId = AuthContextHolder.getUserId(request);
        // 创建结果集
        HashMap<String, Object> dataMap = new HashMap<>();
        // 返回订单明细
        List<CartInfo> cartCheckedList = cartFeignClient.getCartCheckedList(Long.parseLong(userId));
        List<OrderDetail> detailArrayList = new ArrayList<>();
        AtomicInteger totalNum = new AtomicInteger(); // 原子类：AtomicInteger
        if (!CollectionUtils.isEmpty(cartCheckedList)) {
            detailArrayList = cartCheckedList.stream().map(cartInfo -> {
                OrderDetail orderDetail = new OrderDetail();
                orderDetail.setSkuId(cartInfo.getSkuId());
                orderDetail.setSkuName(cartInfo.getSkuName());
                orderDetail.setImgUrl(cartInfo.getImgUrl());
                orderDetail.setSkuNum(cartInfo.getSkuNum());
                orderDetail.setOrderPrice(cartInfo.getSkuPrice());
                // +=总件数
                totalNum.getAndAdd(cartInfo.getSkuNum());
                return orderDetail;
            }).collect(Collectors.toList());
        }
        dataMap.put("detailArrayList", detailArrayList);
        //  返回总金额 totalAmount
        BigDecimal totalAmount = new BigDecimal(0);
        if (!CollectionUtils.isEmpty(detailArrayList)) {
            OrderInfo orderInfo = new OrderInfo();
            orderInfo.setOrderDetailList(detailArrayList);
            orderInfo.sumTotalAmount();
            totalAmount = orderInfo.getTotalAmount();
        }
        dataMap.put("totalAmount", totalAmount);
        //  返回总件数 totalNum
        dataMap.put("totalNum",totalNum);
        //5.TODO 防止订单重复提交生成唯一流水号
        String tradeCode = orderInfoService.getTradeCode(userId);
        dataMap.put("tradeNo", tradeCode);
        return Result.ok(dataMap);
    }

    /**
     * 提交订单
     * @param orderInfo
     * @param request
     * @return
     */
    @PostMapping("auth/submitOrder")
    public Result submitOrder(@RequestBody OrderInfo orderInfo, HttpServletRequest request) {
        // 获取到用户Id。在这里放用户id因为service层获取不到
        String userId = AuthContextHolder.getUserId(request);
        orderInfo.setUserId(Long.parseLong(userId));

        // 收集 异步线程
        List<CompletableFuture>  futureList = new ArrayList<>();
        // 收集 错误信息
        List<String> errorMessageList = new ArrayList<>();

        // 防止用户无刷新重复提交验证
        CompletableFuture<Void> repeatFuture = CompletableFuture.runAsync(() -> {
            String tradeCode = request.getParameter("tradeNo");
            boolean result = orderInfoService.checkTradeCode(userId, tradeCode);
            if (!result) {
                errorMessageList.add("订单不能无刷新重复提交！请刷新页面");
            }
            // 删除交易号
            orderInfoService.deleteTradeCode(userId);
        }, threadPoolExecutor);
        futureList.add(repeatFuture);

        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            // 校验商品库存
            CompletableFuture<Void> stockFuture = CompletableFuture.runAsync(() -> {
                boolean hasStock = orderInfoService.checkStock(orderDetail.getSkuId(), orderDetail.getSkuNum());
                if (!hasStock) errorMessageList.add(orderDetail.getSkuName() + "：该商品库存不足！");
            }, threadPoolExecutor);
            futureList.add(stockFuture);

            // 校验商品价格
            CompletableFuture<Void> priceFuture = CompletableFuture.runAsync(() -> {
                BigDecimal orderPrice = orderDetail.getOrderPrice();
                BigDecimal skuPrice = productFeignClient.getSkuPrice(orderDetail.getSkuId());
                if (orderPrice.compareTo(skuPrice) != 0) {
                    // 价格变了，更新购物车价格 —— 我们删掉购物车缓存信息，这样用户重新加载页面时会走数据库查信息
                    if (redisTemplate.hasKey(getCartKey(userId))) redisTemplate.delete(getCartKey(userId));
                    // 详细提示信息
                    String msg = orderPrice.compareTo(skuPrice) == 1 ? "降价" : "涨价";
                    BigDecimal price = orderPrice.subtract(skuPrice).abs();
                    errorMessageList.add(orderDetail.getSkuName() + "：价格发生变化——" + msg + price + "！请刷新页面！");
                }
            }, threadPoolExecutor);
            futureList.add(priceFuture);
        }

        // 执行异步线程
        CompletableFuture.allOf(futureList.toArray(new CompletableFuture[futureList.size()])).join();
        // 如果有错误信息，返回报错给前端
        if (!CollectionUtils.isEmpty(errorMessageList)) {
            return Result.fail().message(StringUtils.join(errorMessageList, "\n"));
        }

        // 验证通过，保存订单！
        Long orderId = orderInfoService.saveOrderInfo(orderInfo);

        return Result.ok(orderId);
    }

    /**
     * 获取购物车在缓存中的key
     * @param userId
     * @return
     */
    private static String getCartKey(String userId) {
        String cartKey = RedisConst.USER_KEY_PREFIX + userId + RedisConst.USER_CART_KEY_SUFFIX;
        return cartKey;
    }

    /**
     * 内部调用获取订单
     * @param orderId
     * @return
     */
    @GetMapping("inner/getOrderInfo/{orderId}")
    public OrderInfo getOrderInfo(@PathVariable(value = "orderId") Long orderId){
        return orderInfoService.getOrderInfo(orderId);
    }

    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_ORDER_CLOSED,durable = "true",autoDelete = "false"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_ORDER_CLOSED),
            key = {MqConst.ROUTING_ORDER_CLOSED}
    ))
    public void  orderClosed(Long orderId, Message message, Channel channel){
        //  判断当前orderId
        try {
            if (orderId!=null){
                //  查询订单对象
                OrderInfo orderInfo = orderInfoService.getById(orderId);
                if (!"CLOSED".equals(orderInfo.getOrderStatus()) && !"CLOSED".equals(orderInfo.getProcessStatus())){
                    //  调用关闭订单方法.
                    orderInfoService.updateOrderStatus(orderId, ProcessStatus.CLOSED);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        //  手动确认
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }

    /**
     * 拆单 http://localhost:8204/api/order/orderSplit?orderId=xxx&wareSkuMap=xxx
     * @param request
     * @return
     */
    @PostMapping("/orderSplit")
    public List<Map<String, Object>> orderSplit(HttpServletRequest request){
        //  获取参数
        String orderId = request.getParameter("orderId");
        //  [{"wareId":"1","skuIds":["22"]},{"wareId":"2","skuIds":["23",,"24"]}]
        String wareSkuMap = request.getParameter("wareSkuMap");
        //  调用服务层方法. 获取 OrderInfo 构成的子订单
        List<OrderInfo> orderInfoList = orderInfoService.orderSplit(orderId,wareSkuMap);
        //  改造成我们需要的数据 获取 map 够成的子订单
        List<Map<String, Object>> mapList = orderInfoList.stream().map(orderInfo -> {
            //  将orderInfo 转换为map 集合
            Map<String, Object> map = orderInfoService.initWare(orderInfo);
            return map;
        }).collect(Collectors.toList());
        //  真正的子订单集合
        return mapList;
    }

    /**
     * 查看我的订单
     * @param page
     * @param limit
     * @param request
     * @return
     */
    @GetMapping("/auth/{page}/{limit}")
    public Result getOrder(@PathVariable Long page,
                           @PathVariable Long limit,
                           HttpServletRequest request){
        //  查看我的订单. userId
        String userId = AuthContextHolder.getUserId(request);
        //  获取订单状态,
        String orderStatus = request.getParameter("orderStatus");
        //  带分页的查询
        Page<OrderInfo> orderInfoPage = new Page<>(page,limit);
        //  调用服务层方法.
        IPage<OrderInfo> infoIPage = orderInfoService.getOrder(orderInfoPage,userId,orderStatus);
        //  返回封装的数据
        return Result.ok(infoIPage);
    }

    /**
     * 秒杀提交订单，秒杀订单不需要做前置判断，直接下单
     * @param orderInfo
     * @return
     */
    @PostMapping("inner/seckill/submitOrder")
    public Long submitOrder(@RequestBody OrderInfo orderInfo) {
        Long orderId = orderInfoService.saveOrderInfo(orderInfo);
        return orderId;
    }
}
