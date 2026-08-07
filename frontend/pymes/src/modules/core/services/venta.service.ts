import { api } from 'src/boot/axios'
import type { VentaDiaria, VentaRequest } from '../types'

export const ventaService = {
  getAll(tenantId: string) {
    return api.get<VentaDiaria[]>('/core/ventas', { params: { tenantId } })
  },
  getById(id: string, tenantId: string) {
    return api.get<VentaDiaria>(`/core/ventas/${id}`, { params: { tenantId } })
  },
  create(data: VentaRequest) {
    return api.post<VentaDiaria>('/core/ventas', data)
  },
  update(id: string, data: VentaRequest) {
    return api.put<VentaDiaria>(`/core/ventas/${id}`, data)
  },
  remove(id: string, tenantId: string) {
    return api.delete(`/core/ventas/${id}`, { params: { tenantId } })
  },
}
