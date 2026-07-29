import { api } from 'src/boot/axios';

export const memberService = {
  getMembers(tenantId: string, page = 0, size = 50) {
    return api.get(`/tenants/${tenantId}/members`, { params: { page, size } });
  },

  updateRole(tenantId: string, userId: string, role: string) {
    return api.put(`/tenants/${tenantId}/members/${userId}/role`, null, { params: { role } });
  },

  removeMember(tenantId: string, userId: string) {
    return api.delete(`/tenants/${tenantId}/members/${userId}`);
  }
};
