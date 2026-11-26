package com.petshop.user.controller;

import com.github.pagehelper.PageInfo;
import com.petshop.common.api.Result;
import com.petshop.common.security.BearerTokenResolver;
import com.petshop.common.security.TokenUser;
import com.petshop.user.entity.Customer;
import com.petshop.user.entity.RechargeOrder;
import com.petshop.user.mapper.CustomerMapper;
import com.petshop.user.service.RechargeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@CrossOrigin
@RequestMapping("/user")
public class RechargeController {

    @Autowired
    private CustomerMapper customerMapper;

    @Autowired
    private RechargeService rechargeService;

    @Autowired
    private BearerTokenResolver bearerTokenResolver;

    @PostMapping("/recharge")
    public Result<Map<String, Object>> recharge(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                                @RequestBody Map<String, Object> body) {
        Customer customer = resolveCustomer(authHeader);
        if (customer == null) {
            return Result.error(401, "未登录或登录已过期");
        }
        if (body.get("amount") == null) {
            return Result.error(400, "充值金额不能为空");
        }

        BigDecimal amount = new BigDecimal(body.get("amount").toString());
        BigDecimal newBalance = customer.getBalance() == null
                ? amount : customer.getBalance().add(amount);
        customerMapper.updateBalance(customer.getId(), newBalance);

        Map<String, Object> data = new HashMap<>();
        data.put("balance", newBalance);
        data.put("amount", amount);
        return Result.success(data);
    }

    @PostMapping("/recharge/order")
    public Result<Map<String, Object>> createOrder(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                                   @RequestBody Map<String, Object> body) {
        Customer customer = resolveCustomer(authHeader);
        if (customer == null) {
            return Result.error(401, "未登录或登录已过期");
        }

        Object amountObj = body.get("amount");
        Object channelObj = body.get("channel");
        if (amountObj == null || channelObj == null) {
            return Result.error(400, "充值金额和支付渠道不能为空");
        }

        BigDecimal amount;
        try {
            amount = new BigDecimal(amountObj.toString());
        } catch (NumberFormatException e) {
            return Result.error(400, "充值金额格式不正确");
        }

        String channel = channelObj.toString();
        if (!rechargeService.isValidAmount(amount)) {
            return Result.error(400, "充值金额必须大于0");
        }
        if (!rechargeService.isSupportedChannel(channel)) {
            return Result.error(400, "仅支持微信支付或支付宝");
        }

        RechargeOrder order = rechargeService.createOrder(customer.getId(), amount, channel);
        Map<String, Object> data = new HashMap<>();
        data.put("orderNo", order.getOrderNo());
        data.put("amount", order.getAmount());
        data.put("channel", order.getChannel());
        data.put("status", order.getStatus());
        return Result.success(data);
    }

    @PostMapping("/recharge/mock-pay")
    public Result<Map<String, Object>> mockPay(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                               @RequestBody Map<String, Object> body) {
        Customer customer = resolveCustomer(authHeader);
        if (customer == null) {
            return Result.error(401, "未登录或登录已过期");
        }

        Object orderNoObj = body.get("orderNo");
        if (orderNoObj == null || orderNoObj.toString().trim().isEmpty()) {
            return Result.error(400, "订单号不能为空");
        }

        try {
            return Result.success(rechargeService.mockPay(customer.getId(), orderNoObj.toString().trim()));
        } catch (RuntimeException e) {
            return Result.error(400, e.getMessage());
        }
    }

    @GetMapping("/recharge/orders")
    public Result<PageInfo<RechargeOrder>> orders(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                                  @RequestParam(defaultValue = "1") int pageNum,
                                                  @RequestParam(defaultValue = "10") int pageSize) {
        Customer customer = resolveCustomer(authHeader);
        if (customer == null) {
            return Result.error(401, "未登录或登录已过期");
        }
        return Result.success(rechargeService.pageOrders(customer.getId(), pageNum, pageSize));
    }

    @GetMapping("/recharge/order/{orderNo}")
    public Result<RechargeOrder> orderDetail(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                             @PathVariable String orderNo) {
        Customer customer = resolveCustomer(authHeader);
        if (customer == null) {
            return Result.error(401, "未登录或登录已过期");
        }
        RechargeOrder order = rechargeService.getOrder(customer.getId(), orderNo);
        return order == null ? Result.error(404, "订单不存在") : Result.success(order);
    }

    private Customer resolveCustomer(String authHeader) {
        TokenUser user = bearerTokenResolver.resolve(authHeader);
        if (user == null || !"user".equals(user.getRole())) {
            return null;
        }
        return customerMapper.getByName(user.getUsername());
    }
}
