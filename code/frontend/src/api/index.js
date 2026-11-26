import request from '../utils/request'

export const dashboardApi = {
  getStatistics() {
    return request.get('/dashboard/statistics')
  }
}

export const storeApi = {
  page(params) {
    return request.get('/store/page', { params })
  },
  list() {
    return request.get('/store/list')
  },
  getById(id) {
    return request.get(`/store/${id}`)
  },
  add(data) {
    return request.post('/store', data)
  },
  update(data) {
    return request.put('/store', data)
  },
  delete(id) {
    return request.delete(`/store/${id}`)
  }
}

export const employeeApi = {
  page(params) {
    return request.get('/employee/page', { params })
  },
  getById(id) {
    return request.get(`/employee/${id}`)
  },
  add(data) {
    return request.post('/employee', data)
  },
  update(data) {
    return request.put('/employee', data)
  },
  delete(id) {
    return request.delete(`/employee/${id}`)
  }
}

export const categoryApi = {
  page(params) {
    return request.get('/category/page', { params })
  },
  list() {
    return request.get('/category/list')
  },
  getById(id) {
    return request.get(`/category/${id}`)
  },
  add(data) {
    return request.post('/category', data)
  },
  update(data) {
    return request.put('/category', data)
  },
  delete(id) {
    return request.delete(`/category/${id}`)
  }
}

export const customerApi = {
  page(params) {
    return request.get('/customer/page', { params })
  },
  list() {
    return request.get('/customer/list')
  },
  getById(id) {
    return request.get(`/customer/${id}`)
  },
  add(data) {
    return request.post('/customer', data)
  },
  update(data) {
    return request.put('/customer', data)
  },
  delete(id) {
    return request.delete(`/customer/${id}`)
  }
}

export const petApi = {
  page(params) {
    return request.get('/pet/page', { params })
  },
  getById(id) {
    return request.get(`/pet/${id}`)
  },
  add(data) {
    return request.post('/pet', data)
  },
  update(data) {
    return request.put('/pet', data)
  },
  delete(id) {
    return request.delete(`/pet/${id}`)
  }
}

export const productApi = {
  page(params) {
    return request.get('/product/page', { params })
  },
  getById(id) {
    return request.get(`/product/${id}`)
  },
  add(data) {
    return request.post('/product', data)
  },
  update(data) {
    return request.put('/product', data)
  },
  delete(id) {
    return request.delete(`/product/${id}`)
  }
}

export const productOrderApi = {
  page(params) {
    return request.get('/product-order/page', { params })
  },
  getById(id) {
    return request.get(`/product-order/${id}`)
  },
  complete(id) {
    return request.put(`/product-order/${id}/complete`)
  }
}

export const userApi = {
  changePassword(data) {
    return request.post('/change-password', data)
  },
  getInfo() {
    return request.get('/user/info')
  }
}

export const rechargeApi = {
  createOrder(data) {
    return request.post('/user/recharge/order', data)
  },
  mockPay(data) {
    return request.post('/user/recharge/mock-pay', data)
  },
  orders(params) {
    return request.get('/user/recharge/orders', { params })
  },
  orderDetail(orderNo) {
    return request.get(`/user/recharge/order/${orderNo}`)
  }
}
