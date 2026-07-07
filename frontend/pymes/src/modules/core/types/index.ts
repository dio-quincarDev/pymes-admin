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
}

export interface PresentacionRequest {
  name: string
  conversion: number
}

export interface Proveedor {
  id: string
  tenantId: string
  name: string
  ruc: string | null
  isActive: boolean
}

export interface ProveedorRequest {
  tenantId: string
  name: string
  ruc?: string | null
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
