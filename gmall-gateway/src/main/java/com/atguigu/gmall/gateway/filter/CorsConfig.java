package com.atguigu.gmall.gateway.filter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

/**
 * 配置跨域规则
 *
 * @author: atguigu
 * @create: 2023-02-21 11:18
 */
@Configuration //spring注入容器方式：注解、xml文件
public class CorsConfig {


    /**
     * 配置CorsWebFilter产生CORS过滤器,配置CORS跨域规则
     *
     * @return
     */
    @Bean
    public CorsWebFilter corsWebFilter() { //相当于 <bean class="org.springframework.web.cors.reactive.CorsWebFilter"></bean>
        //配置CORS
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        //1.配置允许访问域名
        corsConfiguration.addAllowedOrigin("*");
        //2.配置允许访问方式 POST DELETE GET
        corsConfiguration.addAllowedMethod("*");
        //3.配置允许提交头信息
        corsConfiguration.addAllowedHeader("*");
        //4.配置是否允许提交cookie
        corsConfiguration.setAllowCredentials(true);
        //5.配置预检请求有效时间
        corsConfiguration.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        //注册CORS配置
        source.registerCorsConfiguration("/**", corsConfiguration);
        return new CorsWebFilter(source);
    }
}
