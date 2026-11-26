<template>
  <el-container class="layout-container">
    <el-aside width="220px">
      <div class="logo">
        <div class="logo-icon">🐾</div>
        <h2>土豆宠物之家运营系统</h2>
      </div>
      <el-menu
        :default-active="activeMenu"
        router
        background-color="transparent"
        text-color="#5a4a3a"
        active-text-color="#ff6b6b"
        class="custom-menu"
      >
        <el-menu-item index="/admin/dashboard">
          <span class="menu-icon">📊</span>
          <span>数据统计</span>
        </el-menu-item>
        <el-menu-item index="/admin/store">
          <span class="menu-icon">🏪</span>
          <span>门店管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/employee">
          <span class="menu-icon">👨‍⚕️</span>
          <span>员工管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/category">
          <span class="menu-icon">🏷️</span>
          <span>宠物种类</span>
        </el-menu-item>
        <el-menu-item index="/admin/customer">
          <span class="menu-icon">👥</span>
          <span>客户管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/pet">
          <span class="menu-icon">🐕</span>
          <span>宠物管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/product">
          <span class="menu-icon">🛍️</span>
          <span>商品管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/orders">
          <span class="menu-icon">📦</span>
          <span>订单管理</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div class="header-content">
          <span class="title">{{ currentTitle }}</span>
          <div class="header-right">
            <span class="username">👤 {{ username }}</span>
            <el-button text @click="showPwdDialog = true" class="pwd-btn">修改密码</el-button>
            <el-button text @click="handleLogout" class="logout-btn">退出登录</el-button>
          </div>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>

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
  </el-container>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { userApi } from '../api'

const route = useRoute()
const router = useRouter()
const activeMenu = computed(() => route.path)
const currentTitle = computed(() => route.meta?.title || '土豆宠物之家运营系统')
const username = ref(localStorage.getItem('username') || '')

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
</script>

<style scoped>
.layout-container {
  height: 100%;
}

.el-aside {
  background: linear-gradient(180deg, #fef9f0 0%, #fff5eb 40%, #f0f7ff 100%);
  box-shadow: 2px 0 20px rgba(255, 107, 107, 0.08);
  position: relative;
  overflow: hidden;
}

.el-aside::after {
  content: '';
  position: absolute;
  bottom: -50px;
  right: -50px;
  width: 150px;
  height: 150px;
  background: radial-gradient(circle, rgba(255, 154, 86, 0.08) 0%, transparent 70%);
  pointer-events: none;
}

.logo {
  height: 80px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #ff9a56 0%, #ff6b6b 100%);
  color: white;
  box-shadow: 0 4px 20px rgba(255, 107, 107, 0.3);
}

.logo-icon {
  font-size: 28px;
  animation: pawBounce 1.6s ease infinite;
}

@keyframes pawBounce {
  0%, 100% { transform: scale(1); }
  30% { transform: scale(1.25); }
  50% { transform: scale(0.9); }
}

.logo h2 {
  font-size: 14px;
  margin: 4px 0 0 0;
  font-weight: 500;
}

.custom-menu {
  border-right: none;
}

.custom-menu .el-menu-item {
  margin: 6px 12px;
  border-radius: 14px;
  transition: all 0.3s ease;
  font-size: 14px;
}

.custom-menu .el-menu-item:hover {
  background: rgba(255, 154, 86, 0.1) !important;
  transform: translateX(4px);
}

.custom-menu .el-menu-item.is-active {
  background: linear-gradient(135deg, rgba(255, 154, 86, 0.15), rgba(255, 107, 107, 0.12)) !important;
  box-shadow: 0 4px 15px rgba(255, 107, 107, 0.15);
  font-weight: 600;
}

.menu-icon {
  margin-right: 8px;
  font-size: 16px;
}

.header {
  background: white;
  box-shadow: 0 2px 15px rgba(0, 0, 0, 0.05);
  display: flex;
  align-items: center;
  padding: 0 25px;
}

.header-content {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.title {
  font-size: 18px;
  font-weight: 600;
  background: linear-gradient(135deg, #ff9a56, #ff6b6b);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.username {
  color: #666;
  font-size: 14px;
}

.pwd-btn { color: #409eff !important; font-size: 14px; }

.logout-btn {
  color: #f56c6c !important;
  font-size: 14px;
}

.main {
  background: linear-gradient(135deg, #fef9f0 0%, #f5f7fa 50%, #f0f7ff 100%);
  padding: 24px;
  position: relative;
}
</style>
