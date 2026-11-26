import { BASE_URL } from '../config'

const LOGIN_PATH = '/pages/login/index'

export function getToken() {
  return uni.getStorageSync('token') || ''
}

export function clearAuth() {
  uni.removeStorageSync('token')
  uni.removeStorageSync('username')
  uni.removeStorageSync('role')
  uni.removeStorageSync('balance')
  uni.removeStorageSync('userId')
  uni.removeStorageSync('conversationId')
}

function redirectToLogin() {
  const pages = getCurrentPages()
  const current = pages.length ? pages[pages.length - 1] : null
  if (current && current.route === 'pages/login/index') return
  uni.reLaunch({ url: LOGIN_PATH })
}

function cleanData(data) {
  if (!data || typeof data !== 'object') return data || {}
  const result = {}
  Object.keys(data).forEach((key) => {
    const value = data[key]
    if (value !== undefined && value !== null) result[key] = value
  })
  return result
}

function mapNetworkError(errMsg = '') {
  if (errMsg.includes('timeout')) return '请求超时，请稍后重试'
  if (errMsg.includes('fail')) return '网络连接失败，请检查网络'
  return '网络错误，请稍后重试'
}

/**
 * 统一请求封装：
 * - 自动拼接 BASE_URL
 * - 自动携带 Authorization: Bearer token
 * - 统一处理 Result { code, message, data }
 * - code === 200 时直接 resolve data
 * - code === 401 或 HTTP 401 时清除登录状态并跳转登录页
 * - 网络错误使用 uni.showToast 提示
 */
export function request(options) {
  const token = getToken()

  return new Promise((resolve, reject) => {
    uni.request({
      url: `${BASE_URL}${options.url}`,
      method: options.method || 'GET',
      data: cleanData(options.data),
      header: {
        'Content-Type': 'application/json',
        ...(options.header || {}),
        ...(token ? { Authorization: `Bearer ${token}` } : {})
      },
      timeout: options.timeout || 15000,
      success: (res) => {
        const body = res.data
        const isUnauthorized = res.statusCode === 401 || (body && body.code === 401)

        if (isUnauthorized) {
          clearAuth()
          uni.showToast({ title: (body && body.message) || '登录已过期，请重新登录', icon: 'none' })
          setTimeout(redirectToLogin, 600)
          reject(new Error((body && body.message) || '登录已过期'))
          return
        }

        if (body && body.code === 200) {
          resolve(body.data)
          return
        }

        const message = (body && body.message) || '请求失败，请稍后重试'
        uni.showToast({ title: message, icon: 'none' })
        reject(new Error(message))
      },
      fail: (err) => {
        console.error('[PetShop] 请求失败:', `${BASE_URL}${options.url}`, err)
        const message = mapNetworkError(err && err.errMsg)
        uni.showToast({ title: message, icon: 'none' })
        reject(new Error(message))
      }
    })
  })
}

export default request
