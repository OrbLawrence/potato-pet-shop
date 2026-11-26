<template>
  <view class="page-wrap home-page">
    <!-- 顶部图片轮播 -->
    <swiper
      class="banner-swiper"
      circular
      autoplay
      :interval="3500"
      :duration="500"
      indicator-dots
      indicator-active-color="#ffffff"
      indicator-color="rgba(255,255,255,0.55)"
    >
      <swiper-item v-for="(img, index) in banners" :key="index">
        <image class="banner-img" :src="img" mode="aspectFill"></image>
      </swiper-item>
    </swiper>

    <!-- 快捷入口 -->
    <view class="quick-grid card">
      <view class="quick-item" @tap="go('/pages/pet/list')">
        <view class="quick-icon icon-pet">🐕</view>
        <text class="quick-text">萌宠浏览</text>
      </view>
      <view class="quick-item" @tap="go('/pages/store/list')">
        <view class="quick-icon icon-store">🏪</view>
        <text class="quick-text">门店浏览</text>
      </view>
      <view class="quick-item" @tap="go('/pages/recharge/index')">
        <view class="quick-icon icon-recharge">💰</view>
        <text class="quick-text">账户充值</text>
      </view>
      <view class="quick-item" @tap="go('/pages/recharge/records')">
        <view class="quick-icon icon-record">🧾</view>
        <text class="quick-text">充值记录</text>
      </view>
    </view>

    <!-- 平台统计 -->
    <view class="stats-row">
      <view class="stat-card card">
        <text class="stat-value">{{ stats.store }}</text>
        <text class="stat-label">门店数</text>
      </view>
      <view class="stat-card card">
        <text class="stat-value">{{ stats.pet }}</text>
        <text class="stat-label">宠物数</text>
      </view>
      <view class="stat-card card">
        <text class="stat-value">{{ stats.category }}</text>
        <text class="stat-label">种类数</text>
      </view>
    </view>

    <!-- 主视觉欢迎卡 -->
    <view class="hero-card card">
      <image class="hero-img" src="https://cdn.example.com/images/landing-hero.jpg" mode="aspectFill"></image>
      <view class="hero-mask"></view>
      <view class="hero-text">
        <text class="hero-title">欢迎来到土豆宠物之家</text>
        <text class="hero-sub">在这里，遇见与你投缘的毛孩子 🐾</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { request } from '../../utils/request'

const banners = [
  'https://cdn.example.com/images/banner-1.png',
  'https://cdn.example.com/images/banner-2.png',
  'https://cdn.example.com/images/banner-3.png',
  'https://cdn.example.com/images/banner-4.png',
  'https://cdn.example.com/images/banner-5.png',
  'https://cdn.example.com/images/banner-6.png'
]

const stats = ref({ store: '-', pet: '-', category: '-' })

function go(url) {
  uni.navigateTo({ url })
}

async function loadStats() {
  try {
    const [stores, pets, categories] = await Promise.all([
      request({ url: '/store/list' }),
      request({ url: '/pet/page', data: { pageNum: 1, pageSize: 1 } }),
      request({ url: '/category/list' })
    ])
    stats.value.store = Array.isArray(stores) ? stores.length : '-'
    stats.value.pet = pets && pets.total != null ? pets.total : '-'
    stats.value.category = Array.isArray(categories) ? categories.length : '-'
  } catch (e) {
    // 请求失败时保留占位符
  }
}

loadStats()
</script>

<style lang="scss" scoped>
.home-page {
  padding-bottom: calc(150rpx + env(safe-area-inset-bottom));
}

.banner-swiper {
  height: 340rpx;
  border-radius: 28rpx;
  overflow: hidden;
  box-shadow: 0 12rpx 34rpx rgba(194, 132, 63, 0.18);
}

.banner-img {
  width: 100%;
  height: 340rpx;
}

.quick-grid {
  display: flex;
  justify-content: space-between;
  margin-top: 28rpx;
  padding: 36rpx 20rpx 32rpx;
}

.quick-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 25%;
}

.quick-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 104rpx;
  height: 104rpx;
  border-radius: 32rpx;
  font-size: 52rpx;
}

.icon-pet { background: #fff1e0; }
.icon-store { background: #e8f6ef; }
.icon-recharge { background: #fdeeee; }
.icon-record { background: #eef3ff; }

.quick-text {
  margin-top: 16rpx;
  font-size: 26rpx;
  color: #4a3728;
  font-weight: 600;
}

.stats-row {
  display: flex;
  gap: 20rpx;
  margin-top: 28rpx;
}

.stat-card {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 32rpx 0;
}

.stat-value {
  font-size: 44rpx;
  font-weight: 800;
  color: #e8873a;
}

.stat-label {
  margin-top: 10rpx;
  font-size: 26rpx;
  color: #9b8574;
}

.hero-card {
  position: relative;
  margin-top: 28rpx;
  height: 280rpx;
  overflow: hidden;
}

.hero-img {
  width: 100%;
  height: 100%;
}

.hero-mask {
  position: absolute;
  top: 0; left: 0; right: 0; bottom: 0;
  background: linear-gradient(180deg, rgba(74, 40, 20, 0.05) 0%, rgba(74, 40, 20, 0.62) 100%);
}

.hero-text {
  position: absolute;
  left: 32rpx;
  right: 32rpx;
  bottom: 28rpx;
  display: flex;
  flex-direction: column;
}

.hero-title {
  font-size: 34rpx;
  font-weight: 700;
  color: #ffffff;
}

.hero-sub {
  margin-top: 8rpx;
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.9);
}
</style>

