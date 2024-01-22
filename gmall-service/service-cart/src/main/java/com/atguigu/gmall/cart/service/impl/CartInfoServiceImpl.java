package com.atguigu.gmall.cart.service.impl;

import com.atguigu.gmall.cart.mapper.CartInfoMapper;
import com.atguigu.gmall.cart.service.CartInfoService;
import com.atguigu.gmall.model.cart.CartInfo;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
* @author admin
* @description 针对表【cart_info(购物车表 用户登录系统时更新冗余)】的数据库操作Service实现
* @createDate 2023-12-23 19:14:41
*/
@Service
public class CartInfoServiceImpl extends ServiceImpl<CartInfoMapper, CartInfo>
    implements CartInfoService{
}




