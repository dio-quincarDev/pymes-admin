import { api } from 'src/boot/axios'
import type { Prestamo, PrestamoRequest, PagoPrestamo, PagoPrestamoRequest } from '../types'

export const prestamoService = {
  getAll(tenantId: string) {
    return api.get<Prestamo[]>('/core/prestamos', { params: { tenantId } })
  },
  getById(id: string, tenantId: string) {
    return api.get<Prestamo>(`/core/prestamos/${id}`, { params: { tenantId } })
  },
  create(data: PrestamoRequest) {
    return api.post<Prestamo>('/core/prestamos', data)
  },
  update(id: string, data: PrestamoRequest) {
    return api.put<Prestamo>(`/core/prestamos/${id}`, data)
  },
  remove(id: string, tenantId: string) {
    return api.delete(`/core/prestamos/${id}`, { params: { tenantId } })
  },
  getPagos(loanId: string, tenantId: string) {
    return api.get<PagoPrestamo[]>(`/core/prestamos/${loanId}/pagos`, { params: { tenantId } })
  },
  createPago(loanId: string, data: PagoPrestamoRequest, tenantId: string) {
    return api.post<PagoPrestamo>(`/core/prestamos/${loanId}/pagos`, data, { params: { tenantId } })
  },
}
