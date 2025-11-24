// Loading states
export type LoadingState = 'idle' | 'loading' | 'success' | 'error';

// Chart data point
export interface ChartDataPoint {
  x: number | string;
  y: number;
  label?: string;
}

// Chart dataset
export interface ChartDataset {
  label: string;
  data: ChartDataPoint[];
  backgroundColor?: string | string[];
  borderColor?: string;
  borderWidth?: number;
}

// Color scheme
export interface ColorScheme {
  primary: string;
  secondary: string;
  success: string;
  warning: string;
  error: string;
  info: string;
}

// Health status colors
export const HEALTH_COLORS: Record<'healthy' | 'degraded' | 'critical' | 'down', string> = {
  healthy: '#10B981',   // green-500
  degraded: '#F59E0B',  // amber-500
  critical: '#EF4444',  // red-500
  down: '#DC2626'       // red-600
};

// Severity colors
export const SEVERITY_COLORS: Record<string, string> = {
  INFO: '#3B82F6',      // blue-500
  WARNING: '#F59E0B',   // amber-500
  ERROR: '#EF4444',     // red-500
  CRITICAL: '#DC2626'   // red-600
};
