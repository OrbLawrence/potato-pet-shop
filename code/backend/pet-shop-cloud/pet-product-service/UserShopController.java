package com.petshop.product.controller;

import com.github.pagehelper.PageInfo;
import com.petshop.common.api.Result;
import com.petshop.common.dto.CustomerDTO;
import com.petshop.common.security.BearerTokenResolver;
import com.petshop.common.security.TokenUser;
import com.petshop.product.client.CustomerClient;
import com.petshop.product.dto.ProductOrderRequest;
import com.petshop.product.entity.Product;
import com.petshop.product.entity.ProductOrder;
import com.petshop.product.mapper.CustomerBalanceMapper;
import com.petshop.product.service.ProductOrderService;
import com.petshop.product.service.ProductService;
import lombok.extern.slf4j.Slf4j;
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
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/user/shop")
@CrossOrigin
@Slf4j
public class UserShopController {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductOrderService productOrderService;

    @Autowired
    private CustomerClient customerClient;

    @Autowired
    private CustomerBalanceMapper customerBalanceMapper;

    @Autowired
    private BearerTokenResolver bearerTokenResolver;

    @GetMapping("/products")
    public Result<List<Product>> products(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        CustomerDTO customer = resolveCustomer(authHeader);
        if (customer == null) {
            return Result.error(401, "未登录或登录已过期");
        }
        return Result.success(productService.listOnSale());
    }

    @GetMapping("/products/{id}")
    public Result<Product> productDetail(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                         @PathVariable Long id) {
        CustomerDTO customer = resolveCustomer(authHeader);
        if (customer == null) {
            return Result.error(401, "未登录或登录已过期");
        }
        Product product = productService.getById(id);
        if (product == null || product.getStatus() == null || product.getStatus() != 1) {
            return Result.error(404, "商品不存在或已下架");
        }
        return Result.success(product);
    }

    @PostMapping("/orders")
    public Result<Map<String, Object>> checkout(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                                @RequestBody ProductOrderRequest request) {
        CustomerDTO customer = resolveCustomer(authHeader);
        if (customer == null) {
            return Result.error(401, "未登录或登录已过期");
        }
        try {
            ProductOrder order = productOrderService.checkout(customer, request);
            BigDecimal latest = customerBalanceMapper.selectBalanceById(customer.getId());
            Map<String, Object> data = new HashMap<>();
            data.put("order", order);
            data.put("balance", latest == null ? customer.getBalance() : latest);
            return Result.success(data);
        } catch (RuntimeException e) {
            return Result.error(400, e.getMessage());
        }
    }

    @GetMapping("/orders")
    public Result<PageInfo<ProductOrder>> orders(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                                 @RequestParam(defaultValue = "1") int pageNum,
                                                 @RequestParam(defaultValue = "10") int pageSize) {
        CustomerDTO customer = resolveCustomer(authHeader);
        if (customer == null) {
            return Result.error(401, "未登录或登录已过期");
        }
        return Result.success(productOrderService.pageByCustomer(customer.getId(), pageNum, pageSize));
    }

    @GetMapping("/orders/{id}")
    public Result<ProductOrder> orderDetail(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                            @PathVariable Long id) {
        CustomerDTO customer = resolveCustomer(authHeader);
        if (customer == null) {
            return Result.error(401, "未登录或登录已过期");
        }
        ProductOrder order = productOrderService.getByIdForCustomer(id, customer.getId());
        return order == null ? Result.error(404, "订单不存在") : Result.success(order);
    }

    private CustomerDTO resolveCustomer(String authHeader) {
        TokenUser user = bearerTokenResolver.resolve(authHeader);
        if (user == null || !"user".equals(user.getRole())) {
            return null;
        }
        try {
            log.info("[PRODUCT] 调用 Feign: pet-user-service GET /internal/customers/by-name/{}, username={}",
                    user.getUsername(), user.getUsername());
            Result<CustomerDTO> result = customerClient.getByName(user.getUsername());
            if (result == null || !Integer.valueOf(200).equals(result.getCode())) {
                log.warn("[PRODUCT] Feign 客户查询失败: username={}, code={}",
                        user.getUsername(), result == null ? null : result.getCode());
                return null;
            }
            log.info("[PRODUCT] Feign 客户查询返回: customerId={}, customerName={}",
                    result.getData() == null ? null : result.getData().getId(),
                    result.getData() == null ? null : result.getData().getName());
            return result.getData();
        } catch (Exception e) {
            log.warn("[PRODUCT] Feign 客户查询异常: username={}, error={}", user.getUsername(), e.getMessage());
            return null;
        }
    }
}
