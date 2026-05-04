export const API_ENDPOINTS = {
  workshops: {
    list: '/workshops',
    detail: (id) => `/workshops/${id}`,
  },
  registrations: {
    create: '/registrations',
    mine: '/me/registrations',
    detail: (id) => `/registrations/${id}`,
    mockPaymentSuccess: (id) => `/registrations/${id}/payment/mock-success`,
  },
  admin: {
    workshops: {
      list: '/admin/workshops',
      create: '/admin/workshops',
      update: (id) => `/admin/workshops/${id}`,
      cancel: (id) => `/admin/workshops/${id}/cancel`,
    },
    registrations: '/admin/registrations',
    statistics: '/admin/statistics',
  },
};
