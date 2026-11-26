package com.petshop.user.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.petshop.user.entity.Customer;
import com.petshop.user.entity.RechargeOrder;
import com.petshop.user.mapper.CustomerMapper;
import com.petshop.user.mapper.RechargeOrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Slf4j
public class RechargeService {

    private static final String STATUS_PENDING = "pending";
    private static final String STATUS_SUCCESS = "success";
    private static final String CHANNEL_WECHAT = "wechat";
    private static final String CHANNEL_ALIPAY = "alipay";

    @Autowired
    private RechargeOrderMapper rechargeOrderMapper;

    @Autowired
    private CustomerMapper customerMapper;

    public RechargeOrder createOrder(Long customerId, BigDecimal amount, String channel) {
        log.info("[USER] 创建充值订单: customerId={}, amount={}, channel={}", customerId, amount, channel);
        RechargeOrder order = new RechargeOrder();
        order.setOrderNo(generateOrderNo());
        order.setCustomerId(customerId);
        order.setAmount(amount);
        order.setChannel(channel);
        order.setStatus(STATUS_PENDING);
        order.setCreateTime(LocalDateTime.now());
        order.setRemark("模拟充值");
        rechargeOrderMapper.insert(order);
        log.info("[USER] 充值订单创建成功: orderNo={}, customerId={}, amount={}",
                order.getOrderNo(), customerId, amount);
        return order;
    }

    @Transactional
    public Map<String, Object> mockPay(Long customerId, String orderNo) {
        log.info("[USER] 模拟充值支付开始: customerId={}, orderNo={}", customerId, orderNo);
        RechargeOrder order = rechargeOrderMapper.getByOrderNoForUpdate(orderNo);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!order.getCustomerId().equals(customerId)) {
            throw new RuntimeException("无权操作该订单");
        }
        if (STATUS_SUCCESS.equals(order.getStatus())) {
            return buildPayResult(order, customerId);
        }
        if (!STATUS_PENDING.equals(order.getStatus())) {
            throw new RuntimeException("订单状态异常，无法支付");
        }

        try {
            Thread.sleep(1500 + ThreadLocalRandom.current().nextInt(1001));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String transactionNo = generateTransactionNo(order.getChannel());
        LocalDateTime payTime = LocalDateTime.now();
        int rows = rechargeOrderMapper.markSuccess(orderNo, customerId, transactionNo, payTime);
        if (rows == 0) {
            RechargeOrder latest = rechargeOrderMapper.getByOrderNo(orderNo);
            if (latest != null && STATUS_SUCCESS.equals(latest.getStatus())) {
                return buildPayResult(latest, customerId);
            }
            throw new RuntimeException("订单状态异常，无法支付");
        }

        customerMapper.addBalance(customerId, order.getAmount());
        log.info("[USER] 充值余额已入账: customerId={}, amount={}, transactionNo={}",
                customerId, order.getAmount(), transactionNo);

        order.setTransactionNo(transactionNo);
        order.setPayTime(payTime);
        order.setStatus(STATUS_SUCCESS);
        return buildPayResult(order, customerId);
    }

    public PageInfo<RechargeOrder> pageOrders(Long customerId, int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<RechargeOrder> list = rechargeOrderMapper.listByCustomerId(customerId);
        return new PageInfo<>(list);
    }

    public RechargeOrder getOrder(Long customerId, String orderNo) {
        return rechargeOrderMapper.getByOrderNoAndCustomerId(orderNo, customerId);
    }

    public boolean isSupportedChannel(String channel) {
        return CHANNEL_WECHAT.equals(channel) || CHANNEL_ALIPAY.equals(channel);
    }

    public boolean isValidAmount(BigDecimal amount) {
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
    }

    private Map<String, Object> buildPayResult(RechargeOrder order, Long customerId) {
        Customer customer = customerMapper.getById(customerId);
        Map<String, Object> data = new HashMap<>();
        data.put("orderNo", order.getOrderNo());
        data.put("amount", order.getAmount());
        data.put("channel", order.getChannel());
        data.put("transactionNo", order.getTransactionNo());
        data.put("payTime", order.getPayTime());
        data.put("balance", customer != null && customer.getBalance() != null ? customer.getBalance() : BigDecimal.ZERO);
        return data;
    }

    private String generateOrderNo() {
        String time = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        int random = ThreadLocalRandom.current().nextInt(100000, 1000000);
        return "TD" + time + random;
    }

    private String generateTransactionNo(String channel) {
        String prefix = CHANNEL_WECHAT.equals(channel) ? "WX" : "ALI";
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return prefix + System.currentTimeMillis() + random;
    }
}
