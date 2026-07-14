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
  providerId: string
  providerName: string
  issueDate: string
  type: string
  metodoPago: string | null
  descuentoGlobal: number | null
  status: string
  total: number
  items: ItemFactura[]
  createdAt: string | null
}

export interface ItemFactura {
  id: string
  productoId: string
  productName: string
  cantidad: number
  precioUnitario: number
  descuento: number | null
  subtotal: number
  presentacionId: string | null
  conversionFactor: number
}

export interface ItemFacturaRequest {
  productoId: string
  presentacionId?: string | null
  cantidad: number
  precioUnitario: number
  descuento?: number | null
}

export interface FacturaRequest {
  tenantId: string
  proveedorId: string
  fecha: string
  tipo: string
  metodoPago?: string | null
  descuentoGlobal?: number | null
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
  locations: { code: string; name: string }[]
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
  category: string
  description: string
  amount: number
  expenseDate: string
  paymentMethod: string | null
  isActive: boolean
}

export interface GastoRequest {
  tenantId: string
  category: string
  description: string
  amount: number
  expenseDate: string
  paymentMethod?: string | null
}

export interface VentaDiaria {
  id: string
  tenantId: string
  saleDate: string
  grossAmount: number
  description: string | null
  isActive: boolean
}

export interface VentaRequest {
  tenantId: string
  saleDate: string
  grossAmount: number
  description?: string | null
}

export interface Prestamo {
  id: string
  tenantId: string
  name: string
  lender: string | null
  amount: number
  interestRate: number
  termMonths: number
  startDate: string
  remainingBalance: number
  status: string
  notes: string | null
  isActive: boolean
}

export interface PagoPrestamo {
  id: string
  loanId: string
  amount: number
  interestPaid: number
  principalPaid: number
  paymentDate: string
  paymentMethod: string | null
}

export interface PrestamoRequest {
  tenantId: string
  name: string
  lender?: string | null
  amount: number
  interestRate: number
  termMonths: number
  startDate: string
  notes?: string | null
}

export interface PagoPrestamoRequest {
  amount: number
  paymentDate: string
  paymentMethod?: string | null
}

export interface Patrimonio {
  tenantId: string
  initialCapital: number
  startDate: string | null
  notes: string | null
}

export interface PatrimonioRequest {
  initialCapital?: number
  startDate?: string | null
  notes?: string | null
}

export interface MetricasFinancieras {
  tenantId: string
  periodo: string
  totalIncome: number
  costOfGoods: number
  operatingExpenses: number
  loanPayments: number
  totalExpenses: number
  grossMargin: number
  grossMarginPct: number
  operatingMargin: number
  operatingMarginPct: number
  netMargin: number
  netMarginPct: number
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
