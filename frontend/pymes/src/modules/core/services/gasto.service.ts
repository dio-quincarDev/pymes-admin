import { api } from 'src/boot/axios'
import type { GastoOperativo, GastoRequest } from '../types'

export const gastoService = {
  getAll(tenantId: string) {
    return api.get<GastoOperativo[]>('/core/gastos', { params: { tenantId } })
  },
  getById(id: string, tenantId: string) {
    return api.get<GastoOperativo>(`/core/gastos/${id}`, { params: { tenantId } })
  },
  create(data: GastoRequest) {
    return api.post<GastoOperativo>('/core/gastos', data)
  },
  update(id: string, data: GastoRequest) {
    return api.put<GastoOperativo>(`/core/gastos/${id}`, data)
  },
  remove(id: string, tenantId: string) {
    return api.delete(`/core/gastos/${id}`, { params: { tenantId } })
  },
}
