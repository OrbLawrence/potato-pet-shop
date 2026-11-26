<template>
  <view class="page-wrap profile-page">
    <view class="profile-header card">
      <image class="avatar" :src="avatar || '/static/images/landing-hero.jpg'" mode="aspectFill" @tap="go('/pages/profile/info')"></image>
      <view class="profile-info">
        <text class="username">{{ username || '未登录' }}</text>
        <text class="balance">余额 ¥ {{ balance }}</text>
      </view>
    </view>

    <view class="menu-card card">
      <view class="menu-item" @tap="go('/pages/profile/info')">
        <text class="menu-emoji">👤</text>
        <text class="menu-text">个人信息</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @tap="go('/pages/profile/change-password')">
        <text class="menu-emoji">🔒</text>
        <text class="menu-text">修改密码</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @tap="go('/pages/recharge/index')">
        <text class="menu-emoji">💰</text>
        <text class="menu-text">账户充值</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @tap="go('/pages/shop/orders')">
        <text class="menu-emoji">📦</text>
        <text class="menu-text">商品订单</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @tap="go('/pages/recharge/records')">
        <text class="menu-emoji">🧾</text>
        <text class="menu-text">充值记录</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @tap="go('/pages/about/index')">
        <text class="menu-emoji">🏠</text>
        <text class="menu-text">关于我们</text>
        <text class="menu-arrow">›</text>
      </view>
    </view>

    <button class="logout-btn" @tap="logout">退出登录</button>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { request, clearAuth } from '../../utils/request'

const username = ref(uni.getStorageSync('username') || '')
const balance = ref(uni.getStorageSync('balance') || 0)
const avatar = ref('')

function go(url) {
  uni.navigateTo({ url })
}

async function loadProfile() {
  try {
    const data = await request({ url: '/user/profile' })
    if (data) {
      username.value = data.name || data.username || username.value
      avatar.value = data.avatar || ''
      if (data.balance != null) {
        balance.value = data.balance
        uni.setStorageSync('balance', data.balance)
      }
    }
  } catch (e) {
    // 已统一提示
  }
}

function logout() {
  uni.showModal({
    title: '提示',
    content: '确定退出登录吗？',
    success: (res) => {
      if (res.confirm) {
        clearAuth()
        uni.reLaunch({ url: '/pages/login/index' })
      }
    }
  })
}

// 每次进入该 Tab 时刷新用户信息
onShow(() => {
  loadProfile()
})


</script>


<style lang="scss" scoped>
.profile-page {
  padding-bottom: calc(150rpx + env(safe-area-inset-bottom));
}

.profile-header {
  display: flex;
  align-items: center;
  padding: 36rpx 32rpx;
  background: linear-gradient(135deg, #fff1e0 0%, #ffe7cf 100%);
}

.avatar {
  width: 128rpx;
  height: 128rpx;
  border-radius: 50%;
  border: 6rpx solid #ffffff;
  box-shadow: 0 10rpx 26rpx rgba(194, 132, 63, 0.22);
}

.profile-info {
  margin-left: 28rpx;
  display: flex;
  flex-direction: column;
}

.username {
  font-size: 40rpx;
  font-weight: 800;
  color: #4a3728;
}

.balance {
  margin-top: 12rpx;
  font-size: 28rpx;
  color: #e8873a;
  font-weight: 700;
}

.menu-card {
  margin-top: 24rpx;
  padding: 0 28rpx;
}

.menu-item {
  display: flex;
  align-items: center;
  padding: 32rpx 0;
  border-bottom: 2rpx solid #f8eee1;
}

.menu-item:last-child {
  border-bottom: none;
}

.menu-emoji {
  margin-right: 22rpx;
  font-size: 38rpx;
}

.menu-text {
  flex: 1;
  font-size: 30rpx;
  color: #4a3728;
  font-weight: 600;
}

.menu-arrow {
  font-size: 40rpx;
  color: #d9c8b5;
}

.logout-btn {
  margin-top: 40rpx;
  height: 92rpx;
  line-height: 92rpx;
  text-align: center;
  border-radius: 46rpx;
  background: #ffffff;
  color: #d9534f;
  font-size: 30rpx;
  font-weight: 700;
  border: 2rpx solid #f2c7c4;
}
</style>

