export interface AbcItem {
  productId: string;
  productName: string;
  spend: number;
  pctTotal: number;
  cumulativePct: number;
  category: 'A' | 'B' | 'C';
}

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
}

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
