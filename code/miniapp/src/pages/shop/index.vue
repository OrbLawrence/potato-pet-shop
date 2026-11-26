<template>
  <view class="page-wrap shop-page">
    <view class="shop-hero">
      <view class="hero-copy">
        <text class="hero-kicker">土豆宠物之家</text>
        <text class="hero-title">给毛孩子挑点好物</text>
        <text class="hero-sub">门店自提 · 到店核销</text>
      </view>
      <view class="hero-actions">
        <view class="balance-pill" @tap="goOrders">
          <text class="balance-label">余额</text>
          <text class="balance-value">¥{{ formatMoney(balance) }}</text>
        </view>
        <view class="orders-link" @tap="goOrders">我的订单</view>
      </view>
    </view>

    <view v-if="loading" class="state-card card">
      <text class="state-text">商品加载中...</text>
    </view>
    <view v-else-if="!products.length" class="state-card card">
      <text class="empty-icon">🛍️</text>
      <text class="state-text">暂时没有上架商品</text>
    </view>
    <view v-else class="product-list">
      <view v-for="product in products" :key="product.id" class="product-card card">
        <image
          class="product-image"
          :src="product.imageUrl || placeholderImage"
          mode="aspectFill"
          @error="handleImageError(product)"
        ></image>
        <view class="product-main">
          <view class="product-heading">
            <text class="product-name">{{ product.name }}</text>
            <text class="stock-tag" :class="{ sold: product.stock <= 0 }">{{ product.stock > 0 ? `库存 ${product.stock}` : '已售罄' }}</text>
          </view>
          <text class="product-desc">{{ product.description || '土豆宠物之家精选商品' }}</text>
          <view class="product-footer">
            <view class="price-block">
              <text class="price-symbol">¥</text>
              <text class="product-price">{{ formatMoney(product.price) }}</text>
            </view>
            <view v-if="product.stock > 0" class="buy-controls">
              <view v-if="cart[product.id]" class="quantity-control">
                <button class="quantity-btn" @tap.stop="changeQuantity(product, -1)">−</button>
                <text class="quantity-value">{{ cart[product.id].quantity }}</text>
                <button class="quantity-btn" @tap.stop="changeQuantity(product, 1)">+</button>
              </view>
              <button class="add-btn" @tap.stop="addToCart(product)">
                {{ cart[product.id] ? '已加入' : '加入购物车' }}
              </button>
            </view>
          </view>
        </view>
      </view>
    </view>

    <view class="cart-bar">
      <view class="cart-summary" @tap="openCheckout">
        <view class="cart-icon-wrap">
          <text class="cart-icon">🛒</text>
          <text v-if="cartCount" class="cart-badge">{{ cartCount }}</text>
        </view>
        <view class="cart-total">
          <text class="cart-total-label">合计</text>
          <text class="cart-total-value">¥{{ formatMoney(cartAmount) }}</text>
        </view>
      </view>
      <button class="checkout-btn" :class="{ disabled: !cartCount }" @tap="openCheckout">去结算</button>
    </view>

    <view v-if="checkoutVisible" class="overlay" @tap="closeCheckout">
      <view class="checkout-sheet" @tap.stop>
        <view class="sheet-header">
          <view>
            <text class="sheet-title">确认订单</text>
            <text class="sheet-subtitle">{{ cartCount }} 件商品</text>
          </view>
          <button class="close-btn" @tap="closeCheckout">×</button>
        </view>

        <scroll-view scroll-y class="checkout-scroll">
          <view class="section">
            <view v-for="item in cartItems" :key="item.product.id" class="checkout-item">
              <image class="checkout-image" :src="item.product.imageUrl || placeholderImage" mode="aspectFill" @error="handleImageError(item.product)"></image>
              <view class="checkout-item-main">
                <text class="checkout-item-name">{{ item.product.name }}</text>
                <text class="checkout-item-price">¥{{ formatMoney(item.product.price) }} × {{ item.quantity }}</text>
              </view>
              <view class="quantity-control compact">
                <button class="quantity-btn" @tap="changeQuantity(item.product, -1)">−</button>
                <text class="quantity-value">{{ item.quantity }}</text>
                <button class="quantity-btn" @tap="changeQuantity(item.product, 1)">+</button>
              </view>
            </view>
          </view>

          <view class="section">
            <text class="section-title">选择门店</text>
            <view v-if="storesLoading" class="section-hint">门店加载中...</view>
            <view v-else-if="!stores.length" class="section-hint">暂无可下单门店</view>
            <view v-else class="store-list">
              <view
                v-for="store in stores"
                :key="store.id"
                class="store-option"
                :class="{ active: selectedStoreId === store.id }"
                @tap="selectedStoreId = store.id"
              >
                <view class="store-radio"><view v-if="selectedStoreId === store.id" class="store-radio-dot"></view></view>
                <view class="store-main">
                  <text class="store-name">{{ store.name }}</text>
                  <text class="store-address">{{ store.address || '门店地址待完善' }}</text>
                </view>
              </view>
            </view>
          </view>

          <view class="section">
            <text class="section-title">支付方式</text>
            <view class="pay-list">
              <view
                v-for="channel in payChannels"
                :key="channel.value"
                class="pay-option"
                :class="{ active: paymentChannel === channel.value }"
                @tap="paymentChannel = channel.value"
              >
                <view class="pay-icon" :class="channel.value">{{ channel.short }}</view>
                <view class="pay-main">
                  <text class="pay-name">{{ channel.label }}</text>
                  <text class="pay-hint">{{ channel.value === 'balance' ? `可用余额 ¥${formatMoney(balance)}` : '模拟支付' }}</text>
                </view>
                <view class="store-radio"><view v-if="paymentChannel === channel.value" class="store-radio-dot"></view></view>
              </view>
            </view>
          </view>
        </scroll-view>

        <view class="sheet-footer">
          <view class="sheet-total">
            <text>应付</text>
            <text class="sheet-total-value">¥{{ formatMoney(cartAmount) }}</text>
          </view>
          <button class="pay-btn" :loading="paying" :disabled="paying || !cartCount" @tap="submitOrder">
            {{ paying ? '支付处理中' : '确认支付' }}
          </button>
        </view>
      </view>
    </view>

    <view v-if="paying" class="payment-overlay">
      <view class="payment-progress-card">
        <view class="payment-spinner"></view>
        <text class="payment-progress-title">{{ paymentProgressText }}</text>
        <text class="payment-progress-hint">正在等待支付结果，请稍候</text>
      </view>
    </view>

    <view v-if="successOrder" class="overlay success-overlay">
      <view class="success-card">
        <view class="success-icon">✓</view>
        <text class="success-title">支付成功</text>
        <text class="success-order-no">订单号 {{ successOrder.orderNo }}</text>
        <view class="verify-box">
          <text class="verify-label">核销码</text>
          <text class="verify-code">{{ successOrder.verificationCode }}</text>
        </view>
        <view class="success-actions">
          <button class="secondary-btn" @tap="viewOrders">查看订单</button>
          <button class="primary-btn" @tap="continueShopping">继续逛逛</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { request } from '../../utils/request'

