package com.hpcn.config;

import com.hpcn.interceptor.LoginInterceptor;
import com.hpcn.interceptor.RefreshTokenInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

@Configuration
public class MvcConfig implements WebMvcConfigurer {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注意拦截器是有顺序的

        // 第一个拦截器，Token刷新拦截器。
        // 拦截所有路径，根据需要刷新Token有效期
        registry.addInterceptor(new RefreshTokenInterceptor(stringRedisTemplate)).order(0);

        // 第二个拦截器，登录拦截器。
        // 拦截部分路径，判断用户是否存在。注意excludePathPatterns()里的路径是不拦截直接放行的路径，别搞混了
        registry.addInterceptor(new LoginInterceptor())
                .excludePathPatterns(
                        "/user/code",
                        "/user/login",
                        "/blog/hot",
                        "/shop/**",
                        "/shop-type/**",
                        "/upload/**",
                        "/voucher/**"
                ).order(1);
    }
}
