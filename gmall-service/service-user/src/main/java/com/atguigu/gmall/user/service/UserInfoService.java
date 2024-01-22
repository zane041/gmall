package com.atguigu.gmall.user.service;


import com.atguigu.gmall.model.user.LoginVo;
import com.atguigu.gmall.model.user.UserInfo;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author admin
* @description 针对表【user_info(用户表)】的数据库操作Service
* @createDate 2023-12-21 20:04:55
*/
public interface UserInfoService extends IService<UserInfo> {

    /**
     * 登录
     * @param loginVo
     * @return
     */
    UserInfo login(LoginVo loginVo);
}
