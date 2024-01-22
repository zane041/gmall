package com.atguigu.gmall.gateway.filter;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.result.ResultCodeEnum;
import com.atguigu.gmall.common.util.IpUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 全局配置类 —— 拦截所有的请求，包含静态资源
 */
@Component // 这里不用 @Configuration 因为这个注解是xml解析一般里面配合@Bean完成类注册的
public class AuthFilter implements GlobalFilter {

    @Value("${authUrls.url}")
    private String authUrlsUrl;

    @Autowired
    private RedisTemplate redisTemplate;

    private AntPathMatcher antPathMatcher = new AntPathMatcher();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        //获取到当前用户访问的url路径
        String path = request.getURI().getPath();// 如 http://localhost/api/product/inner/getSkuInfo/22 的path是 /api/product/inner/getSkuInfo/22

        // 释放静态资源
        if (antPathMatcher.match("/**/css/**", path) ||
        antPathMatcher.match("/**/js/**", path) ||
        antPathMatcher.match("/**/img/**", path)) {
            //放行，执行下一个过滤器
            chain.filter(exchange);
        }

        // 不能访问内部接口
        if (antPathMatcher.match("/**/inner/**", path)) {
            return out(response, ResultCodeEnum.PERMISSION);
        }

        // 从缓存获取到当前用户ID 若没有则未登录
        String userId = getUserId(request);

        // 如果未登录，不能访问带有 /auth/ 这样的路径
        if (antPathMatcher.match("/**/auth/**", path) &&
                StringUtils.isEmpty(userId)) {
            return out(response, ResultCodeEnum.LOGIN_AUTH);
        }

        // 限制用户访问哪些业务需要登录，配置文件配置了 authUrls.url —— 限制访问的路径白名单
        String[] split = authUrlsUrl.split(",");
        if (split != null && split.length > 0) {
            for (String url : split) {
                // 如果path包含白名单url则需要登录，若用户未登录则跳转到登录页面
                if (path.indexOf(url) != -1 && StringUtils.isEmpty(userId)) {
                    response.setStatusCode(HttpStatus.SEE_OTHER); //303表示内部重定向url
                    response.getHeaders().set(HttpHeaders.LOCATION, "http://passport.gmall.com/login.html?originUrl="+request.getURI());
                    return response.setComplete();
                }
            }
        }

        String userTempId = getUserTempId(request);
        // 将用户ID传给其他微服务使用
        if (!StringUtils.isEmpty(userId) || !StringUtils.isEmpty(userTempId)) {
            // 放在请求头中。登录后的userId或者没登陆的临时用户Id userTempId
            if (!StringUtils.isEmpty(userId)) request.mutate().header("userId", userId).build();
            if (!StringUtils.isEmpty(userTempId)) request.mutate().header("userTempId", userTempId).build();
            return chain.filter(exchange.mutate().request(request).build());
        }

        // 默认放行，执行下一个过滤
        return chain.filter(exchange);
    }

    /**
     * 获取用户ID
     * @param request
     * @return
     */
    private String getUserId(ServerHttpRequest request) {
        // 先获取token。token被存在 header 和 cookie 中
        String token = "";
        HttpCookie httpCookie = request.getCookies().getFirst("token");
        if (httpCookie != null){
            token = httpCookie.getValue();
        } else {
            List<String> stringList = request.getHeaders().get("token");
            if (!CollectionUtils.isEmpty(stringList)) {
                token = stringList.get(0);
            }
        }

        // 如果没有token，则用户未登录，返回
        if (StringUtils.isEmpty(token)) return "";

        // 根据token组成缓存的key去缓存获取用户信息
        String loginKey = "user:login:" + token;
        String userJson = (String) redisTemplate.opsForValue().get(loginKey);
        if (StringUtils.isEmpty(userJson)) return "";
        JSONObject user = JSON.parseObject(userJson);
        // 校验IP地址 —— 防止token被盗用
        String ip = (String) user.get("ip");
        if (ip.equals(IpUtil.getGatwayIpAddress(request))) {
            return (String) user.get("userId");
        } else {
            return "-1"; // 说明非法盗用token
        }
    }

    /**
     * 尝试获取临时用户ID
     *
     * @param request
     * @return
     */
    private String getUserTempId(ServerHttpRequest request) {
        String userTempId = "";

        //1.尝试从cookie中获取
        List<HttpCookie> cookieList = request.getCookies().get("userTempId");
        if (!CollectionUtils.isEmpty(cookieList)) {
            userTempId = cookieList.get(0).getValue();
            return userTempId;
        }

        //2.尝试从请求头中获取
        userTempId = request.getHeaders().getFirst("userTempId");
        if(!StringUtils.isEmpty(userTempId)){
            return userTempId;
        }

        return userTempId;
    }

    /**
     * 拒绝访问 —— 给前端信息提示
     * @param response
     * @param resultCodeEnum
     * @return
     */
    private Mono<Void> out(ServerHttpResponse response, ResultCodeEnum resultCodeEnum) {
        // 获取提示信息
        Result<Object> result = Result.build(null, resultCodeEnum);
        // 输出内容
        DataBuffer wrap = response.bufferFactory().wrap(JSON.toJSONBytes(result));
        // 指定输出的格式：避免中文的时候有乱码: Content-Type
        response.getHeaders().add("Content-Type", "application/json;charset=UTF-8");
        // 返回 Mono
        return response.writeWith(Mono.just(wrap));
    }
}
