import { ComprehensiveMetrics } from '../types';
import { AlertOverviewCards } from '../components/cards/AlertOverviewCards';
import { ActiveAlertsPanel } from '../components/alerts/ActiveAlertsPanel';
import { AlertSeverityChart, AlertTypeChart } from '../components/charts/AlertStatisticsCharts';

interface AlertsAndAnomaliesProps {
  metrics: ComprehensiveMetrics | null;
}

export const AlertsAndAnomalies: React.FC<AlertsAndAnomaliesProps> = ({ metrics }) => {
  if (!metrics) {
    return (
      <div className="flex items-center justify-center h-full">
        <div className="text-center">
          <div className="w-16 h-16 rounded-2xl bg-gradient-to-br from-red-500 to-rose-600 flex items-center justify-center mb-4 shadow-lg shadow-red-500/30 animate-pulse">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-white"></div>
          </div>
          <p className="text-dark-600 font-medium">Loading alerts...</p>
        </div>
      </div>
    );
  }

  const hasAlerts = metrics.anomalies.length > 0;

  return (
    <div className="space-y-6 animate-fade-in">
      {/* Page Title */}
      <div className="card-gradient p-8">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-4">
            <div className="w-12 h-12 rounded-2xl bg-gradient-to-br from-red-500 to-rose-600 flex items-center justify-center shadow-lg shadow-red-500/30">
              <svg className="w-7 h-7 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
              </svg>
            </div>
            <div>
              <h1 className="text-3xl font-bold text-gradient">Alerts & Anomalies</h1>
              <p className="text-sm text-dark-600 mt-1">Real-time anomaly detection and system alert monitoring</p>
            </div>
          </div>
          <div className="hidden sm:flex items-center gap-2">
            <a href="/alerts/rules" className="btn-primary">Rules</a>
            <a href="/settings/notifications" className="btn-ghost">Notifications</a>
          </div>
        </div>
      </div>

      {/* Overview Cards */}
      <AlertOverviewCards anomalies={metrics.anomalies} />

      {/* Custom Alert Rules Card */}
      <div className="card-gradient p-8 hover:shadow-xl transition-all duration-300">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-4">
            <div className="w-14 h-14 rounded-2xl bg-gradient-to-br from-purple-500 to-purple-700 flex items-center justify-center shadow-lg shadow-purple-500/30">
              <svg className="w-7 h-7 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
              </svg>
            </div>
            <div>
              <h3 className="text-xl font-bold text-dark-900">Custom Alert Rules</h3>
              <p className="text-sm text-dark-600 mt-1">Create rules to automatically detect anomalies and notify your team</p>
            </div>
          </div>
          <div className="flex items-center gap-3">
            <a href="/alerts/rules" className="btn-primary">Manage Rules</a>
            <a href="/alerts/rules/new" className="btn-secondary">Create Rule</a>
          </div>
        </div>
      </div>

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
        <div className="card-gradient p-12 text-center animate-scale-in">
          <div className="w-24 h-24 rounded-3xl bg-gradient-to-br from-emerald-500 to-green-600 flex items-center justify-center mx-auto mb-6 shadow-2xl shadow-emerald-500/40 animate-pulse">
            <svg className="w-14 h-14 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          </div>
          <h3 className="text-3xl font-bold text-gradient mb-3">All Systems Normal</h3>
          <p className="text-dark-600 mb-6 text-lg">
            No anomalies or alerts detected at this time
          </p>
          <div className="inline-flex items-center gap-3 px-6 py-3 bg-gradient-to-r from-emerald-50 to-green-50 rounded-2xl border border-emerald-200">
            <div className="w-3 h-3 bg-emerald-500 rounded-full animate-pulse shadow-lg shadow-emerald-500/50"></div>
            <span className="text-sm font-semibold text-emerald-800">System Monitoring Active</span>
          </div>
        </div>
      )}

      {/* How It Works */}
      <div className="card-gradient p-8">
        <h3 className="text-2xl font-bold text-dark-900 mb-6 flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-primary-500 to-primary-700 flex items-center justify-center">
            <svg className="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          </div>
          How Anomaly Detection Works
        </h3>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          <div className="flex items-start gap-4 p-5 bg-white/50 rounded-2xl hover:shadow-lg transition-all duration-300">
            <div className="flex-shrink-0 w-12 h-12 bg-gradient-to-br from-primary-500 to-primary-700 rounded-xl flex items-center justify-center shadow-lg shadow-primary-500/30">
              <span className="text-white font-bold text-lg">1</span>
            </div>
            <div>
              <h4 className="font-semibold text-dark-900 mb-2">Real-Time Analysis</h4>
              <p className="text-sm text-dark-600">
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
