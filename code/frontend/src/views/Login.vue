<template>
  <div class="login-container">
    <div class="login-bg">
      <!-- 背景图片轮播 -->
      <div class="bg-images">
        <div
          v-for="(img, i) in backgroundImages"
          :key="i"
          class="bg-image"
          :class="{ active: activeIndex === i }"
          :style="{ backgroundImage: `url(${img})` }"
        ></div>
      </div>
      <div class="bg-overlay"></div>
      <!-- 场景层 -->
      <div class="bg-scene">
        <div class="hill hill-1"></div>
        <div class="hill hill-2"></div>
        <div class="hill hill-3"></div>
      </div>
      <!-- 粒子 -->
      <div class="particles">
        <span class="particle p1"></span>
        <span class="particle p2"></span>
        <span class="particle p3"></span>
        <span class="particle p4"></span>
        <span class="particle p5"></span>
        <span class="particle p6"></span>
        <span class="particle p7"></span>
        <span class="particle p8"></span>
      </div>
    </div>
    <div class="login-wrapper">
      <div class="login-card">
        <div class="login-header">
          <div class="logo-icon">🐾</div>
          <h2>土豆宠物之家运营系统</h2>
          <p>欢迎回来，请登录您的账户</p>
        </div>
        <div class="role-tabs">
          <span class="role-tab" :class="{ active: role === 'admin' }" @click="role = 'admin'">👨‍💼 管理员</span>
          <span class="role-tab" :class="{ active: role === 'user' }" @click="role = 'user'">👤 用户</span>
        </div>
        <el-form :model="form" :rules="rules" ref="formRef" class="login-form">
          <el-form-item prop="username">
            <el-input
              v-model="form.username"
              :placeholder="role === 'admin' ? '管理员用户名' : '请输入用户名'"
              size="large"
            >
              <template #prefix>👤</template>
            </el-input>
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              type="password"
              :placeholder="role === 'admin' ? '管理员密码' : '请输入密码'"
              size="large"
              show-password
            >
              <template #prefix>🔒</template>
            </el-input>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" size="large" @click="handleLogin" class="login-btn" :loading="loading">
              {{ loading ? '登录中...' : '登录' }}
            </el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'
import loginBg1 from '../assets/backgrounds/login-bg-1.jpg'
import loginBg2 from '../assets/backgrounds/login-bg-3.jpg'
import loginBg3 from '../assets/backgrounds/login-bg-4.jpg'

const role = ref('admin')
const loading = ref(false)
const formRef = ref(null)
const activeIndex = ref(0)
const backgroundImages = [loginBg1, loginBg2, loginBg3]
let carouselTimer = null

