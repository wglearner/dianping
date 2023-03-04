package com.hpcn.interceptor;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.hpcn.dto.UserDTO;
import com.hpcn.entity.User;
import com.hpcn.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hpcn.utils.RedisConstants.LOGIN_USER_KEY;
import static com.hpcn.utils.RedisConstants.LOGIN_USER_TTL;

public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 判断 ThreadLocal中是否有用户
        if (UserHolder.getUser() == null) {
            response.setStatus(401);
            return false;
        }

        // 有用户，则放行
        return true;
    }
}
