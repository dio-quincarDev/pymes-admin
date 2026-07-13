import { api } from 'src/boot/axios'
import type { MetricasFinancieras } from '../types'

export const accountingService = {
  consultar(tenantId: string, periodo?: string) {
    return api.get<MetricasFinancieras>('/core/accounting/consultar', {
      params: { tenantId, ...(periodo && { periodo }) },
    })
  },
  recalcular(tenantId: string, periodo: string) {
    return api.post<MetricasFinancieras>('/core/accounting/recalcular', null, {
      params: { tenantId, periodo },
    })
  },
}
