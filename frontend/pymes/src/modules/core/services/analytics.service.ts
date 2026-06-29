import { api } from 'src/boot/axios';
import type { AnalyticsResponse } from '../types/analytics';

export const analyticsService = {
  consultar(tenantId: string, periodo?: string) {
    return api.get<AnalyticsResponse>('/core/analytics/consultar', {
      params: { tenantId, ...(periodo && { periodo }) },
    });
  },
  recalcular(tenantId: string, periodo: string) {
    return api.post<AnalyticsResponse>('/core/analytics/recalcular', null, {
      params: { tenantId, periodo },
    });
  },
};
