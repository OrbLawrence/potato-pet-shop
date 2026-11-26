<template>
  <div class="home-page">
    <div class="welcome-banner">
      <div class="banner-content">
        <div class="banner-animals">
          <span class="animal animal-dog">🐕</span>
          <span class="animal animal-cat">🐈</span>
          <span class="animal animal-bird">🐦</span>
          <span class="animal animal-rabbit">🐰</span>
          <span class="animal animal-hamster">🐹</span>
        </div>
        <h1>欢迎回来，{{ username }}！</h1>
        <p>今天也来看看可爱的毛孩子们吧 🐾</p>
      </div>
    </div>

    <div class="quick-actions">
      <div class="action-card" @click="$router.push('/user/pets')">
        <div class="action-icon pets-bg">🐕</div>
        <h3>萌宠浏览</h3>
        <p>看看我们的毛孩子们</p>
      </div>
      <div class="action-card" @click="$router.push('/user/stores')">
        <div class="action-icon stores-bg">🏪</div>
        <h3>门店浏览</h3>
        <p>找到最近的宠物之家</p>
      </div>
      <div class="action-card" @click="$router.push('/user/recharge')">
        <div class="action-icon recharge-bg">💰</div>
        <h3>账户充值</h3>
        <p>余额: ¥{{ balance }}</p>
      </div>
      <div class="action-card" @click="$router.push('/user/ai')">
        <div class="action-icon ai-bg">🤖</div>
        <h3>智能客服</h3>
        <p>随时随地解答宠物问题</p>
      </div>
    </div>

    <div class="info-section">
      <div class="info-card">
        <h3>📊 平台数据</h3>
        <div class="stats-grid">
          <div class="stat-item">
            <span class="stat-num">{{ storeCount }}</span>
            <span class="stat-label">合作门店</span>
          </div>
          <div class="stat-item">
            <span class="stat-num">{{ petCount }}</span>
            <span class="stat-label">可爱萌宠</span>
          </div>
          <div class="stat-item">
            <span class="stat-num">{{ categoryCount }}</span>
            <span class="stat-label">宠物种类</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { storeApi, petApi, categoryApi } from '../../api'
import axios from 'axios'

const username = ref(localStorage.getItem('username') || '用户')
const balance = ref('0.00')
const storeCount = ref(0)
const petCount = ref(0)
const categoryCount = ref(0)

onMounted(async () => {
  try {
    const res = await axios.get('/api/user/info', {
      headers: { Authorization: 'Bearer ' + localStorage.getItem('token') }
    })
    if (res.data.code === 200 && res.data.data.balance != null) {
      balance.value = Number(res.data.data.balance).toFixed(2)
    }
  } catch (e) {}

  try {
    const [sRes, pRes, cRes] = await Promise.all([
      storeApi.page({ pageNum: 1, pageSize: 1 }),
      petApi.page({ pageNum: 1, pageSize: 1 }),
      categoryApi.page({ pageNum: 1, pageSize: 1 })
    ])
    storeCount.value = sRes.data.total
    petCount.value = pRes.data.total
    categoryCount.value = cRes.data.total
  } catch (e) { console.error(e) }
})
</script>

<style scoped>
.home-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.welcome-banner {
  background: linear-gradient(135deg, #ffecd2 0%, #fcb69f 100%);
  border-radius: 20px;
  padding: 40px;
  text-align: center;
  position: relative;
  overflow: hidden;
  box-shadow: 0 8px 30px rgba(252, 182, 159, 0.25);
}

.banner-animals {
  display: flex;
  justify-content: center;
  gap: 16px;
  margin-bottom: 16px;
}

.animal {
  font-size: 36px;
  display: inline-block;
  animation: animalFloat 3s ease-in-out infinite;
}

.animal-dog { animation-delay: 0s; }
.animal-cat { animation-delay: 0.4s; }
.animal-bird { animation-delay: 0.8s; }
.animal-rabbit { animation-delay: 1.2s; }
.animal-hamster { animation-delay: 1.6s; }

@keyframes animalFloat {
  0%, 100% { transform: translateY(0) rotate(0deg); }
  30% { transform: translateY(-12px) rotate(-5deg); }
  60% { transform: translateY(-6px) rotate(5deg); }
}

.welcome-banner h1 {
  font-size: 28px;
  color: #5a3e2b;
  margin: 0 0 8px 0;
}

.welcome-banner p {
  font-size: 16px;
  color: #8b6914;
  margin: 0;
}

.quick-actions {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.action-card {
  background: white;
  border-radius: 20px;
  padding: 28px 20px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 4px 15px rgba(0,0,0,0.06);
}

.action-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 12px 30px rgba(0,0,0,0.1);
}

.action-icon {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 30px;
  margin: 0 auto 12px;
}

.pets-bg { background: linear-gradient(135deg, #ffecd2, #fcb69f); }
.stores-bg { background: linear-gradient(135deg, #a1c4fd, #c2e9fb); }
.recharge-bg { background: linear-gradient(135deg, #f6d365, #fda085); }
.ai-bg { background: linear-gradient(135deg, #a18cd1, #fbc2eb); }

.action-card h3 { font-size: 18px; color: #333; margin: 0 0 6px 0; }
.action-card p { font-size: 14px; color: #999; margin: 0; }

.info-section {
  display: grid;
  grid-template-columns: 1fr;
  gap: 20px;
}

.info-card {
  background: white;
  border-radius: 20px;
  padding: 24px;
  box-shadow: 0 4px 15px rgba(0,0,0,0.06);
}

.info-card h3 {
  font-size: 18px;
  color: #333;
  margin: 0 0 20px 0;
  padding-bottom: 12px;
  border-bottom: 2px solid #fef9f0;
}

.stats-grid {
  display: flex;
  justify-content: space-around;
  text-align: center;
}

.stat-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.stat-num {
  font-size: 36px;
  font-weight: 700;
  background: linear-gradient(135deg, #ff9a56, #ff6b6b);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.stat-label {
  font-size: 14px;
  color: #999;
}

@media (max-width: 768px) {
  .quick-actions { grid-template-columns: 1fr 1fr; }
  .welcome-banner { padding: 24px; }
}
</style>
