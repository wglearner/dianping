package com.hpcn.interceptor;

import com.hpcn.dto.UserDTO;
import com.hpcn.entity.User;
import com.hpcn.utils.UserHolder;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class LoginInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1， 获取 Session 以及 Session中的用户
        HttpSession session = request.getSession();
        Object user = session.getAttribute("user");
        // 2， 判断用户是否存在
        if (user == null) {
            // 不存在
            response.setStatus(401);
            return false;
        }
        // 3， 存在，保存信息到ThreadLocal中
        UserHolder.saveUser((UserDTO) user);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 移除 ThreadLocal
        UserHolder.removeUser();
    }
}