const form = ref({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = async () => {
  await formRef.value.validate()

  loading.value = true
  try {
    const response = await axios.post('/api/login', { ...form.value, role: role.value })
    if (response.data.code === 200) {
      const { token, username, role: userRole, balance } = response.data.data
      localStorage.setItem('token', token)
      localStorage.setItem('username', username)
      localStorage.setItem('role', userRole)
      if (balance != null) localStorage.setItem('balance', balance)
      ElMessage.success('登录成功')
      window.location.href = userRole === 'user' ? '/user/home' : '/admin/dashboard'
    } else {
      ElMessage.error(response.data.message || '登录失败')
    }
  } catch (error) {
    console.error('登录错误:', error)
    ElMessage.error('登录失败，请检查网络连接或后端服务')
  }
  loading.value = false
}

onMounted(() => {
  carouselTimer = setInterval(() => {
    activeIndex.value = (activeIndex.value + 1) % backgroundImages.length
  }, 2500)
})

onBeforeUnmount(() => {
  if (carouselTimer) clearInterval(carouselTimer)
})
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  position: relative;
  overflow: hidden;
}

.login-bg {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: #1a1228;
  overflow: hidden;
}

.bg-images {
  position: absolute;
  inset: 0;
  z-index: 0;
}

.bg-image {
  position: absolute;
  inset: 0;
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  opacity: 0;
  transition: opacity 2.5s ease-in-out;
}

.bg-image.active {
  opacity: 1;
}

.bg-overlay {
  position: absolute;
  inset: 0;
  z-index: 1;
  background: rgba(0,0,0,0.08);
}


/* ===== 场景层: 小山剪影 ===== */
.bg-scene {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 45%;
  overflow: hidden;
  pointer-events: none;
}

.hill {
  position: absolute;
  bottom: 0;
  border-radius: 50%;
  opacity: 0.15;
}

.hill-1 {
  left: -20%;
  width: 80%;
  height: 70%;
  background: linear-gradient(to top, rgba(255,255,255,0.2), transparent);
}

.hill-2 {
  left: 25%;
  width: 65%;
  height: 55%;
  background: linear-gradient(to top, rgba(255,255,255,0.12), transparent);
}

.hill-3 {
  right: -15%;
  width: 70%;
  height: 65%;
  background: linear-gradient(to top, rgba(255,255,255,0.18), transparent);
}

/* ===== 粒子 ===== */
.particles {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
}

.particle {
  position: absolute;
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: rgba(255,255,255,0.5);
  animation: particleRise 6s ease-in infinite;
}

.p1 { left: 10%; top: 90%; animation-delay: 0s; width: 3px; height: 3px; }
.p2 { left: 25%; top: 95%; animation-delay: 1.5s; }
.p3 { left: 45%; top: 88%; animation-delay: 2.8s; width: 5px; height: 5px; }
.p4 { left: 60%; top: 92%; animation-delay: 0.7s; width: 3px; height: 3px; }
.p5 { left: 75%; top: 85%; animation-delay: 3.2s; }
.p6 { left: 88%; top: 93%; animation-delay: 1.9s; width: 3px; height: 3px; }
.p7 { left: 35%; top: 80%; animation-delay: 2.1s; }
.p8 { left: 52%; top: 90%; animation-delay: 4.0s; width: 5px; height: 5px; }

@keyframes particleRise {
  0% { transform: translateY(0) scale(1); opacity: 0.6; }
  50% { opacity: 0.3; }
  100% { transform: translateY(-80vh) scale(0.3); opacity: 0; }
}

.login-wrapper {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  padding: 20px;
}

.login-card {
  background: rgba(255, 255, 255, 0.15); /* 调低白色透明度，变透 */
  border-radius: 24px;
  padding: 40px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.25);
  width: 100%;
  max-width: 420px;
  backdrop-filter: blur(18px); /* 磨砂模糊，数值越大越糊 */
  -webkit-backdrop-filter: blur(18px); /* 兼容webkit浏览器 */
  border: 1px solid rgba(255,255,255,0.25); /* 加一层细白边框，磨砂质感更强 */
}


.login-header {
  text-align: center;
  margin-bottom: 24px;
}

.logo-icon {
  font-size: 56px;
  margin-bottom: 12px;
  animation: pawBounce 1.6s ease infinite;
}

@keyframes pawBounce {
  0%, 100% { transform: scale(1); }
  30% { transform: scale(1.2); }
  50% { transform: scale(0.9); }
}

.login-header h2 {
  color: #fff;
}
.login-header p {
  color: rgba(255,255,255,0.75);
}


.role-tabs {
  display: flex;
  border-radius: 14px;
  background: #f5f5f5;
  padding: 4px;
  margin-bottom: 24px;
}

.role-tab {
  flex: 1;
  text-align: center;
  padding: 10px;
  border-radius: 11px;
  cursor: pointer;
  font-size: 14px;
  color: #999;
  transition: all 0.3s;
}

.role-tab.active {
  background: white;
  color: #ff6b6b;
  font-weight: 600;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
}

.login-form { margin-top: 8px; }

.login-btn {
  width: 100%;
  height: 48px;
  border-radius: 24px;
  font-size: 16px;
  font-weight: 600;
  background: linear-gradient(135deg, #ff9a56 0%, #ff6b6b 100%);
  border: none;
  box-shadow: 0 6px 20px rgba(255, 107, 107, 0.35);
}

.login-btn:hover {
  background: linear-gradient(135deg, #ff8a46, #ff5a5a);
}

.login-tips {
  margin-top: 16px;
  padding: 12px 16px;
  background: #fffbf0;
  border-radius: 12px;
  text-align: center;
}

.login-tips p {
  margin: 4px 0;
  font-size: 12px;
  color: #8b6914;
}
</style>
