package com.atguigu.gmall.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;

/**
 * 用户认证接口
 */
@Controller
public class PassportController {

    @GetMapping("login.html")
    public String login(HttpServletRequest request) {
        //  存储数据
        request.setAttribute("originUrl", request.getParameter("originUrl"));
        //  返回登录页面
        return "login";
    }
}
