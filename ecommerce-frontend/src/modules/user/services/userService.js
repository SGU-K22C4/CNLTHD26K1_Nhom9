import api from '@/shared/utils/api'
import { buildUserHeaders } from '@/shared/utils/userHeaders'

export const userService = {
  /** GET /api/v1/users/me → UserProfileResponse */
  getProfile: async () => {
    const res = await api.get('/api/v1/users/me', { headers: buildUserHeaders() })
    return res
  },

  /** GET /api/v1/users/me/addresses → List<AddressResponse> */
  getAddresses: async () => {
    const res = await api.get('/api/v1/users/me/addresses', { headers: buildUserHeaders() })
    return res
  },

  /** POST /api/v1/users/me/addresses → AddressResponse */
  addAddress: async (address) => {
    const res = await api.post('/api/v1/users/me/addresses', address, { headers: buildUserHeaders() })
    return res
  },

  /** PUT /api/v1/users/me/addresses/{addressId} → AddressResponse */
  updateAddress: async (addressId, address) => {
    const res = await api.put(`/api/v1/users/me/addresses/${addressId}`, address, { headers: buildUserHeaders() })
    return res
  },

  /** DELETE /api/v1/users/me/addresses/{addressId} */
  deleteAddress: async (addressId) => {
    const res = await api.delete(`/api/v1/users/me/addresses/${addressId}`, { headers: buildUserHeaders() })
    return res
  },

  updateProfile: async (data) => {
    const res = await api.put('/api/v1/users/me', data, { headers: buildUserHeaders() })
    return res
  },

  /** PATCH /api/v1/users/me/password */
  changePassword: async (data) => {
    const res = await api.patch('/api/v1/users/me/password', data, { headers: buildUserHeaders() })
    return res
  },
}