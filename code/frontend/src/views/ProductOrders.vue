<template>
  <div class="page">
    <el-card class="custom-card">
      <el-form :inline="true" :model="query" class="search-form">
        <el-form-item label="关键词">
          <el-input
            v-model="query.keyword"
            placeholder="订单号 / 客户 / 门店 / 核销码"
            clearable
            style="width: 280px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="订单状态">
          <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 150px">
            <el-option label="待核销" value="paid" />
            <el-option label="已完成" value="completed" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" border stripe v-loading="loading">
        <el-table-column prop="orderNo" label="订单号" width="190" />
        <el-table-column prop="customerName" label="客户名称" width="110" />
        <el-table-column prop="storeName" label="下单门店" min-width="140" />
        <el-table-column label="订单金额" width="110">
          <template #default="{ row }"><span class="amount">¥{{ formatMoney(row.amount) }}</span></template>
        </el-table-column>
        <el-table-column label="商品明细" min-width="230" show-overflow-tooltip>
          <template #default="{ row }">{{ formatItems(row.items) }}</template>
        </el-table-column>
        <el-table-column label="支付渠道" width="110">
          <template #default="{ row }">{{ paymentLabel(row.paymentChannel) }}</template>
        </el-table-column>
        <el-table-column label="核销码" width="120" align="center">
          <template #default="{ row }"><span class="verify-code">{{ row.verificationCode }}</span></template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'completed' ? 'success' : 'warning'" effect="light">
              {{ row.status === 'completed' ? '已完成' : '待核销' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="下单时间" width="180" />
        <el-table-column label="操作" width="190" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="showDetail(row)">详情</el-button>
            <el-button
              v-if="row.status === 'paid'"
              type="success"
              size="small"
              @click="handleComplete(row)"
            >完成订单</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        class="pagination"
        @size-change="fetchData"
        @current-change="fetchData"
      />
    </el-card>

    <el-dialog v-model="detailVisible" title="订单详情" width="720px" destroy-on-close>
      <template v-if="detail">
        <div class="detail-grid">
          <div><span>订单号</span><strong>{{ detail.orderNo }}</strong></div>
          <div><span>客户名称</span><strong>{{ detail.customerName }}</strong></div>
          <div><span>下单门店</span><strong>{{ detail.storeName }}</strong></div>
          <div><span>支付渠道</span><strong>{{ paymentLabel(detail.paymentChannel) }}</strong></div>
          <div><span>订单金额</span><strong class="amount">¥{{ formatMoney(detail.amount) }}</strong></div>
          <div><span>订单状态</span><strong>{{ detail.status === 'completed' ? '已完成' : '待核销' }}</strong></div>
        </div>

        <div class="verify-panel">
          <span>6位核销码</span>
          <strong>{{ detail.verificationCode }}</strong>
        </div>

        <div class="item-list">
          <div v-for="item in detail.items || []" :key="item.id" class="item-row">
            <el-image :src="item.productImage || placeholderImage" class="item-image" fit="cover">
              <template #error><img :src="placeholderImage" class="item-image" alt="商品占位图" /></template>
            </el-image>
            <div class="item-main">
              <strong>{{ item.productName }}</strong>
              <span>¥{{ formatMoney(item.price) }} × {{ item.quantity }}</span>
            </div>
            <span class="item-subtotal">¥{{ formatMoney(item.subtotal) }}</span>
          </div>
        </div>

        <div class="time-line">
          <span>下单并支付：{{ detail.payTime || detail.createTime }}</span>
          <span v-if="detail.completeTime">完成时间：{{ detail.completeTime }}</span>
        </div>
      </template>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button
          v-if="detail && detail.status === 'paid'"
          type="success"
          :loading="completingId === detail.id"
          @click="handleComplete(detail)"
        >完成订单</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { productOrderApi } from '../api'
import placeholderImage from '../assets/product-placeholder.jpg'

const loading = ref(false)
const completingId = ref(null)
const tableData = ref([])
const total = ref(0)
const detailVisible = ref(false)
const detail = ref(null)
const query = reactive({ pageNum: 1, pageSize: 10, keyword: '', status: '' })

function formatMoney(value) {
  return Number(value || 0).toFixed(2)
}

function paymentLabel(channel) {
  return { balance: '余额支付', wechat: '微信支付', alipay: '支付宝' }[channel] || channel || '-'
}

function formatItems(items = []) {
  return items.map(item => `${item.productName} ×${item.quantity}`).join('、') || '-'
}

async function fetchData(silent = false) {
  if (!silent) loading.value = true
  try {
    const res = await productOrderApi.page({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      keyword: query.keyword || undefined,
      status: query.status || undefined
    })
    tableData.value = res.data.list || []
    total.value = res.data.total || 0
  } catch (error) {
    console.error(error)
  } finally {
    if (!silent) loading.value = false
  }
}

function handleSearch() {
  query.pageNum = 1
  fetchData()
}

function handleReset() {
  query.keyword = ''
  query.status = ''
  handleSearch()
}

async function showDetail(row) {
  try {
    const res = await productOrderApi.getById(row.id)
    detail.value = res.data
    detailVisible.value = true
  } catch (error) {
    console.error(error)
  }
}

async function handleComplete(row) {
  try {
    await ElMessageBox.confirm(
      `确认订单 ${row.orderNo} 已完成核销吗？完成后订单状态将不可回退。`,
      '完成订单',
      { confirmButtonText: '确认完成', cancelButtonText: '取消', type: 'warning' }
    )
    completingId.value = row.id
    await productOrderApi.complete(row.id)
    ElMessage.success('订单已标记为已完成')
    if (detail.value && detail.value.id === row.id) detail.value.status = 'completed'
    detailVisible.value = false
    fetchData()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') console.error(error)
  } finally {
    completingId.value = null
  }
}

let refreshTimer = null

onMounted(() => {
  fetchData()
  refreshTimer = window.setInterval(() => fetchData(true), 8000)
})

onBeforeUnmount(() => {
  if (refreshTimer) window.clearInterval(refreshTimer)
})
</script>

<style scoped>
.page { position: relative; z-index: 1; }
.custom-card { background: rgba(255, 255, 255, 0.9); border-radius: 16px; box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08); }
.search-form { margin-bottom: 8px; }
.amount { color: #e8873a; font-weight: 700; }
.verify-code { display: inline-block; padding: 3px 10px; border-radius: 6px; color: #9a5a18; background: #fff1dc; font-family: Consolas, monospace; font-weight: 800; letter-spacing: 2px; }
.pagination { margin-top: 20px; justify-content: flex-end; }
.detail-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 18px; padding: 20px; background: #fff9f1; border-radius: 12px; }
.detail-grid div { display: flex; flex-direction: column; gap: 8px; min-width: 0; }
.detail-grid span, .time-line { color: #9b8574; font-size: 13px; }
.detail-grid strong { color: #4a3728; word-break: break-all; }
.verify-panel { margin: 18px 0; padding: 18px 24px; display: flex; justify-content: space-between; align-items: center; border-radius: 12px; background: linear-gradient(135deg, #fff0dc, #ffe7cf); color: #7d4b17; }
.verify-panel strong { font-family: Consolas, monospace; font-size: 28px; letter-spacing: 8px; color: #e27b25; }
.item-list { border: 1px solid #f1e5d7; border-radius: 12px; overflow: hidden; }
.item-row { display: flex; align-items: center; gap: 16px; padding: 14px 18px; border-bottom: 1px solid #f5ede4; }
.item-row:last-child { border-bottom: none; }
.item-image { width: 58px; height: 58px; border-radius: 9px; flex: none; }
.item-main { flex: 1; display: flex; flex-direction: column; gap: 6px; color: #4a3728; }
.item-main span { color: #9b8574; font-size: 13px; }
.item-subtotal { color: #e8873a; font-weight: 700; }
.time-line { display: flex; justify-content: space-between; margin-top: 18px; }
</style>
