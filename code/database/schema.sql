-- 展示用数据库结构：仅保留 DDL，已移除所有演示数据与账号信息。
CREATE DATABASE IF NOT EXISTS pet_shop DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE pet_shop;

DROP TABLE IF EXISTS `pet`;
DROP TABLE IF EXISTS `product_order_item`;
DROP TABLE IF EXISTS `product_order`;
DROP TABLE IF EXISTS `product`;
DROP TABLE IF EXISTS `customer`;
DROP TABLE IF EXISTS `recharge_order`;
DROP TABLE IF EXISTS `pet_category`;
DROP TABLE IF EXISTS `employee`;
DROP TABLE IF EXISTS `store`;

CREATE TABLE `store` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '门店ID',
  `name` VARCHAR(100) NOT NULL COMMENT '门店名称',
  `address` VARCHAR(255) DEFAULT NULL COMMENT '门店地址',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
  `avatar` VARCHAR(255) DEFAULT NULL COMMENT '门店图片',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='门店表';

CREATE TABLE `employee` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '员工ID',
  `name` VARCHAR(50) NOT NULL COMMENT '员工姓名',
  `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
  `gender` VARCHAR(10) DEFAULT NULL COMMENT '性别',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
  `position` VARCHAR(50) DEFAULT NULL COMMENT '职位',
  `password` VARCHAR(100) DEFAULT NULL COMMENT '密码',
  `store_id` BIGINT DEFAULT NULL COMMENT '所属门店ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工表';

CREATE TABLE `pet_category` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '种类ID',
  `name` VARCHAR(50) NOT NULL COMMENT '种类名称',
  `description` VARCHAR(255) DEFAULT NULL COMMENT '描述',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='宠物种类表';

CREATE TABLE `customer` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '客户ID',
  `name` VARCHAR(50) NOT NULL COMMENT '客户姓名',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
  `address` VARCHAR(255) DEFAULT NULL COMMENT '地址',
  `avatar` VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
  `password` VARCHAR(100) DEFAULT NULL COMMENT '密码',
  `balance` DECIMAL(10,2) DEFAULT 0.00 COMMENT '余额',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户表';

CREATE TABLE `product` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '商品ID',
  `name` VARCHAR(100) NOT NULL COMMENT '商品名称',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '商品描述',
  `price` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '商品单价',
  `stock` INT NOT NULL DEFAULT 0 COMMENT '库存',
  `image_url` VARCHAR(500) DEFAULT NULL COMMENT 'OSS商品图片',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1上架, 0下架',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序值',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_product_status_sort` (`status`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

CREATE TABLE `product_order` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '订单ID',
  `order_no` VARCHAR(64) NOT NULL COMMENT '订单号',
  `customer_id` BIGINT NOT NULL COMMENT '客户ID',
  `store_id` BIGINT NOT NULL COMMENT '下单门店ID',
  `amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '订单金额',
  `payment_channel` VARCHAR(20) NOT NULL COMMENT '支付渠道: balance/wechat/alipay',
  `status` VARCHAR(20) NOT NULL DEFAULT 'paid' COMMENT '状态: paid/completed',
  `verification_code` CHAR(6) NOT NULL COMMENT '6位核销码',
  `transaction_no` VARCHAR(64) DEFAULT NULL COMMENT '交易流水号',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `pay_time` DATETIME DEFAULT NULL COMMENT '支付时间',
  `complete_time` DATETIME DEFAULT NULL COMMENT '完成时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_product_order_no` (`order_no`),
  UNIQUE KEY `uk_verification_code` (`verification_code`),
  KEY `idx_product_order_customer` (`customer_id`),
  KEY `idx_product_order_store` (`store_id`),
  KEY `idx_product_order_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品订单表';

CREATE TABLE `product_order_item` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '明细ID',
  `order_id` BIGINT NOT NULL COMMENT '订单ID',
  `product_id` BIGINT NOT NULL COMMENT '商品ID',
  `product_name` VARCHAR(100) NOT NULL COMMENT '商品名称快照',
  `product_image` VARCHAR(500) DEFAULT NULL COMMENT '商品图片快照',
  `price` DECIMAL(10,2) NOT NULL COMMENT '成交单价',
  `quantity` INT NOT NULL COMMENT '购买数量',
  `subtotal` DECIMAL(10,2) NOT NULL COMMENT '小计',
  PRIMARY KEY (`id`),
  KEY `idx_product_order_item_order` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品订单明细表';

CREATE TABLE `recharge_order` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '订单ID',
  `order_no` VARCHAR(64) NOT NULL COMMENT '订单号',
  `customer_id` BIGINT NOT NULL COMMENT '客户ID',
  `amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '充值金额',
  `channel` VARCHAR(20) NOT NULL COMMENT '支付渠道: wechat/alipay',
  `status` VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '状态: pending/success',
  `transaction_no` VARCHAR(64) DEFAULT NULL COMMENT '交易流水号',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `pay_time` DATETIME DEFAULT NULL COMMENT '支付时间',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_customer_id` (`customer_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='充值订单表';

CREATE TABLE `pet` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '宠物ID',
  `name` VARCHAR(50) NOT NULL COMMENT '宠物名称',
  `category_id` BIGINT NOT NULL COMMENT '种类ID',
  `customer_id` BIGINT DEFAULT NULL COMMENT '客户ID',
  `age` INT DEFAULT NULL COMMENT '年龄',
  `gender` VARCHAR(10) DEFAULT NULL COMMENT '性别',
  `weight` DECIMAL(5,2) DEFAULT NULL COMMENT '体重',
  `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '描述',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='宠物表';
