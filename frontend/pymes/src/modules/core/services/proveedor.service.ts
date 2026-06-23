import { api } from 'src/boot/axios'
import type { Proveedor, ProveedorRequest } from '../types'

export const proveedorService = {
  getAll(tenantId: string) {
    return api.get<Proveedor[]>('/core/proveedores', { params: { tenantId } })
  },
  getById(id: string, tenantId: string) {
    return api.get<Proveedor>(`/core/proveedores/${id}`, { params: { tenantId } })
  },
  create(data: ProveedorRequest) {
    return api.post<Proveedor>('/core/proveedores', data)
  },
  update(id: string, data: ProveedorRequest) {
    return api.put<Proveedor>(`/core/proveedores/${id}`, data)
  },
  remove(id: string, tenantId: string) {
    return api.delete(`/core/proveedores/${id}`, { params: { tenantId } })
  },
}
