package com.atguigu.gmall.user.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.util.AuthContextHolder;
import com.atguigu.gmall.model.user.UserAddress;
import com.atguigu.gmall.user.service.UserAddressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 *
 */
@RestController
@RequestMapping("/api/user/userAddress")
public class UserApiContrller {

    @Autowired
    private UserAddressService userAddressService;

    /**
     * 获取用户地址列表
     *
     * @param request
     * @return
     */
    @GetMapping("/auth/findUserAddressList")
    public Result findUserAddressList(HttpServletRequest request) {
        String userId = AuthContextHolder.getUserId(request);
        List<UserAddress> userAddressList = userAddressService.getUserAddressList(userId);
        return Result.ok(userAddressList);
    }
}
