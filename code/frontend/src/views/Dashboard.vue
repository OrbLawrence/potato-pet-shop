<template>
  <div class="dashboard">
    <div class="stats-cards">
      <div class="stat-card">
        <div class="stat-icon pets-bg">🐕</div>
        <div class="stat-info">
          <div class="stat-value">{{ petCount }}</div>
          <div class="stat-label">宠物总数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon customers-bg">👥</div>
        <div class="stat-info">
          <div class="stat-value">{{ customerCount }}</div>
          <div class="stat-label">客户总数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon employees-bg">👨‍⚕️</div>
        <div class="stat-info">
          <div class="stat-value">{{ employeeCount }}</div>
          <div class="stat-label">员工总数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon stores-bg">🏪</div>
        <div class="stat-info">
          <div class="stat-value">{{ storeCount }}</div>
          <div class="stat-label">门店总数</div>
        </div>
      </div>
    </div>

    <el-row :gutter="20">
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span class="card-title">🐾 宠物种类分布</span>
            </div>
          </template>
          <div ref="categoryChartRef" class="chart"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span class="card-title">⚥ 宠物性别分布</span>
            </div>
          </template>
          <div ref="genderChartRef" class="chart"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { dashboardApi, petApi, customerApi, employeeApi, storeApi } from '../api'

const categoryChartRef = ref(null)
const genderChartRef = ref(null)
let categoryChart = null
let genderChart = null

const petCount = ref(0)
const customerCount = ref(0)
const employeeCount = ref(0)
const storeCount = ref(0)

const initCharts = async () => {
  try {
    const res = await dashboardApi.getStatistics()
    const { categoryData, genderData } = res.data

    if (!categoryChartRef.value || !genderChartRef.value) return

    const colors = ['#ff9a56', '#ff6b6b', '#f29263', '#f6d365', '#a1c4fd']

    categoryChart = echarts.init(categoryChartRef.value)
    categoryChart.setOption({
      tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
      legend: { orient: 'vertical', left: 'left', textStyle: { fontSize: 13, color: '#666' } },
      color: colors,
      series: [{
        name: '宠物种类',
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['60%', '50%'],
        itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 2 },
        label: { show: true, formatter: '{b}\n{d}%', fontSize: 12 },
        data: categoryData || [],
        emphasis: { itemStyle: { shadowBlur: 20, shadowOffsetX: 0, shadowColor: 'rgba(255, 154, 86, 0.4)' } }
      }]
    })

    genderChart = echarts.init(genderChartRef.value)
    genderChart.setOption({
      tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
      legend: { orient: 'vertical', left: 'left', textStyle: { fontSize: 13, color: '#666' } },
      color: ['#a1c4fd', '#ffb6c1'],
      series: [{
        name: '宠物性别',
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['60%', '50%'],
        itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 2 },
        label: { show: true, formatter: '{b}\n{d}%', fontSize: 12 },
        data: genderData || [],
        emphasis: { itemStyle: { shadowBlur: 20, shadowOffsetX: 0, shadowColor: 'rgba(255, 107, 107, 0.4)' } }
      }]
    })
  } catch (e) {
    console.error('图表初始化失败', e)
    ElMessage.error('数据统计加载失败')
  }
}

const fetchStats = async () => {
  try {
    const [petRes, customerRes, employeeRes, storeRes] = await Promise.all([
      petApi.page({ pageNum: 1, pageSize: 1 }),
      customerApi.page({ pageNum: 1, pageSize: 1 }),
      employeeApi.page({ pageNum: 1, pageSize: 1 }),
      storeApi.page({ pageNum: 1, pageSize: 1 })
    ])
    petCount.value = petRes.data.total
    customerCount.value = customerRes.data.total
    employeeCount.value = employeeRes.data.total
    storeCount.value = storeRes.data.total
  } catch (e) {
    console.error('统计数据获取失败:', e)
    ElMessage.error('统计数据加载失败')
  }
}

onMounted(() => {
  initCharts()
  fetchStats()
  window.addEventListener('resize', () => {
    categoryChart?.resize()
    genderChart?.resize()
  })
})
</script>

<style scoped>
.dashboard { position: relative; z-index: 1; }

.stats-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 24px;
}

.stat-card {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.92) 0%, rgba(255, 255, 255, 0.75) 100%);
  border-radius: 20px;
  padding: 24px 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 4px 20px rgba(255, 107, 107, 0.08);
  transition: all 0.3s;
}

.stat-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 30px rgba(255, 107, 107, 0.12);
}

.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
}

.pets-bg { background: linear-gradient(135deg, #ffecd2, #fcb69f); }
.customers-bg { background: linear-gradient(135deg, #a1c4fd, #c2e9fb); }
.employees-bg { background: linear-gradient(135deg, #d4fc79, #96e6a1); }
.stores-bg { background: linear-gradient(135deg, #f6d365, #fda085); }

.stat-info { flex: 1; }

.stat-value {
  font-size: 30px;
  font-weight: 700;
  background: linear-gradient(135deg, #ff9a56, #ff6b6b);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.stat-label { font-size: 13px; color: #999; margin-top: 4px; }

.chart-card {
  background: rgba(255, 255, 255, 0.85);
  border-radius: 20px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
  backdrop-filter: blur(10px);
  border: none;
}

.card-header { display: flex; align-items: center; }
.card-title { font-weight: 600; font-size: 16px; color: #333; }

.chart { height: 350px; }

@media (max-width: 1200px) { .stats-cards { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 768px) { .stats-cards { grid-template-columns: 1fr; } }
</style>
