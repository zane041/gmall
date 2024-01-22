package com.atguigu.gmall.user.service.impl;

import com.atguigu.gmall.common.util.MD5;
import com.atguigu.gmall.model.user.LoginVo;
import com.atguigu.gmall.model.user.UserInfo;
import com.atguigu.gmall.user.mapper.UserInfoMapper;
import com.atguigu.gmall.user.service.UserInfoService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
* @author admin
* @description 针对表【user_info(用户表)】的数据库操作Service实现
* @createDate 2023-12-21 20:04:55
*/
@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo>
    implements UserInfoService {

    @Override
    public UserInfo login(LoginVo loginVo) {
        LambdaQueryWrapper<UserInfo> userInfoLambdaQueryWrapper = new LambdaQueryWrapper<>();
        userInfoLambdaQueryWrapper.or(query -> {
            query.eq(UserInfo::getLoginName, loginVo.getLoginName())
                    .or().eq(UserInfo::getEmail, loginVo.getLoginName())
                    .or().eq(UserInfo::getPhoneNum, loginVo.getLoginName());
        });
        // 因为密码输入的是暗文！所以这里要加密一下
        String newPwd = MD5.encrypt(loginVo.getPasswd());
        userInfoLambdaQueryWrapper.eq(UserInfo::getPasswd, newPwd);
        return getOne(userInfoLambdaQueryWrapper);
    }
}




