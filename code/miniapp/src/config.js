// 请求根地址配置：
// 1) 本地环境：读取 .env.development 的 VITE_API_BASE_URL，默认 http://localhost:3000/api
// 2) 生产环境：复制 .env.production.example 为 .env.production，并配置真实 HTTPS 域名
const envBase = import.meta.env.VITE_API_BASE_URL
const BASE_URL = (envBase || 'http://localhost:3000/api').replace(/\/+$/, '')

console.log('[PetShop] API BASE_URL =', BASE_URL)

export { BASE_URL }
