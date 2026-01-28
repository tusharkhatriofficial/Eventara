import { ComprehensiveMetrics } from '../types';
import { LiveMetricDisplay } from '../components/cards/LiveMetricDisplay';
import { CircularGauge } from '../components/charts/CircularGauge';
import { SourceStatusCard } from '../components/cards/SourceStatusCard';
import { RealTimeLineChart } from '../components/charts/RealTimeLineChart';
import { ActiveAnomaliesAlert } from '../components/alerts/ActiveAnomaliesAlert';

interface RealTimeMonitoringProps {
  metrics: ComprehensiveMetrics | null;
}

export const RealTimeMonitoring: React.FC<RealTimeMonitoringProps> = ({ metrics }) => {
  if (!metrics) {
    return (
      <div className="flex items-center justify-center h-[60vh]">
        <div className="text-center">
          <div className="w-16 h-16 rounded-2xl bg-gradient-to-br from-success-500 to-success-600 flex items-center justify-center mb-4 mx-auto shadow-lg shadow-success-500/25">
            <div className="loading-spinner border-white/20 border-t-white"></div>
          </div>
          <p className="text-slate-600 font-medium">Loading real-time data...</p>
          <p className="text-sm text-slate-400 mt-1">Establishing connection</p>
        </div>
      </div>
    );
  }

  // Determine status based on error rate
  const getErrorRateStatus = (rate: number): 'normal' | 'warning' | 'critical' => {
    if (rate >= 10) return 'critical';
    if (rate >= 5) return 'warning';
    return 'normal';
  };

  const sources = Object.entries(metrics.eventsBySource);

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="page-header">
        <div className="flex flex-col sm:flex-row sm:items-center gap-4">
          <div className="w-12 h-12 rounded-2xl bg-gradient-to-br from-success-500 to-success-600 flex items-center justify-center shadow-lg shadow-success-500/25 animate-pulse">
            <svg className="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
            </svg>
          </div>
          <div>
            <h1 className="page-title">Real-Time Monitoring</h1>
            <p className="page-subtitle flex items-center gap-2">
              <span className="w-2 h-2 bg-success-500 rounded-full animate-pulse"></span>
              Live system metrics updating every second
            </p>
          </div>
        </div>
      </div>

      {/* Large Live Metrics Display */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4 lg:gap-6">
        <LiveMetricDisplay
          label="Current Throughput"
          value={metrics.throughput.current.perSecond.toFixed(2)}
          unit="events/sec"
          status="normal"
          subtitle={`Peak: ${metrics.throughput.peak.value.toFixed(2)} events/sec`}
        />

        <LiveMetricDisplay
          label="Last Minute"
          value={metrics.timeWindows.last1Minute}
          unit="events"
          status="normal"
        />

        <LiveMetricDisplay
          label="Error Rate"
          value={metrics.errorAnalysis.errorRate.toFixed(2)}
          unit="%"
          status={getErrorRateStatus(metrics.errorAnalysis.errorRate)}
          subtitle={`${metrics.errorAnalysis.totalErrors} total errors`}
        />
      </div>

      {/* Circular Gauges */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4 lg:gap-6">
        <CircularGauge
          label="Events/Second"
          value={metrics.throughput.current.perSecond}
          max={Math.max(metrics.throughput.peak.value, 10)}
          unit="eps"
          color="#6366f1"
        />

        <CircularGauge
          label="Active Sources"
          value={metrics.summary.uniqueSources}
          max={20}
          unit="sources"
          color="#10b981"
        />

        <CircularGauge
          label="Active Users"
          value={metrics.userMetrics.activeUsersLast1Hour}
          max={Math.max(metrics.userMetrics.totalUniqueUsers, 10)}
          unit="users"
          color="#f59e0b"
        />
      </div>

      {/* Real-Time Charts */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <RealTimeLineChart
          title="Events/Second Trend"
          currentValue={metrics.throughput.current.perSecond}
          color="rgb(99, 102, 241)"
          maxDataPoints={60}
        />

        <RealTimeLineChart
          title="Events (Last Minute)"
          currentValue={metrics.timeWindows.last1Minute}
          color="rgb(16, 185, 129)"
          maxDataPoints={60}
        />
      </div>

      {/* Time Windows Grid */}
      <div className="card p-6">
        <h3 className="text-lg font-semibold text-slate-900 mb-4 flex items-center gap-2">
          <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-primary-500 to-primary-600 flex items-center justify-center text-white shadow-lg shadow-primary-500/25">
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          </div>
          Time Window Metrics
        </h3>
        <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-4">
          <div className="text-center p-4 bg-primary-50 rounded-xl border border-primary-100">
            <p className="text-sm text-primary-600 font-medium">1 Minute</p>
            <p className="text-2xl font-bold text-primary-900 mt-2">
              {metrics.timeWindows.last1Minute.toLocaleString()}
            </p>
          </div>
          <div className="text-center p-4 bg-success-50 rounded-xl border border-success-100">
            <p className="text-sm text-success-600 font-medium">5 Minutes</p>
            <p className="text-2xl font-bold text-success-900 mt-2">
              {metrics.timeWindows.last5Minutes.toLocaleString()}
            </p>
          </div>
          <div className="text-center p-4 bg-warning-50 rounded-xl border border-warning-100">
            <p className="text-sm text-warning-600 font-medium">15 Minutes</p>
            <p className="text-2xl font-bold text-warning-900 mt-2">
              {metrics.timeWindows.last15Minutes.toLocaleString()}
            </p>
          </div>
          <div className="text-center p-4 bg-violet-50 rounded-xl border border-violet-100">
            <p className="text-sm text-violet-600 font-medium">1 Hour</p>
            <p className="text-2xl font-bold text-violet-900 mt-2">
              {metrics.timeWindows.last1Hour.toLocaleString()}
            </p>
          </div>
          <div className="text-center p-4 bg-slate-50 rounded-xl border border-slate-200 col-span-2 sm:col-span-1">
            <p className="text-sm text-slate-600 font-medium">24 Hours</p>
            <p className="text-2xl font-bold text-slate-900 mt-2">
              {metrics.timeWindows.last24Hours.toLocaleString()}
            </p>
          </div>
        </div>
      </div>

      {/* Active Sources Status */}
      <div className="space-y-4">
        <h3 className="text-lg font-semibold text-slate-900 flex items-center gap-2">
          <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-cyan-500 to-cyan-600 flex items-center justify-center text-white shadow-lg shadow-cyan-500/25">
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 12h14M5 12a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v4a2 2 0 01-2 2M5 12a2 2 0 00-2 2v4a2 2 0 002 2h14a2 2 0 002-2v-4a2 2 0 00-2-2m-2-4h.01M17 16h.01" />
            </svg>
          </div>
          Active Sources
        </h3>
        {sources.length > 0 ? (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {sources.map(([sourceName, sourceMetrics]) => (
              <SourceStatusCard
                key={sourceName}
                sourceName={sourceName}
                metrics={sourceMetrics}
              />
            ))}
          </div>
        ) : (
          <div className="card p-12 text-center">
            <div className="empty-state-icon mx-auto">
              <svg className="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M5 12h14M5 12a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v4a2 2 0 01-2 2M5 12a2 2 0 00-2 2v4a2 2 0 002 2h14a2 2 0 002-2v-4a2 2 0 00-2-2m-2-4h.01M17 16h.01" />
              </svg>
            </div>
            <p className="text-slate-500 mt-4">No active sources detected</p>
          </div>
        )}
      </div>

      {/* Active Anomalies */}
      <ActiveAnomaliesAlert anomalies={metrics.anomalies} />

      {/* Last Updated */}
      <div className="flex justify-center">
        <div className="inline-flex items-center gap-2 px-4 py-2 bg-white rounded-full border border-slate-100 shadow-sm">
          <div className="w-2 h-2 bg-success-500 rounded-full animate-pulse"></div>
          <span className="text-sm text-slate-500">
            Live • Updated {new Date(metrics.summary.lastUpdated).toLocaleTimeString()}
          </span>
        </div>
      </div>
    </div>
  );
};
