package com.petshop.product.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.petshop.common.api.Result;
import com.petshop.common.dto.CustomerDTO;
import com.petshop.common.dto.StoreDTO;
import com.petshop.product.client.StoreClient;
import com.petshop.product.dto.ProductOrderRequest;
import com.petshop.product.entity.Product;
import com.petshop.product.entity.ProductOrder;
import com.petshop.product.entity.ProductOrderItem;
import com.petshop.product.mapper.CustomerBalanceMapper;
import com.petshop.product.mapper.ProductMapper;
import com.petshop.product.mapper.ProductOrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Slf4j
public class ProductOrderService {

    private static final String PAYMENT_BALANCE = "balance";
    private static final String PAYMENT_WECHAT = "wechat";
    private static final String PAYMENT_ALIPAY = "alipay";
    private static final String STATUS_PAID = "paid";
    private static final String STATUS_COMPLETED = "completed";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private ProductOrderMapper productOrderMapper;

    @Autowired
    private CustomerBalanceMapper customerBalanceMapper;

    @Autowired
    private StoreClient storeClient;

    @Autowired
    private ProductService productService;

    @Transactional
    public ProductOrder checkout(CustomerDTO customer, ProductOrderRequest request) {
        log.info("[PRODUCT] 开始下单: customerId={}, storeId={}, channel={}, itemCount={}",
                customer == null ? null : customer.getId(),
                request == null ? null : request.getStoreId(),
                request == null ? null : request.getPaymentChannel(),
                request == null || request.getItems() == null ? 0 : request.getItems().size());
        if (request == null || request.getStoreId() == null) {
            throw new RuntimeException("请选择下单门店");
        }
        if (!isSupportedPaymentChannel(request.getPaymentChannel())) {
            throw new RuntimeException("不支持该支付方式");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new RuntimeException("购物车不能为空");
        }

        StoreDTO store = resolveStore(request.getStoreId());
        log.info("[PRODUCT] 门店校验通过: storeId={}, storeName={}", store.getId(), store.getName());

        Map<Long, Integer> quantities = mergeQuantities(request.getItems());
        List<ProductOrderItem> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
            Product product = productMapper.getById(entry.getKey());
            int quantity = entry.getValue();
            if (product == null || product.getStatus() == null || product.getStatus() != 1) {
                throw new RuntimeException("商品已下架或不存在");
            }
            if (product.getStock() == null || product.getStock() < quantity) {
                throw new RuntimeException(product.getName() + " 库存不足");
            }
            log.info("[PRODUCT] 商品校验通过: productId={}, name={}, price={}, quantity={}, stock={}",
                    product.getId(), product.getName(), product.getPrice(), quantity, product.getStock());

            ProductOrderItem item = new ProductOrderItem();
            item.setProductId(product.getId());
            item.setProductName(product.getName());
            item.setProductImage(product.getImageUrl());
            item.setPrice(product.getPrice());
            item.setQuantity(quantity);
            item.setSubtotal(product.getPrice().multiply(BigDecimal.valueOf(quantity)));
            items.add(item);
            total = total.add(item.getSubtotal());
        }

