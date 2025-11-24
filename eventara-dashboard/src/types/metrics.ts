// Base timestamp type
export type Timestamp = string; // ISO 8601 format from backend

// ===== SUMMARY METRICS =====
export interface SummaryMetrics {
  totalEvents: number;
  uniqueSources: number;
  uniqueEventTypes: number;
  uniqueUsers: number;
  systemHealth: 'healthy' | 'degraded' | 'critical';
  lastUpdated: Timestamp;
}

// ===== THROUGHPUT METRICS =====
export interface CurrentThroughput {
  perSecond: number;
  perMinute: number;
  perHour: number;
  perDay: number;
}

export interface PeakThroughput {
  value: number;
  timestamp: Timestamp;
}

export interface AverageThroughput {
  last1Hour: number;
  last24Hours: number;
}

export interface ThroughputMetrics {
  current: CurrentThroughput;
  peak: PeakThroughput;
  average: AverageThroughput;
}

// ===== TIME WINDOW METRICS =====
export interface TimeWindowMetrics {
  last1Minute: number;
  last5Minutes: number;
  last15Minutes: number;
  last1Hour: number;
  last24Hours: number;
}

// ===== EVENT TYPE METRICS =====
export interface EventTypeMetrics {
  count: number;
  percentage: number;
  avgLatency: number;
}

// ===== SOURCE METRICS =====
export interface SourceMetrics {
  count: number;
  health: 'healthy' | 'degraded' | 'down';
  avgLatency: number;
  errorCount: number;
  errorRate: number;
}

// ===== USER METRICS =====
export interface UserActivity {
  userId: string;
  eventCount: number;
}

export interface UserMetrics {
  totalUniqueUsers: number;
  activeUsersLast1Hour: number;
  activeUsersLast24Hours: number;
  topActiveUsers: UserActivity[];
}

// ===== TOP EVENTS METRICS =====
export interface EventRanking {
  type: string;
  count: number;
  avgLatency: number;
}

export interface TopEventsMetrics {
  mostFrequent: EventRanking[];
  fastest: EventRanking[];
  slowest: EventRanking[];
}

// ===== ERROR ANALYSIS =====
export interface ErrorBreakdown {
  name: string;
  count: number;
  percentage: number;
}

export interface ErrorAnalysisMetrics {
  totalErrors: number;
  errorRate: number;
  errorsByType: ErrorBreakdown[];
  errorsBySource: ErrorBreakdown[];
}

// ===== PERFORMANCE METRICS =====
export interface PerformanceMetrics {
  avgLatency: number;
  p50: number;
  p95: number;
  p99: number;
  maxLatency: number;
  minLatency: number;
}

// ===== ANOMALY ALERTS =====
export interface AnomalyAlert {
  severity: 'info' | 'warning' | 'critical';
  type: 'spike' | 'drop' | 'high_error_rate' | 'high_latency' | 'traffic_spike';
  message: string;
  threshold: number;
  currentValue: number;
  detectedAt: Timestamp;
}

// ===== MAIN COMPREHENSIVE METRICS =====
export interface ComprehensiveMetrics {
  summary: SummaryMetrics;
  throughput: ThroughputMetrics;
  timeWindows: TimeWindowMetrics;
  eventsByType: Record<string, EventTypeMetrics>;
  eventsBySource: Record<string, SourceMetrics>;
  eventsBySeverity: Record<string, number>;
  userMetrics: UserMetrics;
  topEvents: TopEventsMetrics;
  errorAnalysis: ErrorAnalysisMetrics;
  performance: PerformanceMetrics;
  anomalies: AnomalyAlert[];
}
