import { ComprehensiveMetrics } from '../types';
import { HeroMetricCard } from '../components/cards/HeroMetricCard';
import { SystemHealthBadge } from '../components/cards/SystemHealthBadge';
import { QuickStatsGrid } from '../components/cards/QuickStatsGrid';
import { RecentAlertsPanel } from '../components/alerts/RecentAlertsPanel';
import { EventsOverTimeChart } from '../components/charts/EventsOverTimeChart';
import { ThroughputChart } from '../components/charts/ThroughputChart';
import { EventsByTypeChart } from '../components/charts/EventsByTypeChart';

interface OverviewProps {
  metrics: ComprehensiveMetrics | null;
}

export const Overview: React.FC<OverviewProps> = ({ metrics }) => {
  if (!metrics) {
    return (
      <div className="flex items-center justify-center h-[60vh]">
        <div className="text-center">
          <div className="w-16 h-16 rounded-2xl bg-gradient-to-br from-primary-500 to-primary-600 flex items-center justify-center mb-4 mx-auto shadow-lg shadow-primary-500/25">
            <div className="loading-spinner border-white/20 border-t-white"></div>
          </div>
          <p className="text-slate-600 font-medium">Loading metrics...</p>
          <p className="text-sm text-slate-400 mt-1">Connecting to data stream</p>
        </div>
      </div>
    );
  }

  const quickStats = [
    { label: 'Unique Sources', value: metrics.summary.uniqueSources },
    { label: 'Event Types', value: metrics.summary.uniqueEventTypes },
    { label: 'Active Users', value: metrics.summary.uniqueUsers },
    { label: 'Total Errors', value: metrics.errorAnalysis.totalErrors }
  ];

  // Get top 5 event types
  const topEventTypes = Object.entries(metrics.eventsByType)
    .sort(([, a], [, b]) => b.count - a.count)
    .slice(0, 5);

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="page-header">
        <div className="flex flex-col sm:flex-row sm:items-center gap-4">
          <div className="page-icon">
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" />
            </svg>
          </div>
          <div className="flex-1">
            <h1 className="page-title">Dashboard Overview</h1>
            <p className="page-subtitle">Real-time system health and key performance metrics</p>
          </div>
          <SystemHealthBadge health={metrics.summary.systemHealth} />
        </div>
      </div>

      {/* Hero Metrics - 4 Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 lg:gap-6">
        <HeroMetricCard
          title="Total Events"
          value={metrics.summary.totalEvents}
          subtitle="All time"
          accentColor="primary"
          icon={
            <svg className="w-full h-full" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
            </svg>
          }
        />

        <HeroMetricCard
          title="Current Throughput"
          value={metrics.throughput.current.perSecond.toFixed(2)}
          subtitle="events/second"
          accentColor="success"
          icon={
            <svg className="w-full h-full" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
            </svg>
          }
        />

        <HeroMetricCard
          title="Last Minute"
          value={metrics.timeWindows.last1Minute}
          subtitle="events processed"
          accentColor="info"
          icon={
            <svg className="w-full h-full" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          }
        />

        <HeroMetricCard
          title="Error Rate"
          value={metrics.errorAnalysis.errorRate.toFixed(2) + '%'}
          subtitle={`${metrics.errorAnalysis.totalErrors} total errors`}
          accentColor={metrics.errorAnalysis.errorRate > 5 ? 'error' : metrics.errorAnalysis.errorRate > 2 ? 'warning' : 'success'}
          icon={
            <svg className="w-full h-full" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
            </svg>
          }
        />
      </div>

      {/* Charts Row 1 */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <EventsOverTimeChart timeWindows={metrics.timeWindows} />
        <ThroughputChart throughput={metrics.throughput} />
      </div>

      {/* Quick Stats & Alerts */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <QuickStatsGrid stats={quickStats} />
        <RecentAlertsPanel alerts={metrics.anomalies} />
      </div>

      {/* Top Event Types Chart */}
      <EventsByTypeChart eventsByType={metrics.eventsByType} />

      {/* Top Event Types Table */}
      <div className="card overflow-hidden">
        <div className="p-6 border-b border-slate-100">
          <h3 className="text-lg font-semibold text-slate-900 flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-cyan-500 to-blue-600 flex items-center justify-center shadow-lg shadow-cyan-500/25">
              <svg className="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
              </svg>
            </div>
            Top Event Types
          </h3>
        </div>
        <div className="overflow-x-auto">
          <table className="table-modern">
            <thead>
              <tr>
                <th>Event Type</th>
                <th>Count</th>
                <th>Percentage</th>
                <th>Avg Latency</th>
              </tr>
            </thead>
            <tbody>
              {topEventTypes.map(([type, data]) => (
                <tr key={type}>
                  <td className="font-semibold text-slate-900">{type}</td>
                  <td>{data.count.toLocaleString()}</td>
                  <td>
                    <span className="badge badge-info">{data.percentage.toFixed(2)}%</span>
                  </td>
                  <td>{data.avgLatency.toFixed(2)} ms</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Last Updated */}
      <div className="flex justify-center">
        <div className="inline-flex items-center gap-2 px-4 py-2 bg-white rounded-full border border-slate-100 shadow-sm">
          <svg className="w-4 h-4 text-slate-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          <span className="text-sm text-slate-500">Last updated: {new Date(metrics.summary.lastUpdated).toLocaleString()}</span>
        </div>
      </div>
    </div>
  );
};
