package com.hpcn.interceptor;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.hpcn.dto.UserDTO;
import com.hpcn.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hpcn.utils.RedisConstants.LOGIN_USER_KEY;
import static com.hpcn.utils.RedisConstants.LOGIN_USER_TTL;

/**
 * 第一个拦截器，拦截所有请求
 * 功能：Token存在则刷新有效期，不存在则直接放行到下一个拦截器
 */
public class RefreshTokenInterceptor implements HandlerInterceptor {

    // 注意这里不能用 Autowired 或者 Resource 注解
    // 因为这个拦截器类并没有加 Component 注解，不归Spring容器管理，因此不能自动装配和注入
    // 可以使用构造器注入，或者加上 Component 注解（没尝试过）
    private StringRedisTemplate stringRedisTemplate;

    public RefreshTokenInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1， 获取请求头中的 token
        String token = request.getHeader("authorization");
        if (StrUtil.isBlank(token)) {
            // token 不存在，直接放行到第2个拦截器处
            return true;
        }

        // 2. 根据Token获取redis中的用户
        String tokenKey = LOGIN_USER_KEY + token;
        Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(tokenKey);
        // 判断用户是否存在
        if (userMap.isEmpty()) {
            // 不存在，直接放行
            return true;
        }
        // 查询结果转化为 UserDTO对象
        UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false);

        // 3， 保存信息到ThreadLocal中
        UserHolder.saveUser(userDTO);

        // 4， 刷新 token 有效期
        stringRedisTemplate.expire(tokenKey, LOGIN_USER_TTL, TimeUnit.MINUTES);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 移除 ThreadLocal
        UserHolder.removeUser();
    }
}