const placeholderImage = '/static/images/product-placeholder.jpg'
const products = ref([])
const loading = ref(false)
const cart = ref({})
const stores = ref([])
const storesLoading = ref(false)
const checkoutVisible = ref(false)
const selectedStoreId = ref(null)
const paymentChannel = ref('balance')
const paying = ref(false)
const successOrder = ref(null)
const balance = ref(Number(uni.getStorageSync('balance') || 0))

const payChannels = [
  { value: 'balance', label: '余额支付', short: '余' },
  { value: 'wechat', label: '微信支付', short: '微' },
  { value: 'alipay', label: '支付宝支付', short: '支' }
]

const cartItems = computed(() => Object.values(cart.value))
const cartCount = computed(() => cartItems.value.reduce((sum, item) => sum + item.quantity, 0))
const cartAmount = computed(() => cartItems.value.reduce((sum, item) => sum + Number(item.product.price) * item.quantity, 0))
const paymentProgressText = computed(() => {
  const channel = payChannels.find(item => item.value === paymentChannel.value)
  return `${channel ? channel.label : '支付'}处理中`
})

function formatMoney(value) {
  return Number(value || 0).toFixed(2)
}

async function loadProducts() {
  loading.value = true
  try {
    products.value = await request({ url: '/user/shop/products' })
  } catch (error) {
    products.value = []
  } finally {
    loading.value = false
  }
}

