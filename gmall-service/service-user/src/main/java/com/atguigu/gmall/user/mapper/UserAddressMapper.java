package com.atguigu.gmall.user.mapper;


import com.atguigu.gmall.model.user.UserAddress;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
* @author admin
* @description 针对表【user_address(用户地址表)】的数据库操作Mapper
* @createDate 2023-12-21 20:03:12
* @Entity com.atguigu.gmall.user.model.UserAddress
*/
@Mapper
public interface UserAddressMapper extends BaseMapper<UserAddress> {

}




