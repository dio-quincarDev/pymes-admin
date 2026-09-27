export interface AbcItem {
  productId: string;
  productName: string;
  spend: number;
  pctTotal: number;
  cumulativePct: number;
  category: 'A' | 'B' | 'C';
}

// Wire compat: backend envía totalSpend/pct (ver AnalyticsServiceImpl.java:121)
// Este tipo representa lo que realmente llega por la red antes de normalizar.
export type AbcItemWire = AbcItem & {
  totalSpend?: number | string;
  pct?: number | string;
};

export interface TrendItem {
  productId: string;
  productName: string;
  currentAvgPrice: number;
  movingAvg90d: number;
  pctChange: number;
}

export interface MarginItem {
  productId: string;
  productName: string;
  currentPrice: number;
  previousPrice: number;
  pctChange: number;
}

export interface OpexItem {
  period: string;
  totalSpend: number;
  invoiceCount: number;
  productCount: number;
  providerCount: number;
  projectedMonthly: number;
  avgDailySpend: number;
  variableDailySpend?: number;
  fixedDailyCost?: number;
}

export interface ProjectionItem {
  period: string;
  projectedSpend: number;
  confidence: number;
}

export interface AlertItem {
  productId: string;
  productName: string;
  currentPrice: number;
  avgPrice: number;
  variationPct: number;
  severity: 'warning' | 'critical';
  // Enriquecimiento UI (frontend-only, ver enrichAlert): tipo, proveedor señalado y evidencia.
  alertKind?: 'PRICE_VARIATION' | 'SUPPLIER_PREMIUM';
  providerId?: string | undefined;
  providerName?: string | undefined;
  purchaseCount?: number | undefined;
  providerCount?: number | undefined;
}

// Wire compat: backend envía cvPct/premiumPct sin variationPct ni severity (ver AnalyticsServiceImpl.java:324,364)
export type AlertItemWire = Omit<AlertItem, 'currentPrice' | 'variationPct' | 'severity' | 'alertKind' | 'purchaseCount' | 'providerCount'> & {
  currentPrice?: number | string;
  variationPct?: number | string;
  severity?: string;
  cvPct?: number | string;
  premiumPct?: number | string;
  type?: string;
};

export interface SupplierComparisonItem {
  productId: string;
  productName: string;
  providerId: string;
  providerName: string;
  purchaseCount: number;
  avgPrice: number;
  minPrice: number;
  maxPrice: number;
  priceStddev: number;
}

export interface SupplierRecommendationItem {
  productId: string;
  productName: string;
  recommendedProviderId: string;
  recommendedProviderName: string;
  recommendedPrice: number;
  currentAvgPrice: number;
  savingsPerUnit: number;
  savingsPct: number;
  supplierCount: number;
  // Enriquecimiento UI (frontend-only, ver enrichRecommendation): contra quién y en qué unidad.
  comparedProviderName?: string | undefined;
  unitLabel?: string | undefined;
}

export interface PricePredictionItem {
  productId: string;
  productName: string;
  lastPrice: number;
  predictedPrice: number;
  pctChange: number;
  confidence: number;
  dataPoints: number;
}

export interface FinancialHealthBreakdown {
  score: number;
  drivers: string[];
}

export interface FinancialHealthAlert {
  code: string;
  title: string;
  description: string;
  current: number;
  threshold: number;
  action: string;
}

// Wire: backend manda type/message/metric (AnalyticsServiceImpl.java:830)
export type FinancialHealthAlertWire = Omit<FinancialHealthAlert, 'code' | 'description' | 'current'> & {
  type?: string;
  code?: string;
  message?: string;
  description?: string;
  metric?: number | string;
  current?: number | string;
  severity?: string;
};

export type FinancialHealthWire = Omit<FinancialHealth, 'criticalAlerts' | 'breakdown'> & {
  criticalAlerts: FinancialHealthAlertWire[];
  breakdown: Record<string, FinancialHealthBreakdown>;
};

export interface FinancialHealthExpansionRequirement {
  met: boolean;
  label: string;
  current: string;
}

export interface FinancialHealthExpansion {
  score: number;
  status: string;
  requirements: FinancialHealthExpansionRequirement[];
}

export interface FinancialHealth {
  overallHealth: number;
  breakdown: Record<string, FinancialHealthBreakdown>;
  criticalAlerts: FinancialHealthAlert[];
  investmentSignals: Record<string, unknown>[];
  expansionReadiness: FinancialHealthExpansion;
  recommendations: string[];
}

export interface AnalyticsResponse {
  id: string;
  tenantId: string;
  period: string;
  abc: AbcItem[];
  trend: TrendItem[];
  margin: MarginItem[];
  opexPct: OpexItem[];
  projection: ProjectionItem[];
  alerts: AlertItem[];
  supplierComparison: SupplierComparisonItem[];
  supplierRecommendations: SupplierRecommendationItem[];
  pricePrediction: PricePredictionItem[];
  financialHealth?: FinancialHealth;
}

// Respuesta cruda del backend antes de normalizar (usa AbcItemWire + FinancialHealthWire + AlertItemWire)
export type AnalyticsResponseWire = Omit<AnalyticsResponse, 'abc' | 'alerts' | 'financialHealth'> & {
  abc: AbcItemWire[];
  alerts: AlertItemWire[];
  financialHealth?: FinancialHealthWire | FinancialHealth;
};