async function loadProfile() {
  try {
    const profile = await request({ url: '/user/profile' })
    if (profile && profile.balance != null) {
      balance.value = Number(profile.balance)
      uni.setStorageSync('balance', profile.balance)
    }
  } catch (error) {
    // 统一请求层已提示
  }
}

function handleImageError(product) {
  product.imageUrl = placeholderImage
}

function addToCart(product) {
  if (product.stock <= 0) {
    uni.showToast({ title: '商品已售罄', icon: 'none' })
    return
  }
  const current = cart.value[product.id]
  if (current) {
    changeQuantity(product, 1)
    return
  }
  cart.value = { ...cart.value, [product.id]: { product, quantity: 1 } }
}

function changeQuantity(product, delta) {
  const current = cart.value[product.id]
  if (!current) return
  const nextQuantity = current.quantity + delta
  if (nextQuantity <= 0) {
    const nextCart = { ...cart.value }
    delete nextCart[product.id]
    cart.value = nextCart
    return
  }
  if (nextQuantity > product.stock) {
    uni.showToast({ title: `库存仅剩 ${product.stock} 件`, icon: 'none' })
    return
  }
  cart.value = {
    ...cart.value,
    [product.id]: { ...current, quantity: nextQuantity }
  }
}

async function loadStores() {
  if (stores.value.length || storesLoading.value) return
  storesLoading.value = true
  try {
    stores.value = await request({ url: '/user/shop/stores' })
    if (stores.value.length) selectedStoreId.value = stores.value[0].id
  } catch (error) {
    stores.value = []
  } finally {
    storesLoading.value = false
  }
}

function openCheckout() {
  if (!cartCount.value) {
    uni.showToast({ title: '请先选择商品', icon: 'none' })
    return
  }
  checkoutVisible.value = true
  loadStores()
}

function closeCheckout() {
  if (paying.value) return
  checkoutVisible.value = false
}

async function submitOrder() {
  if (!selectedStoreId.value) {
    uni.showToast({ title: '请选择下单门店', icon: 'none' })
    return
  }
  if (paymentChannel.value === 'balance' && Number(balance.value) < Number(cartAmount.value)) {
    uni.showToast({ title: '账户余额不足', icon: 'none' })
    return
  }

  paying.value = true
  const startedAt = Date.now()
  const minimumDuration = 1800 + Math.floor(Math.random() * 700)
  try {
    const result = await request({
      url: '/user/shop/orders',
      method: 'POST',
      data: {
        storeId: selectedStoreId.value,
        paymentChannel: paymentChannel.value,
        items: cartItems.value.map(item => ({ productId: item.product.id, quantity: item.quantity }))
      },
      timeout: 20000
    })
    await waitRemaining(startedAt, minimumDuration)
    successOrder.value = result.order
    balance.value = Number(result.balance || 0)
    uni.setStorageSync('balance', balance.value)
    cart.value = {}
    checkoutVisible.value = false
    await loadProducts()
  } catch (error) {
    await waitRemaining(startedAt, minimumDuration)
  } finally {
    paying.value = false
  }
}

function waitRemaining(startedAt, minimumDuration) {
  const remaining = minimumDuration - (Date.now() - startedAt)
  return remaining > 0 ? new Promise(resolve => setTimeout(resolve, remaining)) : Promise.resolve()
}

function goOrders() {
  uni.navigateTo({ url: '/pages/shop/orders' })
}

function viewOrders() {
  successOrder.value = null
  goOrders()
}

function continueShopping() {
  successOrder.value = null
}

onShow(() => {
  loadProducts()
  loadProfile()
})
</script>

<style lang="scss" scoped>
.shop-page {
  min-height: 100vh;
  padding-bottom: calc(270rpx + env(safe-area-inset-bottom));
}

