import { api } from 'src/boot/axios'
import type { SetupInfo } from '../types'

export interface TenantSetup {
  id: string
  tenantId: string
  industry: string
  onboardingCompleted: boolean
}

export const setupService = {
  get(tenantId: string) {
    return api.get<SetupInfo>(`/core/setup/${tenantId}`)
  },
  completeOnboarding(tenantId: string, industry: string) {
    return api.post<TenantSetup>(`/core/setup/${tenantId}/onboarding`, { industry })
  },
}
