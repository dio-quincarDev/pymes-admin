import { api } from 'src/boot/axios';

export interface CreateTenantRequest {
  name: string;
  slug: string;
  industry?: string;
}

export const tenantService = {
  getUserTenants(page = 0, size = 10) {
    return api.get(`/tenants?page=${page}&size=${size}`);
  },

  createTenant(data: CreateTenantRequest) {
    return api.post('/tenants', data);
  },

  selectTenant(tenantId: string) {
    return api.post('/tenants/select', { tenantId });
  }
};
