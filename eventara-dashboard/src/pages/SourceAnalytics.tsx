import { ComprehensiveMetrics } from '../types';
import { SourceHealthOverview } from '../components/cards/SourceHealthOverview';
import { DetailedSourceCard } from '../components/cards/DetailedSourceCard';
import { SourceComparisonTable } from '../components/tables/SourceComparisonTable';
import { SourceLatencyChart } from '../components/charts/SourceLatencyChart';
import { EventsBySourceChart } from '../components/charts/EventsBySourceChart';

interface SourceAnalyticsProps {
  metrics: ComprehensiveMetrics | null;
}

export const SourceAnalytics: React.FC<SourceAnalyticsProps> = ({ metrics }) => {
  if (!metrics) {
    return (
      <div className="flex items-center justify-center h-full">
        <div className="text-center">
          <div className="inline-block animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mb-4"></div>
          <p className="text-gray-600">Loading source analytics...</p>
        </div>
      </div>
    );
  }

  const sources = Object.entries(metrics.eventsBySource);

  return (
    <div className="space-y-6">
      {/* Page Title */}
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Source Analytics</h1>
        <p className="text-sm text-gray-500 mt-1">
          Monitor health and performance of all event sources
        </p>
      </div>

      {/* Health Overview Cards */}
      <SourceHealthOverview eventsBySource={metrics.eventsBySource} />

      {/* Charts Row */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <EventsBySourceChart eventsBySource={metrics.eventsBySource} />
        <SourceLatencyChart eventsBySource={metrics.eventsBySource} />
      </div>

      {/* Detailed Source Cards */}
      <div>
        <h2 className="text-lg font-semibold text-gray-900 mb-4">Source Details</h2>
        {sources.length > 0 ? (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {sources.map(([sourceName, sourceMetrics]) => (
              <DetailedSourceCard
                key={sourceName}
                sourceName={sourceName}
                metrics={sourceMetrics}
              />
            ))}
          </div>
        ) : (
          <div className="bg-white p-12 rounded-lg shadow-sm border border-gray-200 text-center">
            <p className="text-gray-500">No sources detected</p>
          </div>
        )}
      </div>

      {/* Comparison Table */}
      <div>
        <h2 className="text-lg font-semibold text-gray-900 mb-4">Source Comparison</h2>
        <SourceComparisonTable eventsBySource={metrics.eventsBySource} />
      </div>
    </div>
  );
};
