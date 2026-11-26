<template>
  <div class="user-layout">
    <header class="user-header">
      <div class="header-inner">
        <div class="logo-area" @click="$router.push('/user/home')">
          <span class="paw-icon">🐾</span>
          <span class="logo-text">土豆宠物</span>
        </div>
        <nav class="nav-links">
          <router-link to="/user/home" class="nav-item">🏠 首页</router-link>
          <router-link to="/user/ai" class="nav-item">🤖 智能客服</router-link>
          <router-link to="/user/pets" class="nav-item">🐕 萌宠</router-link>
          <router-link to="/user/stores" class="nav-item">🏪 门店</router-link>
          <router-link to="/user/recharge" class="nav-item">💰 充值</router-link>
        </nav>
        <div class="user-info">
          <span class="balance">余额: ¥{{ balance }}</span>
          <span class="username">👤 {{ username }}</span><el-button text @click="showPwdDialog = true" class="pwd-btn">修改密码</el-button>
          <el-button text @click="handleLogout" class="logout-btn">退出登录</el-button>
        </div>
      </div>
    </header>
    <main class="user-main">
      <router-view />
    </main>
    
    <el-dialog v-model="showPwdDialog" title="修改密码" width="420px">
      <el-form :model="pwdForm" :rules="pwdRules" ref="pwdFormRef" label-width="80px">
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="pwdForm.oldPassword" type="password" placeholder="请输入原密码" show-password />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="pwdForm.newPassword" type="password" placeholder="请输入新密码" show-password />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="pwdForm.confirmPassword" type="password" placeholder="请再次输入新密码" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showPwdDialog = false">取消</el-button>
        <el-button type="primary" @click="handleChangePwd" :loading="pwdLoading">确定</el-button>
      </template>
    </el-dialog>
    <footer class="user-footer">
      <p>🐾 土豆宠物 © 2026 · 用心呵护每一只小生命 🐾</p>
    </footer>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import axios from 'axios'
import { userApi } from '../api'

const router = useRouter()
const username = ref(localStorage.getItem('username') || '用户')
const balance = ref('0.00')

const fetchUserInfo = async () => {
  try {
    const res = await axios.get('/api/user/info', {
      headers: { Authorization: 'Bearer ' + localStorage.getItem('token') }
    })
    if (res.data.code === 200 && res.data.data.balance != null) {
      balance.value = Number(res.data.data.balance).toFixed(2)
    }
  } catch (e) {
    console.error('获取用户信息失败:', e)
  }
}

const showPwdDialog = ref(false)
const pwdLoading = ref(false)
const pwdFormRef = ref(null)
const pwdForm = ref({ oldPassword: '', newPassword: '', confirmPassword: '' })
const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [{ required: true, message: '请输入新密码', trigger: 'blur' }, { min: 4, message: '密码至少4位', trigger: 'blur' }],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: (rule, value, callback) => { if (value !== pwdForm.value.newPassword) callback(new Error('两次密码不一致')); else callback() }, trigger: 'blur' }
  ]
}

const handleChangePwd = async () => {
  try {
    await pwdFormRef.value.validate()
    pwdLoading.value = true
    await userApi.changePassword({ oldPassword: pwdForm.value.oldPassword, newPassword: pwdForm.value.newPassword })
    ElMessage.success('密码修改成功')
    showPwdDialog.value = false
    pwdForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
  } catch (e) {
    if (e.response) ElMessage.error(e.response?.data?.message || '修改失败')
  }
  pwdLoading.value = false
}

const handleLogout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('username')
  localStorage.removeItem('role')
  localStorage.removeItem('balance')
  ElMessage.success('已退出登录')
  window.location.href = '/login'
}

onMounted(() => { fetchUserInfo() })
</script>

<style scoped>
.user-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: linear-gradient(135deg, #fef9f0 0%, #fff5e6 30%, #f0f7ff 70%, #fef9f0 100%);
  position: relative;
  overflow: hidden;
}

.user-layout::before {
  content: '';
  position: absolute;
  top: -200px;
  right: -200px;
  width: 500px;
  height: 500px;
  background: radial-gradient(circle, rgba(255, 182, 107, 0.1) 0%, transparent 70%);
  pointer-events: none;
  animation: floatBg 8s ease-in-out infinite;
}

.user-layout::after {
  content: '';
  position: absolute;
  bottom: -150px;
  left: -150px;
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, rgba(130, 180, 255, 0.08) 0%, transparent 70%);
  pointer-events: none;
  animation: floatBg 10s ease-in-out infinite reverse;
}

@keyframes floatBg {
  0%, 100% { transform: translate(0, 0) scale(1); }
  50% { transform: translate(30px, -20px) scale(1.05); }
}

.user-header {
  background: linear-gradient(135deg, #ff9a56 0%, #ff6b6b 50%, #ff8e53 100%);
  box-shadow: 0 4px 20px rgba(255, 107, 107, 0.3);
  position: sticky;
  top: 0;
  z-index: 100;
  padding: 0;
}

.header-inner {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  padding: 0 24px;
  height: 60px;
}

.logo-area {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  user-select: none;
}

.paw-icon {
  font-size: 28px;
  animation: pawBounce 1.5s ease infinite;
}

@keyframes pawBounce {
  0%, 100% { transform: scale(1); }
  30% { transform: scale(1.2); }
  50% { transform: scale(0.9); }
}

.logo-text {
  font-size: 20px;
  font-weight: 700;
  color: white;
  letter-spacing: 2px;
}

.nav-links {
  display: flex;
  gap: 8px;
  margin-left: 40px;
  flex: 1;
}

.nav-item {
  color: rgba(255,255,255,0.85);
  text-decoration: none;
  padding: 8px 18px;
  border-radius: 20px;
  font-size: 14px;
  transition: all 0.3s;
}

.nav-item:hover {
  background: rgba(255,255,255,0.2);
  color: #fff;
  transform: translateY(-1px);
}

.nav-item.router-link-exact-active {
  background: rgba(255,255,255,0.25);
  color: #fff;
  font-weight: 600;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 16px;
  color: white;
  font-size: 14px;
}

.balance {
  background: rgba(255,255,255,0.2);
  padding: 4px 14px;
  border-radius: 12px;
  font-weight: 600;
}

.pwd-btn { color: rgba(255,255,255,0.9) !important; font-size: 14px; }
.pwd-btn:hover { color: #fff !important; }

.logout-btn {
  color: rgba(255,255,255,0.8) !important;
}

.logout-btn:hover {
  color: #fff !important;
}

.user-main {
  flex: 1;
  max-width: 1200px;
  width: 100%;
  margin: 0 auto;
  padding: 24px;
  position: relative;
  z-index: 1;
}

.user-footer {
  text-align: center;
  padding: 20px;
  color: #999;
  font-size: 13px;
  border-top: 1px solid rgba(0,0,0,0.04);
}
</style>
