import { api } from 'src/boot/axios'
import type { SetupInfo } from '../types'

export const setupService = {
  get(tenantId: string) {
    return api.get<SetupInfo>(`/core/setup/${tenantId}`)
  },
  completeOnboarding(tenantId: string, industry: string) {
    return api.post<SetupInfo>(`/core/setup/${tenantId}/onboarding`, { industry })
  },
  preview(industry: string) {
    return api.get<SetupInfo>(`/core/setup/preview/${industry}`)
  },
}
