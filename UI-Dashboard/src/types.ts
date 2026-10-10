export interface DashboardSummary {
  facilitiesMonitored: number;
  facilitiesAtRisk: number;
  batchesWithExpiryRisk: number;
  pendingRedistributionRecommendations: number;
  generatedAt: string;
}

export interface ExpiryAlert {
  batchId: number;
  batchNumber: string;
  batchQuantity: number;
  expiryDate: string;
  daysUntilExpiry: number;
  estimatedQuantityAtRisk: number;
  explanation: string;
}

export interface RedistributionRecommendation {
  donorFacilityId: number;
  donorFacilityName: string;
  donorCurrentStock: number;
  donorForecastDailyDemand: number;
  donorSafetyStockTarget: number;
  donorTransferableQuantity: number;
  recommendedTransferQuantity: number;
  estimatedDistanceKm: number;
  receiverProjectedCoverageDaysAfterTransfer: number;
  explanation: string;
}

export interface FacilityAnalysis {
  facilityId: number;
  facilityName: string;
  district: string;
  medicineId: number;
  medicineName: string;
  unitOfMeasurement: string;
  usableStock: number;
  forecastDailyDemand: number;
  sevenDayAvgDemand: number;
  thirtyDayAvgDemand: number;
  daysToStockOut: number;
  riskLevel: 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW';
  expiryAlerts: ExpiryAlert[];
  redistribution: RedistributionRecommendation | null;
  analysisExplanation: string;
}

export interface Medicine {
  id: number;
  name: string;
  unitOfMeasurement: string;
  essentialMedicine: boolean;
}
