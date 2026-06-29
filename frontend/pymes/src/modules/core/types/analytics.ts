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
}
