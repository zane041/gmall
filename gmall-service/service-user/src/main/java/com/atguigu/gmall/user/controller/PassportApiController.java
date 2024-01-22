package com.atguigu.gmall.user.controller;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.util.IpUtil;
import com.atguigu.gmall.model.user.LoginVo;
import com.atguigu.gmall.model.user.UserInfo;
import com.atguigu.gmall.user.service.UserInfoService;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Api(tags = "用户登录")
@RestController
@RequestMapping("/api/user/passport")
public class PassportApiController {

    @Autowired
    UserInfoService userInfoService;

    @Autowired
    RedisTemplate redisTemplate;

    /**
     * 用户登录
     * @param loginVo
     * @param request
     * @return
     */
    @PostMapping("/login")
    public Result login(@RequestBody LoginVo loginVo, HttpServletRequest request) {
        UserInfo userInfo = userInfoService.login(loginVo);
        if (userInfo != null) {
            // 创建一个map作为返回给前端数据的载体。后续前端会将这个数据存到cookie中
            Map<String, Object> map = new HashMap<>();
            // 1. 需要一个token。前端后续会使用token判断这个用户是否登录
            String token = UUID.randomUUID().toString();
            map.put("token", token);
            // 2. 需要用户昵称
            map.put("nickName", userInfo.getNickName());
            // 3. 将用户信息存储到redis中。
            String loginKey = RedisConst.USER_LOGIN_KEY_PREFIX + token; //user:login:token
            // 创建一个对象作为redis的value
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("userId", userInfo.getId().toString());
            // token是存储在cookie中的，防止用户盗用token登录，记录认证用户的IP地址
            jsonObject.put("ip", IpUtil.getIpAddress(request));
            redisTemplate.opsForValue().set(loginKey, jsonObject.toString(), RedisConst.USERKEY_TIMEOUT, TimeUnit.SECONDS);
            return Result.ok(map);
        } else {
            return Result.fail().message("登录失败");
        }
    }

    /**
     * 退出登录
     * @param token
     * @param request
     * @return
     */
    @GetMapping("logout")
    public Result logout(@RequestHeader String token, HttpServletRequest request) { // 前端代码中用户登录后将token存到了cookie和header中！
        // 删除cookie数据和缓存数据。cookie数据在前端删除
        redisTemplate.delete(RedisConst.USER_LOGIN_KEY_PREFIX + token);
        return Result.ok();
    }
}
