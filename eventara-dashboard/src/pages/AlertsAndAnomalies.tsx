import { ComprehensiveMetrics } from '../types';
import { AlertOverviewCards } from '../components/cards/AlertOverviewCards';
import { ActiveAlertsPanel } from '../components/alerts/ActiveAlertsPanel';
import { AlertSeverityChart, AlertTypeChart } from '../components/charts/AlertStatisticsCharts';
import { ComingSoonBanner } from '../components/alerts/ComingSoonBanner';

interface AlertsAndAnomaliesProps {
  metrics: ComprehensiveMetrics | null;
}

export const AlertsAndAnomalies: React.FC<AlertsAndAnomaliesProps> = ({ metrics }) => {
  if (!metrics) {
    return (
      <div className="flex items-center justify-center h-full">
        <div className="text-center">
          <div className="inline-block animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mb-4"></div>
          <p className="text-gray-600">Loading alerts...</p>
        </div>
      </div>
    );
  }

  const hasAlerts = metrics.anomalies.length > 0;

  return (
    <div className="space-y-6">
      {/* Page Title */}
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Alerts & Anomalies</h1>
        <p className="text-sm text-gray-500 mt-1">
          Real-time anomaly detection and system alert monitoring
        </p>
      </div>

      {/* Overview Cards */}
      <AlertOverviewCards anomalies={metrics.anomalies} />

      {/* Coming Soon Banner */}
      <ComingSoonBanner />

      {/* Active Alerts Panel */}
      <ActiveAlertsPanel anomalies={metrics.anomalies} />

      {/* Statistics Charts */}
      {hasAlerts && (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <AlertSeverityChart anomalies={metrics.anomalies} />
          <AlertTypeChart anomalies={metrics.anomalies} />
        </div>
      )}

      {/* System Status */}
      {!hasAlerts && (
        <div className="bg-green-50 border border-green-200 rounded-lg p-12 text-center">
          <svg className="w-20 h-20 text-green-400 mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          <h3 className="text-2xl font-bold text-green-900 mb-2">All Systems Normal</h3>
          <p className="text-green-700 mb-4">
            No anomalies or alerts detected at this time
          </p>
          <div className="inline-flex items-center gap-2 px-4 py-2 bg-green-100 rounded-lg">
            <div className="w-2 h-2 bg-green-500 rounded-full animate-pulse"></div>
            <span className="text-sm font-medium text-green-800">System monitoring active</span>
          </div>
        </div>
      )}

      {/* How It Works */}
      <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-200">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">How Anomaly Detection Works</h3>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          <div className="flex items-start gap-3">
            <div className="flex-shrink-0 w-10 h-10 bg-blue-100 rounded-lg flex items-center justify-center">
              <span className="text-blue-600 font-bold">1</span>
            </div>
            <div>
              <h4 className="font-semibold text-gray-900 text-sm mb-1">Real-Time Analysis</h4>
              <p className="text-xs text-gray-600">
                System continuously monitors metrics and compares them against baseline patterns
              </p>
            </div>
          </div>
          
          <div className="flex items-start gap-3">
            <div className="flex-shrink-0 w-10 h-10 bg-purple-100 rounded-lg flex items-center justify-center">
              <span className="text-purple-600 font-bold">2</span>
            </div>
            <div>
              <h4 className="font-semibold text-gray-900 text-sm mb-1">Automatic Detection</h4>
              <p className="text-xs text-gray-600">
                Identifies spikes, drops, high error rates, and latency issues automatically
              </p>
            </div>
          </div>

          <div className="flex items-start gap-3">
            <div className="flex-shrink-0 w-10 h-10 bg-green-100 rounded-lg flex items-center justify-center">
              <span className="text-green-600 font-bold">3</span>
            </div>
            <div>
              <h4 className="font-semibold text-gray-900 text-sm mb-1">Instant Alerts</h4>
              <p className="text-xs text-gray-600">
                Generates alerts with severity levels to help prioritize response
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
