import { ComprehensiveMetrics } from '../types';
import { EventTypeStatsCard } from '../components/cards/EventTypeStatsCard';
import { EventTypeTable } from '../components/tables/EventTypeTable';
import { EventDistributionChart } from '../components/charts/EventDistributionChart';
import { EventsByTypeChart } from '../components/charts/EventsByTypeChart';
import { LatencyComparisonChart } from '../components/charts/LatencyComparisonChart';


interface EventAnalyticsProps {
  metrics: ComprehensiveMetrics | null;
}

export const EventAnalytics: React.FC<EventAnalyticsProps> = ({ metrics }) => {
  if (!metrics) {
    return (
      <div className="flex items-center justify-center h-full">
        <div className="text-center">
          <div className="w-16 h-16 rounded-2xl bg-gradient-to-br from-cyan-500 to-blue-600 flex items-center justify-center mb-4 shadow-lg shadow-cyan-500/30 animate-pulse">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-white"></div>
          </div>
          <p className="text-dark-600 font-medium">Loading event analytics...</p>
        </div>
      </div>
    );
  }

  const eventTypes = Object.entries(metrics.eventsByType);
  const totalEventTypes = eventTypes.length;

  // Calculate stats
  const mostFrequentEvent = eventTypes.length > 0
    ? eventTypes.reduce((max, curr) => (curr[1].count > max[1].count ? curr : max))
    : null;

  const fastestEvent = eventTypes.length > 0
    ? eventTypes.reduce((min, curr) => (curr[1].avgLatency < min[1].avgLatency ? curr : min))
    : null;

  const avgLatencyAcrossAll = eventTypes.length > 0
    ? eventTypes.reduce((sum, [, data]) => sum + data.avgLatency, 0) / eventTypes.length
    : 0;

  return (
    <div className="space-y-6 animate-fade-in">
      {/* Page Title */}
      <div className="card-gradient p-8">
        <div className="flex items-center gap-4">
          <div className="w-12 h-12 rounded-2xl bg-gradient-to-br from-cyan-500 to-blue-600 flex items-center justify-center shadow-lg shadow-cyan-500/30">
            <svg className="w-7 h-7 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
            </svg>
          </div>
          <div>
            <h1 className="text-3xl font-bold text-gradient">Event Analytics</h1>
            <p className="text-sm text-dark-600 mt-1">
              Comprehensive analysis of event types and patterns
            </p>
          </div>
        </div>
      </div>

      {/* Stats Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <EventTypeStatsCard
          title="Total Event Types"
          value={totalEventTypes}
          description="Unique event types detected"
          color="blue"
          icon={
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 12V7a4 4 0 014-4z"
              />
            </svg>
          }
        />

        <EventTypeStatsCard
          title="Most Frequent"
          value={mostFrequentEvent ? mostFrequentEvent[0] : 'N/A'}
          description={
            mostFrequentEvent
              ? `${mostFrequentEvent[1].count.toLocaleString()} events`
              : 'No data'
          }
          color="green"
          icon={
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6"
              />
            </svg>
          }
        />

        <EventTypeStatsCard
          title="Fastest Event"
          value={fastestEvent ? `${fastestEvent[1].avgLatency.toFixed(1)} ms` : 'N/A'}
          description={fastestEvent ? fastestEvent[0] : 'No data'}
          color="purple"
          icon={
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M13 10V3L4 14h7v7l9-11h-7z"
              />
            </svg>
          }
        />

        <EventTypeStatsCard
          title="Average Latency"
          value={`${avgLatencyAcrossAll.toFixed(1)} ms`}
          description="Across all event types"
          color="yellow"
          icon={
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"
              />
            </svg>
          }
        />
      </div>

      {/* Charts Row 1 */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <EventsByTypeChart eventsByType={metrics.eventsByType} />
        <EventDistributionChart eventsByType={metrics.eventsByType} topN={8} />
      </div>

      {/* Latency Analysis */}
      <LatencyComparisonChart eventsByType={metrics.eventsByType} topN={10} />

      {/* Detailed Table */}
      <div>
        <h2 className="text-lg font-semibold text-gray-900 mb-4">
          Event Type Details
        </h2>
        <EventTypeTable eventsByType={metrics.eventsByType} />
      </div>

      {/* Top & Bottom Performers */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Top Performers */}
        <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-200">
          <h3 className="text-lg font-semibold text-gray-900 mb-4">
            Top Performers (Lowest Latency)
          </h3>
          <div className="space-y-3">
            {eventTypes.filter(([, data]) => data.avgLatency <= 50)
              .sort(([, a], [, b]) => a.avgLatency - b.avgLatency)
              .slice(0, 5)
              .map(([type, data], index) => (
                <div
                  key={type}
                  className="flex items-center justify-between p-3 bg-green-50 rounded-lg border border-green-200"
                >
                  <div className="flex items-center gap-3">
                    <span className="flex items-center justify-center w-6 h-6 bg-green-600 text-white text-xs font-bold rounded-full">
                      {index + 1}
                    </span>
                    <span className="text-sm font-medium text-gray-900">{type}</span>
                  </div>
                  <span className="text-sm font-semibold text-green-700">
                    {data.avgLatency.toFixed(2)} ms
                  </span>
                </div>
              ))}
          </div>
        </div>

        {/* Needs Attention */}
        <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-200">
          <h3 className="text-lg font-semibold text-gray-900 mb-4">
            Needs Attention (Highest Latency)
          </h3>
          <div className="space-y-3">
            {eventTypes.filter(([, data]) => data.avgLatency > 50)
              .sort(([, a], [, b]) => b.avgLatency - a.avgLatency)
              .slice(0, 5)
              .map(([type, data], index) => (
                <div
                  key={type}
                  className="flex items-center justify-between p-3 bg-red-50 rounded-lg border border-red-200"
                >
                  <div className="flex items-center gap-3">
                    <span className="flex items-center justify-center w-6 h-6 bg-red-600 text-white text-xs font-bold rounded-full">
                      {index + 1}
                    </span>
                    <span className="text-sm font-medium text-gray-900">{type}</span>
                  </div>
                  <span className="text-sm font-semibold text-red-700">
                    {data.avgLatency.toFixed(2)} ms
                  </span>
                </div>
              ))}
          </div>
        </div>
      </div>
    </div>
  );
};
