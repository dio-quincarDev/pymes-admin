import { api } from 'src/boot/axios'
import type { Producto, ProductoRequest, Presentacion, PresentacionRequest, PageResponse } from '../types'

export const productoService = {
  getAll(tenantId: string) {
    return api.get<Producto[]>('/core/productos', { params: { tenantId } })
  },
  search(tenantId: string, params?: { category?: string; name?: string; page?: number; size?: number }) {
    return api.get<PageResponse<Producto>>('/core/productos/search', {
      params: { tenantId, ...params },
    })
  },
  getById(id: string, tenantId: string) {
    return api.get<Producto>(`/core/productos/${id}`, { params: { tenantId } })
  },
  create(data: ProductoRequest) {
    return api.post<Producto>('/core/productos', data)
  },
  update(id: string, data: ProductoRequest) {
    return api.put<Producto>(`/core/productos/${id}`, data, { params: { tenantId: data.tenantId } })
  },
  remove(id: string, tenantId: string) {
    return api.delete(`/core/productos/${id}`, { params: { tenantId } })
  },
  addPresentation(productId: string, data: PresentacionRequest, tenantId: string) {
    return api.post<Presentacion>(`/core/productos/${productId}/presentaciones?tenantId=${tenantId}`, data)
  },
  removePresentation(id: string, tenantId: string) {
    return api.delete(`/core/productos/presentaciones/${id}`, { params: { tenantId } })
  },
}
