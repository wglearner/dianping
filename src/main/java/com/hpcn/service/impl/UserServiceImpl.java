package com.hpcn.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hpcn.utils.RedisConstants.*;
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

        // 4, 保存验证码到 Redis 并设置有效期
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY + phone, code, LOGIN_CODE_TTL, TimeUnit.MINUTES);

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
        // 2， 从Redis获取验证码并校验
        Object redisCode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);
        String loginCode = loginForm.getCode();
        if (redisCode == null || !redisCode.toString().equals(loginCode)){
            return Result.fail("验证码错误");
        }
        // 3， 校验通过，根据手机号到数据库查询用户
        User user = query().eq("phone", phone).one();
        // 4， 判断用户是否存在
        if (user == null) {
            // 5，不存在，创建新用户并保存到数据库中
            user = createAndSaveUserWithPhone(phone);
        }

        // 生成Token，保存信息到 Redis中，并设置有效期
        String token = UUID.randomUUID().toString(true);
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        // 转Map格式, 注意 UserDTO 中存在一个 Long 类型的属性，必须转换为 String 类型（StringRedisTemplate的要求）
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO, new HashMap<>(),
                CopyOptions.create()
                        .setIgnoreNullValue(true)
                        .setFieldValueEditor((fieldName, fieldValue) -> fieldValue.toString()));
        String tokenKey = LOGIN_USER_KEY + token;
        // 存入Redis
        stringRedisTemplate.opsForHash().putAll(tokenKey, userMap);
        // 设置有效期
        stringRedisTemplate.expire(tokenKey, LOGIN_USER_TTL, TimeUnit.MINUTES);

        // 返回token
        return Result.ok(token);
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
