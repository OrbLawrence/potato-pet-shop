package com.petshop.user.controller;

import com.petshop.common.api.Result;
import com.petshop.common.security.BearerTokenResolver;
import com.petshop.common.security.JwtTokenProvider;
import com.petshop.common.security.TokenUser;
import com.petshop.user.entity.Customer;
import com.petshop.user.entity.Employee;
import com.petshop.user.mapper.CustomerMapper;
import com.petshop.user.mapper.EmployeeMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@CrossOrigin
@Slf4j
public class LoginController {

    // 展示节选：已移除原演示环境的硬编码兜底管理员账号。
    // 生产环境请使用数据库账号，并在首次登录后强制修改密码。


    @Autowired
    private CustomerMapper customerMapper;

    @Autowired
    private EmployeeMapper employeeMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private BearerTokenResolver bearerTokenResolver;

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody LoginRequest request) {
        String role = request.getRole();
        log.info("[USER] 登录请求: username={}, role={}", request.getUsername(), role);

        if ("user".equals(role)) {
            Customer customer = customerMapper.getByName(request.getUsername());
            if (customer != null && customer.getPassword() != null
                    && customer.getPassword().equals(request.getPassword())) {
                String token = jwtTokenProvider.generateToken(customer.getName(), "user");
                Map<String, Object> data = new HashMap<>();
                data.put("token", token);
                data.put("username", customer.getName());
                data.put("role", "user");
                data.put("userId", customer.getId());
                data.put("balance", customer.getBalance());
                log.info("[USER] 用户登录成功: username={}, customerId={}", customer.getName(), customer.getId());
                return Result.success(data);
            }
            log.warn("[USER] 用户登录失败: username={}", request.getUsername());
            return Result.error(401, "用户名或密码错误");
        }

        if ("admin".equals(role) || role == null) {
            Employee employee = employeeMapper.getByName(request.getUsername());
            if (employee != null && employee.getPassword() != null
                    && employee.getPassword().equals(request.getPassword())) {
                String token = jwtTokenProvider.generateToken(employee.getName(), "admin");
                Map<String, Object> data = new HashMap<>();
                data.put("token", token);
                data.put("username", employee.getName());
                data.put("role", "admin");
                log.info("[USER] 管理员登录成功: username={}", employee.getName());
                return Result.success(data);
            }


            if (role == null) {
                Customer customer = customerMapper.getByName(request.getUsername());
                if (customer != null && customer.getPassword() != null
                        && customer.getPassword().equals(request.getPassword())) {
                    String token = jwtTokenProvider.generateToken(customer.getName(), "user");
                    Map<String, Object> data = new HashMap<>();
                    data.put("token", token);
                    data.put("username", customer.getName());
                    data.put("role", "user");
                    data.put("userId", customer.getId());
                    data.put("balance", customer.getBalance());
                    log.info("[USER] 用户登录成功: username={}, customerId={}", customer.getName(), customer.getId());
                    return Result.success(data);
                }
            }
            log.warn("[USER] 管理员登录失败: username={}", request.getUsername());
            return Result.error(401, "用户名或密码错误");
        }

        log.warn("[USER] 无效登录角色: role={}", role);
        return Result.error(400, "无效的登录角色");
    }

    @PostMapping("/change-password")
    public Result<Void> changePassword(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                       @RequestBody ChangePasswordRequest request) {
        TokenUser user = bearerTokenResolver.resolve(authHeader);
        if (user == null) {
            return Result.error(401, "登录已过期，请重新登录");
        }
        String username = user.getUsername();
        String role = user.getRole();

        if (request.getNewPassword() == null || request.getNewPassword().isEmpty()) {
            return Result.error(400, "新密码不能为空");
        }

        if ("admin".equals(role)) {
            Employee employee = employeeMapper.getByName(username);
            if (employee != null && employee.getPassword() != null) {
                if (!employee.getPassword().equals(request.getOldPassword())) {
                    return Result.error(400, "原密码错误");
                }
                employeeMapper.updatePassword(username, request.getNewPassword());
                return Result.success();
            }
            if (FALLBACK_ADMIN_USERNAME.equals(username)
                    && FALLBACK_ADMIN_PASSWORD.equals(request.getOldPassword())) {
                Employee newAdmin = new Employee();
                newAdmin.setName(username);
                newAdmin.setPassword(request.getNewPassword());
                newAdmin.setPosition("系统管理员");
                employeeMapper.insert(newAdmin);
                return Result.success();
            }
            return Result.error(400, "原密码错误");
        }

        Customer customer = customerMapper.getByName(username);
        if (customer == null) {
            return Result.error(404, "用户不存在");
        }
        if (!customer.getPassword().equals(request.getOldPassword())) {
            return Result.error(400, "原密码错误");
        }
        customer.setPassword(request.getNewPassword());
        customerMapper.update(customer);
        return Result.success();
    }

    @GetMapping("/user/info")
    public Result<Map<String, Object>> getUserInfo(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        TokenUser user = bearerTokenResolver.resolve(authHeader);
        if (user == null) {
            return Result.error(401, "登录已过期，请重新登录");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("username", user.getUsername());
        data.put("role", user.getRole());

        if ("user".equals(user.getRole())) {
            Customer customer = customerMapper.getByName(user.getUsername());
            if (customer != null) {
                data.put("userId", customer.getId());
                data.put("balance", customer.getBalance());
            }
        }
        return Result.success(data);
    }

    @Data
    public static class LoginRequest {
        private String username;
        private String password;
        private String role;
    }

    @Data
    public static class ChangePasswordRequest {
        private String oldPassword;
        private String newPassword;
    }
}
