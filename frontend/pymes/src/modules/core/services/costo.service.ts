import { api } from 'src/boot/axios'
import type {
  Collaborador,
  CollaboradorRequest,
  ConfigLaboral,
  ConfigLaboralRequest,
  CostoDiario,
  GastoFijoRecurrente,
  GastoFijoRequest,
} from '../types'

export const costoService = {
  getAllCollaboradores(tenantId: string) {
    return api.get<Collaborador[]>('/core/costos/collaboradores', { params: { tenantId } })
  },
  createCollaborador(data: CollaboradorRequest) {
    return api.post<Collaborador>('/core/costos/collaboradores', data)
  },
  updateCollaborador(id: string, data: CollaboradorRequest) {
    return api.put<Collaborador>(`/core/costos/collaboradores/${id}`, data)
  },
  removeCollaborador(id: string, tenantId: string) {
    return api.delete(`/core/costos/collaboradores/${id}`, { params: { tenantId } })
  },
  getAllGastosFijos(tenantId: string) {
    return api.get<GastoFijoRecurrente[]>('/core/costos/gastos-fijos', { params: { tenantId } })
  },
  createGastoFijo(data: GastoFijoRequest) {
    return api.post<GastoFijoRecurrente>('/core/costos/gastos-fijos', data)
  },
  updateGastoFijo(id: string, data: GastoFijoRequest) {
    return api.put<GastoFijoRecurrente>(`/core/costos/gastos-fijos/${id}`, data)
  },
  removeGastoFijo(id: string, tenantId: string) {
    return api.delete(`/core/costos/gastos-fijos/${id}`, { params: { tenantId } })
  },
  getConfiguracion(tenantId: string) {
    return api.get<ConfigLaboral>('/core/costos/configuracion', { params: { tenantId } })
  },
  updateConfiguracion(tenantId: string, data: ConfigLaboralRequest) {
    return api.put<ConfigLaboral>('/core/costos/configuracion', data, { params: { tenantId } })
  },
  getDiario(tenantId: string) {
    return api.get<CostoDiario>('/core/costos/diario', { params: { tenantId } })
  },
}
