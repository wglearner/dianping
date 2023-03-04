package com.hpcn.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hpcn.dto.LoginFormDTO;
import com.hpcn.dto.Result;
import com.hpcn.dto.UserDTO;
import com.hpcn.entity.User;
import com.hpcn.mapper.UserMapper;
import com.hpcn.service.IUserService;
import com.hpcn.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import java.util.concurrent.TimeUnit;

import static com.hpcn.utils.RedisConstants.LOGIN_CODE_KEY;
import static com.hpcn.utils.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result sendCode(String phone, HttpSession session) {
        // 1， 校验手机号
        if (RegexUtils.isPhoneInvalid(phone)) {
            // 2， 不符合则返回错误信息
            return Result.fail("手机号格式错误！");
        }
        // 3, 符合则生成验证码
        String code = RandomUtil.randomNumbers(6);

        // 4, 保存验证码到Session
        session.setAttribute(phone, code);
        // 4, 保存验证码到 Redis 并设置有效期
//        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY + phone, code, 5, TimeUnit.MINUTES);

        // 5, 发送验证码 (模拟发送，可通过阿里云、腾讯云注册短信服务)
        log.debug("发送验证码：{}", code);
        // 返回 ok
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        log.debug("Login RequestBody: {}", loginForm);
        // 1，校验手机号
        String phone = loginForm.getPhone();
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号格式错误！");
        }
        // 2， 校验验证码
        Object sessionCode = session.getAttribute(phone);
        String loginCode = loginForm.getCode();
        if (sessionCode == null || !sessionCode.toString().equals(loginCode)){
            return Result.fail("验证码错误");
        }
        // 3， 校验通过，根据手机号到数据库查询用户
        User user = query().eq("phone", phone).one();
        // 4， 判断用户是否存在
        if (user == null) {
            // 5，不存在，创建新用户并保存到数据库中
            user = createAndSaveUserWithPhone(phone);
        }

        // 6, 保存信息到Session中
//        session.setAttribute("user", user);
        session.setAttribute("user", BeanUtil.copyProperties(user, UserDTO.class));

        // 返回ok
        return Result.ok();
    }

    /**
     * 根据手机号，创建新用户并保存到数据库
     * @param phone 用户手机号
     * @return 新用户
     */
    private User createAndSaveUserWithPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(6));
        save(user);
        return user;
    }
}
