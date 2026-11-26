package com.petshop.product.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.petshop.product.entity.Product;
import com.petshop.product.mapper.ProductMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class ProductService {

    private static final String PRODUCT_LIST_CACHE_KEY = "pet:shop:products:on-sale:v1";

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${pet.shop.product-cache-ttl-seconds:600}")
    private long productCacheTtlSeconds;

    public PageInfo<Product> page(int pageNum, int pageSize, String keyword, Integer status) {
        PageHelper.startPage(pageNum, pageSize);
        return new PageInfo<>(productMapper.list(keyword, status));
    }

    @SuppressWarnings("unchecked")
    public List<Product> listOnSale() {
        try {
            Object cached = redisTemplate.opsForValue().get(PRODUCT_LIST_CACHE_KEY);
            if (cached instanceof List) {
                log.info("[PRODUCT] Redis 缓存命中: key={}, count={}", PRODUCT_LIST_CACHE_KEY, ((List<?>) cached).size());
                return (List<Product>) cached;
            }
        } catch (Exception e) {
            log.warn("[PRODUCT] 读取 Redis 商品缓存失败，将直接查询数据库: {}", e.getMessage());
        }

        log.info("[PRODUCT] Redis 缓存未命中，查询数据库商品列表: key={}", PRODUCT_LIST_CACHE_KEY);
        List<Product> products = productMapper.listOnSale();
        log.info("[PRODUCT] 数据库商品列表查询完成: count={}", products.size());
        try {
            redisTemplate.opsForValue().set(
                    PRODUCT_LIST_CACHE_KEY,
                    products,
                    productCacheTtlSeconds,
                    TimeUnit.SECONDS
            );
            log.info("[PRODUCT] 商品列表已写入 Redis 缓存: key={}, ttl={}s, count={}",
                    PRODUCT_LIST_CACHE_KEY, productCacheTtlSeconds, products.size());
        } catch (Exception e) {
            log.warn("[PRODUCT] 写入 Redis 商品缓存失败，不影响商品查询: {}", e.getMessage());
        }
        return products;
    }

    public Product getById(Long id) {
        return productMapper.getById(id);
    }

    public boolean add(Product product) {
        normalize(product);
        if (!isValid(product)) {
            return false;
        }
        boolean success = productMapper.insert(product) > 0;
        if (success) {
            log.info("[PRODUCT] 新增商品成功: id={}, name={}, price={}, stock={}",
                    product.getId(), product.getName(), product.getPrice(), product.getStock());
            evictListCache();
        }
        return success;
    }

    public boolean update(Product product) {
        normalize(product);
        if (product.getId() == null || !isValid(product)) {
            return false;
        }
        boolean success = productMapper.update(product) > 0;
        if (success) {
            log.info("[PRODUCT] 修改商品成功: id={}, name={}, price={}, stock={}, status={}",
                    product.getId(), product.getName(), product.getPrice(), product.getStock(), product.getStatus());
            evictListCache();
        }
        return success;
    }

    public boolean delete(Long id) {
        boolean success = productMapper.deleteById(id) > 0;
        if (success) {
            log.info("[PRODUCT] 删除商品成功: id={}", id);
            evictListCache();
        }
        return success;
    }

    public void evictListCache() {
        try {
            redisTemplate.delete(PRODUCT_LIST_CACHE_KEY);
            log.info("[PRODUCT] 已清理 Redis 商品列表缓存: key={}", PRODUCT_LIST_CACHE_KEY);
        } catch (Exception e) {
            log.warn("[PRODUCT] 清理 Redis 商品缓存失败，缓存将在到期后自动失效: {}", e.getMessage());
        }
    }

    private void normalize(Product product) {
        if (product.getStock() == null) {
            product.setStock(0);
        }
        if (product.getStatus() == null) {
            product.setStatus(1);
        }
        if (product.getSortOrder() == null) {
            product.setSortOrder(0);
        }
    }

    private boolean isValid(Product product) {
        return product.getName() != null && !product.getName().trim().isEmpty()
                && product.getPrice() != null && product.getPrice().compareTo(BigDecimal.ZERO) >= 0
                && product.getStock() != null && product.getStock() >= 0
                && (product.getStatus() == 0 || product.getStatus() == 1);
    }
}