        for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
            if (productMapper.decreaseStock(entry.getKey(), entry.getValue()) == 0) {
                throw new RuntimeException("商品库存不足，请刷新后重试");
            }
        }
        log.info("[PRODUCT] 库存扣减完成: productCount={}", quantities.size());

        if (PAYMENT_BALANCE.equals(request.getPaymentChannel())
                && customerBalanceMapper.deductBalance(customer.getId(), total) == 0) {
            throw new RuntimeException("账户余额不足，请选择其他支付方式");
        }
        if (PAYMENT_BALANCE.equals(request.getPaymentChannel())) {
            log.info("[PRODUCT] 余额支付扣款成功: customerId={}, amount={}", customer.getId(), total);
        } else {
            log.info("[PRODUCT] 模拟支付成功: customerId={}, channel={}, amount={}",
                    customer.getId(), request.getPaymentChannel(), total);
        }

        LocalDateTime now = LocalDateTime.now();
        ProductOrder order = new ProductOrder();
        order.setOrderNo(generateOrderNo());
        order.setCustomerId(customer.getId());
        order.setCustomerName(customer.getName());
        order.setStoreId(store.getId());
        order.setStoreName(store.getName());
        order.setAmount(total);
        order.setPaymentChannel(request.getPaymentChannel());
        order.setStatus(STATUS_PAID);
        order.setVerificationCode(generateVerificationCode());
        order.setTransactionNo(generateTransactionNo(request.getPaymentChannel()));
        order.setCreateTime(now);
        order.setPayTime(now);
        productOrderMapper.insert(order);
        log.info("[PRODUCT] 订单主表写入成功: orderId={}, orderNo={}, amount={}",
                order.getId(), order.getOrderNo(), order.getAmount());

        for (ProductOrderItem item : items) {
            item.setOrderId(order.getId());
            productOrderMapper.insertItem(item);
        }
        log.info("[PRODUCT] 订单明细写入成功: orderId={}, itemCount={}", order.getId(), items.size());
        order.setItems(items);
        productService.evictListCache();
        log.info("[PRODUCT] 下单完成: orderNo={}, customerId={}, amount={}",
                order.getOrderNo(), customer.getId(), order.getAmount());
        return order;
    }

    public PageInfo<ProductOrder> page(int pageNum, int pageSize, String keyword, String status) {
        PageHelper.startPage(pageNum, pageSize);
        List<ProductOrder> orders = productOrderMapper.page(keyword, status);
        fillItems(orders);
        return new PageInfo<>(orders);
    }

    public PageInfo<ProductOrder> pageByCustomer(Long customerId, int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<ProductOrder> orders = productOrderMapper.listByCustomerId(customerId);
        fillItems(orders);
        return new PageInfo<>(orders);
    }

    public ProductOrder getById(Long id) {
        ProductOrder order = productOrderMapper.getById(id);
        if (order != null) {
            order.setItems(productOrderMapper.listItems(id));
        }
        return order;
    }

    public ProductOrder getByIdForCustomer(Long id, Long customerId) {
        ProductOrder order = getById(id);
        if (order == null || !customerId.equals(order.getCustomerId())) {
            return null;
        }
        return order;
    }

    public boolean complete(Long id) {
        if (productOrderMapper.complete(id) > 0) {
            log.info("[PRODUCT] 订单核销成功: orderId={}", id);
            return true;
        }
        ProductOrder order = productOrderMapper.getById(id);
        boolean alreadyCompleted = order != null && STATUS_COMPLETED.equals(order.getStatus());
        if (alreadyCompleted) {
            log.info("[PRODUCT] 订单已核销，无需重复操作: orderId={}", id);
        } else {
            log.warn("[PRODUCT] 订单核销失败: orderId={}, status={}", id, order == null ? null : order.getStatus());
        }
        return alreadyCompleted;
    }

    public boolean isSupportedPaymentChannel(String channel) {
        return PAYMENT_BALANCE.equals(channel)
                || PAYMENT_WECHAT.equals(channel)
                || PAYMENT_ALIPAY.equals(channel);
    }

    private StoreDTO resolveStore(Long storeId) {
        log.info("[PRODUCT] 调用 Feign: pet-biz-service POST /internal/stores/batch, storeId={}", storeId);
        Result<List<StoreDTO>> result = storeClient.listByIds(Collections.singletonList(storeId));
        if (result == null || !Integer.valueOf(200).equals(result.getCode())
                || result.getData() == null || result.getData().isEmpty()) {
            log.warn("[PRODUCT] Feign 门店查询失败: storeId={}, result={}", storeId, result == null ? null : result.getCode());
            throw new RuntimeException("所选门店不存在");
        }
        log.info("[PRODUCT] Feign 门店查询返回: storeId={}, storeName={}",
                result.getData().get(0).getId(), result.getData().get(0).getName());
        return result.getData().get(0);
    }

    private Map<Long, Integer> mergeQuantities(List<ProductOrderRequest.Item> items) {
        Map<Long, Integer> quantities = new LinkedHashMap<>();
        for (ProductOrderRequest.Item item : items) {
            if (item == null || item.getProductId() == null
                    || item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new RuntimeException("商品数量不正确");
            }
            if (item.getQuantity() > 99) {
                throw new RuntimeException("单件商品最多购买99件");
            }
            quantities.merge(item.getProductId(), item.getQuantity(), Integer::sum);
            if (quantities.get(item.getProductId()) > 99) {
                throw new RuntimeException("单件商品最多购买99件");
            }
        }
        return quantities;
    }

    private void fillItems(List<ProductOrder> orders) {
        for (ProductOrder order : orders) {
            order.setItems(productOrderMapper.listItems(order.getId()));
        }
    }

    private String generateVerificationCode() {
        for (int i = 0; i < 30; i++) {
            String code = String.valueOf(100000 + SECURE_RANDOM.nextInt(900000));
            if (productOrderMapper.countByVerificationCode(code) == 0) {
                return code;
            }
        }
        throw new RuntimeException("核销码生成失败，请重试");
    }

    private String generateOrderNo() {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        return "SP" + time + ThreadLocalRandom.current().nextInt(100, 1000);
    }

    private String generateTransactionNo(String channel) {
        return channel.toUpperCase() + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
    }
}
