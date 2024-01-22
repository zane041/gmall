package com.atguigu.gmall.payment.mapper;


import com.atguigu.gmall.model.payment.PaymentInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
* @author admin
* @description 针对表【payment_info(支付信息表)】的数据库操作Mapper
* @createDate 2024-01-04 14:35:04
* @Entity com.atguigu.gmall.payment.PaymentInfo
*/
@Mapper
public interface PaymentInfoMapper extends BaseMapper<PaymentInfo> {

}




