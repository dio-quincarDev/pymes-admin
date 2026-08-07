import { api } from 'src/boot/axios'
import type { Patrimonio, PatrimonioRequest } from '../types'

export const patrimonioService = {
  get(tenantId: string) {
    return api.get<Patrimonio>(`/core/patrimonio/${tenantId}`)
  },
  update(tenantId: string, data: PatrimonioRequest) {
    return api.put<Patrimonio>(`/core/patrimonio/${tenantId}`, data)
  },
}
