package com.atguigu.gmall.payment.controller;

import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.enums.PaymentType;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.payment.config.AlipayConfig;
import com.atguigu.gmall.payment.service.AlipayService;
import com.atguigu.gmall.payment.service.PaymentInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author: atguigu
 * @create: 2023-01-28 16:19
 */
@RestController
@Slf4j
@RequestMapping("/api/payment/alipay")
public class AlipayApiController {

    @Autowired
    private AlipayService alipayService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private PaymentInfoService paymentInfoService;

    /**
     * 生成支付二维码
     * @param orderId
     * @return
     */
    @GetMapping("/submit/{orderId}")
    public String  submitPay(@PathVariable Long orderId){
        //  调用生成二维码方法
        String form = null;
        try {
            form = alipayService.createAliPay(orderId);
        } catch (AlipayApiException e) {
            throw new RuntimeException(e);
        }
        //  返回
        return form; // 这里有@ResponseBody 返回内容给前端页面
    }

    /**
     * 处理支付宝支付页面同步回调-给用户展示支付成功页面
     */
    @GetMapping("/callback/return")
    public void returnPaySuccessPage(HttpServletResponse response) throws IOException {
        //重定向到支付成功页面
        response.sendRedirect(AlipayConfig.return_order_url);
    }

    /**
     * 异步通知
     * @param paramsMap 获取异步通知参数
     * @return
     */
    @PostMapping("/callback/notify")
    public String callbackNotify(@RequestParam HashMap<String,String> paramsMap) {
        System.out.println("死鬼，你才回来...");
        //  Map<String, String> paramsMap = ... //将异步通知中收到的所有参数都存放到map中

        //  调用SDK获取验证签名
        boolean signVerified = false;
        try {
            signVerified = AlipaySignature.rsaCheckV1(paramsMap, AlipayConfig.alipay_public_key, AlipayConfig.charset, AlipayConfig.sign_type);
        } catch (AlipayApiException e) {
            throw new RuntimeException(e);
        }
        //  验签
        if(signVerified){
            //  TODO 验签成功后，按照支付结果异步通知中的描述，对支付结果中的业务内容进行二次校验，校验成功后在response中返回success并继续商户自身业务处理，校验失败返回failure
            // 做校验
            //  1 获取支付宝传递过来的out_trade_no
            String outTradeNo = paramsMap.get("out_trade_no");
            //  2 获取订单总金额
            String totalAmount = paramsMap.get("total_amount");
            //  4 验证 app_id 是否为该商家本身
            String appId = paramsMap.get("app_id");
            //  并且过滤重复的通知结果数据 支付宝针对同一条异步通知重试时，异步通知参数中的 notify_id 是不变的。
            String notifyId = paramsMap.get("notify_id");
            //  获取交易状态 trade_status
            String tradeStatus = paramsMap.get("trade_status");

            try {
                // 校验outTradeNo：判断它是否与商家系统的订单号一致！通过 outTradeNo 且支付类性为支付宝 能查到数据，则成功
                PaymentInfo paymentInfo = paymentInfoService.getPaymentInfo(outTradeNo, PaymentType.ALIPAY.name());
                // 校验 totalAmount、app_id
                if (paymentInfo == null || paymentInfo.getTotalAmount().compareTo(new BigDecimal(totalAmount))!=0) {
    //                        ||  !appId.equals(AlipayConfig.app_id)){
                    return "failure";
                }
                //  保证消息的幂等性! setnx key value; 24*60 = 1440 + 22m
                Boolean result = this.redisTemplate.opsForValue().setIfAbsent(notifyId, notifyId, 1462, TimeUnit.MINUTES);
                if (!result){
                    //  停止后续业务处理！
                    return "failure";
                }
                //  校验交易状态tradeStatus
                if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)){
                    // 细节：防止支付过程中订单关闭了，这里再加个判断
                    if ("PAID".equals(paymentInfo.getPaymentStatus()) || "CLOSED".equals(paymentInfo.getPaymentStatus())) return "failure";
                    //  修改交易状态：payment_status ; callback_time; callback_content; trade_no;
                    paymentInfoService.updatePaymentInfoStatus(outTradeNo,PaymentType.ALIPAY.name(),paramsMap);
                    return "success";
                }
            } catch (Exception e) {
                //  删除缓存的key
                this.redisTemplate.delete(notifyId);
                log.error("处理异步通知失败 {}"+outTradeNo);
                throw new RuntimeException(e);
            }
        }else{
            // TODO 验签失败则记录异常日志，并在response中返回failure.
            return "failure";
        }
        return "failure";
    }

    /**
     * 发起退款
     * @param orderId
     * @return
     */
    @GetMapping("refund/{orderId}")
    public Result refund(@PathVariable(value = "orderId")Long orderId) {
        // 调用退款接口
        boolean flag = alipayService.refund(orderId);
        return Result.ok(flag);
    }

    /**
     * 根据订单ID关闭支付宝交易记录
     * @param orderId 订单ID
     * @return
     */
    @GetMapping("closePay/{orderId}")
    public Boolean closePay(@PathVariable Long orderId){
        Boolean aBoolean = alipayService.closePay(orderId);
        return aBoolean;
    }

    /**
     * 查看是否有交易记录
     * @param orderId
     * @return
     */
    @GetMapping("checkPayment/{orderId}")
    public Boolean checkPayment(@PathVariable Long orderId){
        // 调用退款接口
        boolean flag = alipayService.checkPayment(orderId);
        return flag;
    }

    /**
     * 查看是否有本地交易记录 为什么这里可以直接当参数直接传递进去！
     * @param outTradeNo
     * @return
     */
    @GetMapping("/getPaymentInfo/{outTradeNo}")
    public PaymentInfo getPaymentInfo(@PathVariable String outTradeNo){
        return this.paymentInfoService.getPaymentInfo(outTradeNo,PaymentType.ALIPAY.name());
    }

}
