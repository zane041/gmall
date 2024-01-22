package com.atguigu.gmall.user.service;


import com.atguigu.gmall.model.user.UserAddress;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
* @author admin
* @description 针对表【user_address(用户地址表)】的数据库操作Service
* @createDate 2023-12-21 20:03:12
*/
public interface UserAddressService extends IService<UserAddress> {

    /**
     * 获取用户地址列表
     * @param userId
     * @return
     */
    List<UserAddress> getUserAddressList(String userId);
}
