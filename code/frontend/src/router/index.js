import { createRouter, createWebHistory } from 'vue-router'
import Layout from '../layout/Layout.vue'
import UserLayout from '../layout/UserLayout.vue'
import { storeApi, customerApi, categoryApi, petApi, employeeApi } from '../api'

const createCrudConfig = (api, overrides = {}) => ({
  api,
  ...overrides
})

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'Landing',
      component: () => import('../views/Landing.vue'),
      meta: { title: '土豆宠物之家' }
    },
    {
      path: '/login',
      name: 'Login',
      component: () => import('../views/Login.vue'),
      meta: { title: '登录' }
    },
    {
      path: '/admin',
      component: Layout,
      redirect: '/admin/dashboard',
      meta: { requiresAuth: true, role: 'admin' },
      children: [
        {
          path: 'dashboard',
          name: 'Dashboard',
          component: () => import('../views/Dashboard.vue'),
          meta: { title: '数据统计', requiresAuth: true }
        },
        {
          path: 'store',
          name: 'Store',
          component: () => import('../views/CrudPage.vue'),
          meta: {
            title: '门店管理', requiresAuth: true,
            crudConfig: {
              entityName: '门店',
              api: storeApi,
              dialogWidth: '550px',
              searchFields: [
                { prop: 'name', label: '门店名称', type: 'input', placeholder: '请输入门店名称' }
              ],
              columns: [
                { prop: 'id', label: 'ID', width: '80' },
                { prop: 'avatar', label: '门店图片', width: '100', type: 'image' },
                { prop: 'name', label: '门店名称' },
                { prop: 'address', label: '地址' },
                { prop: 'phone', label: '联系电话' },
                { prop: 'createTime', label: '创建时间', width: '180' }
              ],
              formFields: [
                { prop: 'name', label: '门店名称', type: 'input', placeholder: '请输入门店名称', required: true, requiredMsg: '请输入门店名称' },
                { prop: 'avatar', label: '门店图片', type: 'image' },
                { prop: 'address', label: '地址', type: 'input', placeholder: '请输入地址' },
                { prop: 'phone', label: '联系电话', type: 'input', placeholder: '请输入联系电话' }
              ]
            }
          }
        },
        {
          path: 'employee',
          name: 'Employee',
          component: () => import('../views/Employee.vue'),
          meta: { title: '员工管理', requiresAuth: true }
        },
        {
          path: 'category',
          name: 'Category',
          component: () => import('../views/CrudPage.vue'),
          meta: {
            title: '宠物种类', requiresAuth: true,
            crudConfig: {
              entityName: '种类',
              api: categoryApi,
              searchFields: [
                { prop: 'name', label: '种类名称', type: 'input', placeholder: '请输入种类名称' }
              ],
              columns: [
                { prop: 'id', label: 'ID', width: '80' },
                { prop: 'name', label: '种类名称' },
                { prop: 'description', label: '描述' },
                { prop: 'createTime', label: '创建时间', width: '180' }
              ],
              formFields: [
                { prop: 'name', label: '种类名称', type: 'input', placeholder: '请输入种类名称', required: true, requiredMsg: '请输入种类名称' },
                { prop: 'description', label: '描述', type: 'textarea', rows: 3, placeholder: '请输入描述' }
              ]
            }
          }
        },
        {
          path: 'customer',
          name: 'Customer',
          component: () => import('../views/CrudPage.vue'),
          meta: {
            title: '客户管理', requiresAuth: true,
            crudConfig: {
              entityName: '客户',
              api: customerApi,
              searchFields: [
                { prop: 'name', label: '客户姓名', type: 'input', placeholder: '请输入客户姓名' },
                { prop: 'phone', label: '联系电话', type: 'input', placeholder: '请输入联系电话' }
              ],
              columns: [
                { prop: 'id', label: 'ID', width: '80' },
                { prop: 'name', label: '客户姓名' },
                { prop: 'phone', label: '联系电话' },
                { prop: 'address', label: '地址' },
                { prop: 'balance', label: '余额(¥)', width: '120' },
                { prop: 'createTime', label: '创建时间', width: '180' }
              ],
              formFields: [
                { prop: 'name', label: '客户姓名', type: 'input', placeholder: '请输入客户姓名', required: true, requiredMsg: '请输入客户姓名' },
                { prop: 'phone', label: '联系电话', type: 'input', placeholder: '请输入联系电话' },
                { prop: 'address', label: '地址', type: 'input', placeholder: '请输入地址' },
                { prop: 'balance', label: '余额(¥)', type: 'input', placeholder: '请输入余额' }
              ]
            }
          }
        },
        {
          path: 'pet',
          name: 'Pet',
          component: () => import('../views/Pet.vue'),
          meta: { title: '宠物管理', requiresAuth: true }
        },
        {
          path: 'product',
          name: 'Product',
          component: () => import('../views/Product.vue'),
          meta: { title: '商品管理', requiresAuth: true }
        },
        {
          path: 'orders',
          name: 'ProductOrders',
          component: () => import('../views/ProductOrders.vue'),
          meta: { title: '订单管理', requiresAuth: true }
        }
      ]
    },
    {
      path: '/user',
      component: UserLayout,
      redirect: '/user/home',
      meta: { requiresAuth: true, role: 'user' },
      children: [
        {
          path: 'home',
          name: 'UserHome',
          component: () => import('../views/user/UserHome.vue'),
          meta: { title: '宠物之家', requiresAuth: true, role: 'user' }
        },
        {
          path: 'ai',
          name: 'UserAI',
          component: () => import('../views/user/UserAI.vue'),
          meta: { title: 'AI宠物专家', requiresAuth: true, role: 'user' }
        },
        {
          path: 'pets',
          name: 'UserPets',
          component: () => import('../views/user/UserPets.vue'),
          meta: { title: '宠物浏览', requiresAuth: true, role: 'user' }
        },
        {
          path: 'stores',
          name: 'UserStores',
          component: () => import('../views/user/UserStores.vue'),
          meta: { title: '门店浏览', requiresAuth: true, role: 'user' }
        },
        {
          path: 'recharge',
          name: 'UserRecharge',
          component: () => import('../views/user/UserRecharge.vue'),
          meta: { title: '账户充值', requiresAuth: true, role: 'user' }
        }
      ]
    }
  ]
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  const role = localStorage.getItem('role')
  const isLoggedIn = !!token

  if (to.meta.requiresAuth && !isLoggedIn) {
    next('/login')
    return
  }

  if (to.path === '/login' && isLoggedIn) {
    next(role === 'user' ? '/user/home' : '/admin/dashboard')
    return
  }

  if (to.meta.role && to.meta.role !== role) {
    next(role === 'user' ? '/user/home' : '/admin/dashboard')
    return
  }

  next()
})

export default router
