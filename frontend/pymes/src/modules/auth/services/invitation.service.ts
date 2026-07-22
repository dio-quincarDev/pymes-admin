import { api } from 'src/boot/axios';

export const invitationService = {
  getPendingInvitations(page = 0, size = 10) {
    return api.get('/invitations', {
      params: { page, size }
    });
  },

  createInvitation(data: { tenantId: string; email: string; role: string }) {
    return api.post('/invitations', data);
  },

  cancelInvitation(invitationId: string) {
    return api.delete(`/invitations/${invitationId}`);
  },

  acceptInvitation(invitationToken: string) {
    return api.post('/invitations/accept', { invitationToken });
  },

  getInvitationInfo(token: string) {
    return api.get(`/invitations/${token}/info`);
  }
};