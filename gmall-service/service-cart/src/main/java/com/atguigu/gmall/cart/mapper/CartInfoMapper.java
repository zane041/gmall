package com.atguigu.gmall.cart.mapper;

import com.atguigu.gmall.model.cart.CartInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
* @author admin
* @description 针对表【cart_info(购物车表 用户登录系统时更新冗余)】的数据库操作Mapper
* @createDate 2023-12-23 19:14:41
* @Entity com.atguigu.gmall.model.cart.CartInfo
*/
@Mapper
public interface CartInfoMapper extends BaseMapper<CartInfo> {

}