.shop-hero {
  display: flex;
  justify-content: space-between;
  gap: 24rpx;
  padding: 40rpx 34rpx;
  border-radius: 30rpx;
  background: linear-gradient(135deg, #ffecd7 0%, #ffe0c2 58%, #e7f6ed 100%);
  box-shadow: 0 14rpx 34rpx rgba(194, 132, 63, 0.14);
}

.hero-copy,
.hero-actions,
.price-block,
.cart-total,
.sheet-total,
.success-card {
  display: flex;
  flex-direction: column;
}

.hero-kicker {
  font-size: 24rpx;
  color: #a87548;
  font-weight: 600;
}

.hero-title {
  margin-top: 10rpx;
  font-size: 42rpx;
  line-height: 1.25;
  color: #4a3728;
  font-weight: 800;
}

.hero-sub {
  margin-top: 12rpx;
  font-size: 24rpx;
  color: #9b8574;
}

.hero-actions {
  align-items: flex-end;
  justify-content: space-between;
  flex: none;
}

.balance-pill {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  padding: 14rpx 20rpx;
  border-radius: 20rpx;
  background: rgba(255, 255, 255, 0.67);
}

.balance-label {
  font-size: 20rpx;
  color: #9b8574;
}

.balance-value {
  margin-top: 4rpx;
  color: #e8873a;
  font-size: 28rpx;
  font-weight: 800;
}

.orders-link {
  margin-top: 18rpx;
  color: #8b684d;
  font-size: 24rpx;
  font-weight: 600;
}

.product-list {
  margin-top: 26rpx;
  display: flex;
  flex-direction: column;
  gap: 22rpx;
}

.product-card {
  display: flex;
  gap: 24rpx;
  padding: 22rpx;
}

.product-image {
  width: 196rpx;
  height: 196rpx;
  flex: none;
  border-radius: 22rpx;
  background: #fff1e0;
}

.product-main {
  min-width: 0;
  flex: 1;
  display: flex;
  flex-direction: column;
}

.product-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12rpx;
}

.product-name {
  flex: 1;
  color: #4a3728;
  font-size: 30rpx;
  font-weight: 800;
  line-height: 1.35;
}

.stock-tag {
  flex: none;
  padding: 6rpx 12rpx;
  border-radius: 999rpx;
  background: #eaf8ef;
  color: #2d9a61;
  font-size: 20rpx;
}

.stock-tag.sold {
  background: #f3f0ed;
  color: #a79b90;
}

