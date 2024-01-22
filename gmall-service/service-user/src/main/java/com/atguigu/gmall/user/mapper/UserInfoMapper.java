package com.atguigu.gmall.user.mapper;


import com.atguigu.gmall.model.user.UserInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
* @author admin
* @description 针对表【user_info(用户表)】的数据库操作Mapper
* @createDate 2023-12-21 20:04:55
* @Entity com.atguigu.gmall.user.model.UserInfo
*/
@Mapper
public interface UserInfoMapper extends BaseMapper<UserInfo> {

}




