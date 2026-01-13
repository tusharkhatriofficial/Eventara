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
      <div className="flex items-center justify-center h-full">
        <div className="text-center">
          <div className="w-16 h-16 rounded-2xl bg-gradient-to-br from-emerald-500 to-green-600 flex items-center justify-center mb-4 shadow-lg shadow-emerald-500/30 animate-pulse">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-white"></div>
          </div>
          <p className="text-dark-600 font-medium">Loading real-time data...</p>
        </div>
      </div>
    );
  }

  // Determine status based on error rate
  const getErrorRateStatus = (rate: number) => {
    if (rate >= 10) return 'critical';
    if (rate >= 5) return 'warning';
    return 'normal';
  };

  const sources = Object.entries(metrics.eventsBySource);

  return (
    <div className="space-y-6 animate-fade-in">
      {/* Page Title */}
      <div className="card-gradient p-8">
        <div className="flex items-center gap-4">
          <div className="w-12 h-12 rounded-2xl bg-gradient-to-br from-emerald-500 to-green-600 flex items-center justify-center shadow-lg shadow-emerald-500/30 animate-pulse">
            <svg className="w-7 h-7 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
            </svg>
          </div>
          <div>
            <h1 className="text-3xl font-bold text-gradient">Real-Time Monitoring</h1>
            <p className="text-sm text-dark-600 mt-1 flex items-center gap-2">
              <span className="w-2 h-2 bg-emerald-500 rounded-full animate-pulse"></span>
              Live system metrics updating every second
            </p>
          </div>
        </div>
      </div>

      {/* Large Live Metrics Display */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
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
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <CircularGauge
          label="Events/Second"
          value={metrics.throughput.current.perSecond}
          max={Math.max(metrics.throughput.peak.value, 10)}
          unit="eps"
          color="#3B82F6"
        />
        
        <CircularGauge
          label="Active Sources"
          value={metrics.summary.uniqueSources}
          max={20}
          unit="sources"
          color="#10B981"
        />
        
        <CircularGauge
          label="Active Users"
          value={metrics.userMetrics.activeUsersLast1Hour}
          max={Math.max(metrics.userMetrics.totalUniqueUsers, 10)}
          unit="users"
          color="#F59E0B"
        />
      </div>

      {/* Real-Time Charts */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <RealTimeLineChart
          title="Events/Second Trend"
          currentValue={metrics.throughput.current.perSecond}
          color="rgb(59, 130, 246)"
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
      <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-200">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">Time Window Metrics</h3>
        <div className="grid grid-cols-2 md:grid-cols-5 gap-4">
          <div className="text-center p-4 bg-blue-50 rounded-lg border border-blue-200">
            <p className="text-sm text-blue-600 font-medium">1 Minute</p>
            <p className="text-3xl font-bold text-blue-900 mt-2">
              {metrics.timeWindows.last1Minute}
            </p>
          </div>
          <div className="text-center p-4 bg-green-50 rounded-lg border border-green-200">
            <p className="text-sm text-green-600 font-medium">5 Minutes</p>
            <p className="text-3xl font-bold text-green-900 mt-2">
              {metrics.timeWindows.last5Minutes}
            </p>
          </div>
          <div className="text-center p-4 bg-yellow-50 rounded-lg border border-yellow-200">
            <p className="text-sm text-yellow-600 font-medium">15 Minutes</p>
            <p className="text-3xl font-bold text-yellow-900 mt-2">
              {metrics.timeWindows.last15Minutes}
            </p>
          </div>
          <div className="text-center p-4 bg-purple-50 rounded-lg border border-purple-200">
            <p className="text-sm text-purple-600 font-medium">1 Hour</p>
            <p className="text-3xl font-bold text-purple-900 mt-2">
              {metrics.timeWindows.last1Hour}
            </p>
          </div>
          <div className="text-center p-4 bg-gray-50 rounded-lg border border-gray-200">
            <p className="text-sm text-gray-600 font-medium">24 Hours</p>
            <p className="text-3xl font-bold text-gray-900 mt-2">
              {metrics.timeWindows.last24Hours}
            </p>
          </div>
        </div>
      </div>

      {/* Active Sources Status */}
      <div>
        <h3 className="text-lg font-semibold text-gray-900 mb-4">Active Sources</h3>
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
          <div className="bg-white p-12 rounded-lg shadow-sm border border-gray-200 text-center">
            <p className="text-gray-500">No active sources detected</p>
          </div>
        )}
      </div>

      {/* Active Anomalies */}
      <ActiveAnomaliesAlert anomalies={metrics.anomalies} />

      {/* Last Updated */}
      <div className="text-center">
        <div className="inline-flex items-center gap-2 px-4 py-2 bg-gray-100 rounded-full">
          <div className="w-2 h-2 bg-green-500 rounded-full animate-pulse"></div>
          <span className="text-sm text-gray-600">
            Live • Updated {new Date(metrics.summary.lastUpdated).toLocaleTimeString()}
          </span>
        </div>
      </div>
    </div>
  );
};