.product-desc {
  margin-top: 12rpx;
  color: #9b8574;
  font-size: 24rpx;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.product-footer {
  margin-top: auto;
  padding-top: 18rpx;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 12rpx;
}

.price-block {
  flex-direction: row;
  align-items: baseline;
  color: #e8873a;
}

.price-symbol {
  font-size: 24rpx;
  font-weight: 700;
}

.product-price {
  margin-left: 2rpx;
  font-size: 38rpx;
  font-weight: 900;
}

.buy-controls,
.quantity-control {
  display: flex;
  align-items: center;
}

.buy-controls {
  gap: 12rpx;
}

.quantity-control {
  height: 54rpx;
  border: 2rpx solid #f0ddc7;
  border-radius: 999rpx;
  overflow: hidden;
  background: #fffaf4;
}

.quantity-btn {
  width: 48rpx;
  height: 50rpx;
  line-height: 50rpx;
  text-align: center;
  color: #b27337;
  font-size: 28rpx;
  font-weight: 800;
}

.quantity-value {
  width: 42rpx;
  text-align: center;
  color: #4a3728;
  font-size: 24rpx;
  font-weight: 700;
}

.add-btn {
  height: 58rpx;
  padding: 0 18rpx;
  line-height: 58rpx;
  border-radius: 999rpx;
  background: #e8873a;
  color: #ffffff;
  font-size: 22rpx;
  font-weight: 700;
}

.cart-bar {
  position: fixed;
  left: 22rpx;
  right: 22rpx;
  bottom: calc(126rpx + env(safe-area-inset-bottom));
  z-index: 900;
  min-height: 108rpx;
  padding: 14rpx 16rpx 14rpx 22rpx;
  display: flex;
  align-items: center;
  gap: 18rpx;
  border-radius: 28rpx;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 12rpx 34rpx rgba(114, 74, 36, 0.18);
}

.cart-summary {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 18rpx;
}

.cart-icon-wrap {
  position: relative;
  width: 68rpx;
  height: 68rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 22rpx;
  background: #fff1e0;
}

.cart-icon {
  font-size: 36rpx;
}

.cart-badge {
  position: absolute;
  top: -8rpx;
  right: -8rpx;
  min-width: 32rpx;
  height: 32rpx;
  padding: 0 8rpx;
  line-height: 32rpx;
  text-align: center;
  border-radius: 999rpx;
  background: #ff6b6b;
  color: #ffffff;
  font-size: 20rpx;
}

.cart-total-label {
  color: #9b8574;
  font-size: 20rpx;
}

.cart-total-value {
  color: #e8873a;
  font-size: 34rpx;
  font-weight: 900;
}

.checkout-btn {
  width: 196rpx;
  height: 78rpx;
  line-height: 78rpx;
  border-radius: 24rpx;
  background: linear-gradient(135deg, #f19a4d, #e0752b);
  color: #ffffff;
  text-align: center;
  font-size: 28rpx;
  font-weight: 800;
}

.checkout-btn.disabled {
  opacity: 0.5;
}

.state-card {
  margin-top: 26rpx;
  min-height: 260rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.empty-icon {
  font-size: 68rpx;
}

.state-text {
  color: #9b8574;
  font-size: 26rpx;
}

.overlay {
  position: fixed;
  inset: 0;
  z-index: 1200;
  display: flex;
  align-items: flex-end;
  padding: 72rpx 18rpx calc(128rpx + env(safe-area-inset-bottom));
  background: rgba(61, 42, 28, 0.48);
}

.checkout-sheet {
  width: 100%;
  height: calc(100vh - 220rpx - env(safe-area-inset-bottom));
  display: flex;
  flex-direction: column;
  border-radius: 32rpx;
  background: #fffaf4;
  overflow: hidden;
}

.sheet-header {
  flex: none;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 30rpx 30rpx 22rpx;
  border-bottom: 2rpx solid #f4e8da;
}

.sheet-header > view {
  display: flex;
  flex-direction: column;
}

.sheet-title {
  color: #4a3728;
  font-size: 34rpx;
  font-weight: 800;
}

.sheet-subtitle {
  margin-top: 6rpx;
  color: #9b8574;
  font-size: 22rpx;
}

.close-btn {
  width: 58rpx;
  height: 58rpx;
  line-height: 54rpx;
  border-radius: 50%;
  background: #f4e8da;
  color: #8d7866;
  font-size: 40rpx;
}

.checkout-scroll {
  height: 0;
  flex: 1;
  min-height: 0;
  padding: 0 30rpx;
}

.section {
  padding: 24rpx 0;
  border-bottom: 2rpx solid #f4e8da;
}

.checkout-item {
  display: flex;
  align-items: center;
  gap: 18rpx;
  margin-bottom: 18rpx;
}

.checkout-item:last-child {
  margin-bottom: 0;
}

.checkout-image {
  width: 92rpx;
  height: 92rpx;
  border-radius: 16rpx;
  flex: none;
}

.checkout-item-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.checkout-item-name {
  color: #4a3728;
  font-size: 26rpx;
  font-weight: 700;
}

.checkout-item-price {
  margin-top: 8rpx;
  color: #9b8574;
  font-size: 22rpx;
}

.quantity-control.compact {
  height: 48rpx;
}

.quantity-control.compact .quantity-btn {
  width: 42rpx;
  height: 44rpx;
  line-height: 44rpx;
}

.section-title {
  display: block;
  margin-bottom: 18rpx;
  color: #4a3728;
  font-size: 28rpx;
  font-weight: 800;
}

.section-hint {
  color: #9b8574;
  font-size: 24rpx;
}

.store-list,
.pay-list {
  display: flex;
  flex-direction: column;
  gap: 14rpx;
}

.store-option,
.pay-option {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 20rpx;
  border: 2rpx solid #f0e2d2;
  border-radius: 20rpx;
  background: #ffffff;
}

.store-option.active,
.pay-option.active {
  border-color: #e8873a;
  background: #fff5e9;
}

.store-radio {
  width: 32rpx;
  height: 32rpx;
  border: 2rpx solid #d7c4b0;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex: none;
}

.store-radio-dot {
  width: 18rpx;
  height: 18rpx;
  border-radius: 50%;
  background: #e8873a;
}

.store-main,
.pay-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.store-name,
.pay-name {
  color: #4a3728;
  font-size: 26rpx;
  font-weight: 700;
}

.store-address,
.pay-hint {
  margin-top: 6rpx;
  color: #9b8574;
  font-size: 21rpx;
}

.pay-icon {
  width: 54rpx;
  height: 54rpx;
  line-height: 54rpx;
  border-radius: 16rpx;
  text-align: center;
  color: #ffffff;
  font-size: 24rpx;
  font-weight: 800;
}

.pay-icon.balance { background: #e8873a; }
.pay-icon.wechat { background: #2fbf71; }
.pay-icon.alipay { background: #1677ff; }

.sheet-footer {
  flex: none;
  padding: 20rpx 30rpx;
  display: flex;
  align-items: center;
  gap: 22rpx;
  border-top: 2rpx solid #f4e8da;
  background: #ffffff;
}

.sheet-total {
  flex: 1;
  color: #9b8574;
  font-size: 22rpx;
}

.sheet-total-value {
  margin-top: 2rpx;
  color: #e8873a;
  font-size: 38rpx;
  font-weight: 900;
}

.pay-btn {
  width: 300rpx;
  height: 84rpx;
  line-height: 84rpx;
  border-radius: 24rpx;
  background: linear-gradient(135deg, #f19a4d, #df6e25);
  color: #ffffff;
  text-align: center;
  font-size: 28rpx;
  font-weight: 800;
}

.payment-overlay {
  position: fixed;
  inset: 0;
  z-index: 1600;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48rpx;
  background: rgba(61, 42, 28, 0.52);
}

.payment-progress-card {
  width: 480rpx;
  padding: 52rpx 40rpx 44rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  border-radius: 30rpx;
  background: #fffaf4;
  box-shadow: 0 24rpx 70rpx rgba(72, 43, 22, 0.22);
}

.payment-spinner {
  width: 76rpx;
  height: 76rpx;
  border-radius: 50%;
  border: 8rpx solid #f2dfca;
  border-top-color: #e8873a;
  animation: payment-spin 0.8s linear infinite;
}

.payment-progress-title {
  margin-top: 28rpx;
  color: #4a3728;
  font-size: 30rpx;
  font-weight: 800;
}

.payment-progress-hint {
  margin-top: 12rpx;
  color: #9b8574;
  font-size: 23rpx;
}

@keyframes payment-spin {
  to { transform: rotate(360deg); }
}

.success-overlay {
  align-items: center;
  justify-content: center;
  padding: 40rpx;
}

.success-card {
  width: 100%;
  align-items: center;
  padding: 46rpx 34rpx 34rpx;
  border-radius: 32rpx;
  background: #fffaf4;
}

.success-icon {
  width: 96rpx;
  height: 96rpx;
  line-height: 96rpx;
  border-radius: 50%;
  background: #e5f7ec;
  color: #2fbf71;
  text-align: center;
  font-size: 54rpx;
  font-weight: 900;
}

.success-title {
  margin-top: 20rpx;
  color: #4a3728;
  font-size: 38rpx;
  font-weight: 900;
}

.success-order-no {
  margin-top: 10rpx;
  color: #9b8574;
  font-size: 22rpx;
}

.verify-box {
  width: 100%;
  margin-top: 28rpx;
  padding: 26rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  border-radius: 22rpx;
  background: linear-gradient(135deg, #fff0dc, #ffe4c4);
}

.verify-label {
  color: #946139;
  font-size: 24rpx;
}

.verify-code {
  margin-top: 10rpx;
  color: #e0752b;
  font-size: 64rpx;
  font-weight: 900;
  letter-spacing: 18rpx;
  text-indent: 18rpx;
}

.success-actions {
  width: 100%;
  margin-top: 30rpx;
  display: flex;
  gap: 18rpx;
}

.secondary-btn,
.primary-btn {
  flex: 1;
  height: 82rpx;
  line-height: 82rpx;
  border-radius: 22rpx;
  text-align: center;
  font-size: 27rpx;
  font-weight: 700;
}

.secondary-btn {
  border: 2rpx solid #efdac3;
  color: #8b684d;
  background: #ffffff;
}

.primary-btn {
  background: #e8873a;
  color: #ffffff;
}
</style>
