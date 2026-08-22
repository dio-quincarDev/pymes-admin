export interface Producto {
  id: string
  tenantId: string
  name: string
  sku: string
  category: string
  baseUnit: string
  imageUrl: string | null
  isActive: boolean
  createdAt: string | null
  updatedAt: string | null
  presentaciones: Presentacion[]
  lastUnitPrice: number | null
  totalInvestment: number | null
  lastPurchaseDate: string | null
  minQuantity: number | null
  maxQuantity: number | null
  proveedorId: string | null
  proveedorName: string | null
}

export interface Presentacion {
  id: string
  name: string
  conversion: number
  productId: string
}

export interface ProductoRequest {
  tenantId: string
  name: string
  sku?: string
  category?: string
  baseUnit?: string
  minQuantity?: number | null
  maxQuantity?: number | null
  proveedorId?: string | null
}

export interface PresentacionRequest {
  name: string
  conversion: number
}

export interface Proveedor {
  id: string
  tenantId: string
  name: string
  contactName: string | null
  contactPhone: string | null
  contactEmail: string | null
  isActive: boolean
}

export interface ProveedorRequest {
  tenantId: string
  name: string
  contactName?: string | null
  contactPhone?: string | null
  contactEmail?: string | null
}

export interface Factura {
  id: string
  tenantId: string
  invoiceNumber: string
  providerId: string | null
  providerName: string | null
  colaboradorId: string | null
  collaboradorName: string | null
  issueDate: string
  type: string
  paymentMethod: string | null
  category: string | null
  globalDiscount: number | null
  status: string
  total: number
  items: ItemFactura[]
  createdAt: string | null
}

export interface ItemFactura {
  id: string
  productId: string
  productName: string
  quantity: number
  unitPrice: number
  discount: number | null
  subtotal: number
  presentacionId: string | null
  conversionFactor: number
  cantidadPresentacion?: number
  valorPresentacion?: number
  precioUnitarioInput?: number
  descuentoInput?: number
  descuentoEsPorcentaje?: boolean
}

export interface ItemFacturaRequest {
  productoId: string
  presentacionId?: string | null
  cantidad?: number
  precioUnitario?: number
  descuento?: number | null
  cantidadPresentacion?: number
  valorPresentacion?: number
  precioUnitarioInput?: number
  descuentoInput?: number
  descuentoEsPorcentaje?: boolean
}

export interface FacturaRequest {
  tenantId: string
  proveedorId: string | null
  colaboradorId?: string | null
  fecha: string
  tipo: string
  metodoPago?: string | null
  category?: string | null
  descuentoGlobal?: number | null
  total?: number | null
  items: ItemFacturaRequest[]
}

export interface ProductOption {
  label: string
  value: string
  productName: string
  sku: string
  category: string
  categoryName: string
  proveedorId: string | null
  proveedorName: string | null
  lastUnitPrice: number | null
}

export interface ProductTemplateDTO {
  id: string
  name: string
  baseUnit: string
  categoryName: string
}

export interface SetupInfo {
  onboardingCompleted: boolean
  industry: string | null
  categories: SetupCategory[]
  units: { code: string; name: string }[]
  products: ProductTemplateDTO[]
}

export interface SetupCategory {
  code: string
  name: string
  parentId: string | null
  children: SetupCategory[]
}

export interface GastoOperativo {
  id: string
  tenantId: string
  categoria: string
  descripcion: string
  monto: number
  fecha: string
  metodoPago: string | null
  isActive: boolean
}

export interface GastoRequest {
  tenantId: string
  categoria: string
  descripcion: string
  monto: number
  fecha: string
  metodoPago?: string | null
}

export interface VentaDiaria {
  id: string
  tenantId: string
  fecha: string
  montoBruto: number
  descripcion: string | null
  isActive: boolean
}

export interface VentaRequest {
  tenantId: string
  fecha: string
  montoBruto: number
  descripcion?: string | null
}

export interface Prestamo {
  id: string
  tenantId: string
  nombre: string
  prestamista: string | null
  monto: number
  tasaInteres: number
  plazoMeses: number
  fechaInicio: string
  saldoPendiente: number
  estado: string
  notas: string | null
  isActive: boolean
}

export interface PagoPrestamo {
  id: string
  prestamoId: string
  monto: number
  interesPagado: number
  capitalPagado: number
  fechaPago: string
  metodoPago: string | null
}

export interface PrestamoRequest {
  tenantId: string
  nombre: string
  prestamista?: string | null
  monto: number
  tasaInteres: number
  plazoMeses: number
  fechaInicio: string
  notas?: string | null
}

export interface PagoPrestamoRequest {
  monto: number
  fechaPago: string
  metodoPago?: string | null
}

export interface Patrimonio {
  tenantId: string
  capitalInicial: number
  fechaInicio: string | null
  notas: string | null
  createdAt: string
}

export interface PatrimonioRequest {
  tenantId: string
  capitalInicial: number
  fechaInicio: string | null
}

export interface MetricasFinancieras {
  tenantId: string
  periodo: string
  totalIngresos: number
  costoMercaderia: number
  gastosOperativos: number
  pagosPrestamos: number
  totalGastos: number
  margenBruto: number
  margenBrutoPct: number
  margenOperativo: number
  margenOperativoPct: number
  margenNeto: number
  margenNetoPct: number
  costoOperativoDiario: number | null
}

export interface Collaborador {
  id: string
  tenantId: string
  nombre: string
  tipoPago: string
  monto: number
  activo: boolean
  createdAt: string | null
}

export interface CollaboradorRequest {
  tenantId: string
  nombre: string
  tipoPago: string
  monto: number
}

export interface GastoFijoRecurrente {
  id: string
  tenantId: string
  categoria: string
  monto: number
  descripcion: string | null
  diaEjecucion: number
  metodoPago: string | null
  proveedorId: string | null
  proveedorName: string | null
  activo: boolean
}

export interface GastoFijoRequest {
  tenantId: string
  categoria: string
  monto: number
  descripcion?: string | null
  diaEjecucion: number
  metodoPago?: string | null
  proveedorId?: string | null
}

export interface ConfigLaboral {
  tenantId: string
  diasLaborales: number
}

export interface ConfigLaboralRequest {
  diasLaborales: number
}

export interface CostoDiario {
  costoFijoMensual: number
  costoSemiFijoMensual: number
  costoSalariosMensual: number
  costoOperativoMensual: number
  diasLaborales: number
  costoOperativoDiario: number
  ventasHoy: number
  gananciaRealEstimada: number
}

export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
  first: boolean
  last: boolean
}
