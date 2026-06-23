import { api } from 'src/boot/axios'
import type { Factura, FacturaRequest } from '../types'

export const facturaService = {
  getAll(tenantId: string) {
    return api.get<Factura[]>('/core/facturas', { params: { tenantId } })
  },
  getById(id: string, tenantId: string) {
    return api.get<Factura>(`/core/facturas/${id}`, { params: { tenantId } })
  },
  create(data: FacturaRequest) {
    return api.post<Factura>('/core/facturas', data)
  },
  remove(id: string, tenantId: string) {
    return api.delete(`/core/facturas/${id}`, { params: { tenantId } })
  },
  pay(id: string, tenantId: string) {
    return api.post<Factura>(`/core/facturas/${id}/pagar?tenantId=${tenantId}`)
  },
}
