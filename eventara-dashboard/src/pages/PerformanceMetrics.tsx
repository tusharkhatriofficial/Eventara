import { ComprehensiveMetrics } from '../types';
import { PerformanceOverview } from '../components/cards/PerformanceOverview';
import { LatencyDistributionChart } from '../components/charts/LatencyDistributionChart';
import { PercentileComparisonChart } from '../components/charts/PercentileComparisonChart';
import { PerformanceInsightsCard } from '../components/cards/PerformanceInsightsCard';
import { SLAComplianceCard } from '../components/cards/SLAComplianceCard';
import { LatencyComparisonChart } from '../components/charts/LatencyComparisonChart';

interface PerformanceMetricsProps {
  metrics: ComprehensiveMetrics | null;
}

export const PerformanceMetrics: React.FC<PerformanceMetricsProps> = ({ metrics }) => {
  if (!metrics) {
    return (
      <div className="flex items-center justify-center h-full">
        <div className="text-center">
          <div className="inline-block animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mb-4"></div>
          <p className="text-gray-600">Loading performance metrics...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Page Title */}
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Performance Metrics</h1>
        <p className="text-sm text-gray-500 mt-1">
          Detailed latency analysis and performance optimization insights
        </p>
      </div>

      {/* Performance Overview Cards */}
      <PerformanceOverview performance={metrics.performance} />

      {/* Charts Row 1 */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <LatencyDistributionChart performance={metrics.performance} />
        <PercentileComparisonChart performance={metrics.performance} />
      </div>

      {/* Insights & SLA Compliance */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <PerformanceInsightsCard performance={metrics.performance} />
        <SLAComplianceCard performance={metrics.performance} />
      </div>

      {/* Latency by Event Type */}
      <LatencyComparisonChart eventsByType={metrics.eventsByType} topN={10} />

      {/* Performance Summary */}
      <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-200">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">Performance Summary</h3>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <div>
            <p className="text-sm text-gray-600 mb-2">Latency Range</p>
            <p className="text-2xl font-bold text-gray-900">
              {metrics.performance.minLatency.toFixed(1)} - {metrics.performance.maxLatency.toFixed(1)} ms
            </p>
            <p className="text-xs text-gray-500 mt-1">Min to Max</p>
          </div>
          <div>
            <p className="text-sm text-gray-600 mb-2">Latency Variance</p>
            <p className="text-2xl font-bold text-gray-900">
              {(metrics.performance.maxLatency - metrics.performance.minLatency).toFixed(1)} ms
            </p>
            <p className="text-xs text-gray-500 mt-1">Max - Min</p>
          </div>
          <div>
            <p className="text-sm text-gray-600 mb-2">P95/P50 Ratio</p>
            <p className="text-2xl font-bold text-gray-900">
              {metrics.performance.p50 > 0 
                ? (metrics.performance.p95 / metrics.performance.p50).toFixed(2) 
                : 'N/A'}x
            </p>
            <p className="text-xs text-gray-500 mt-1">Consistency indicator</p>
          </div>
        </div>
      </div>

      {/* Key Metrics Table */}
      <div className="bg-white rounded-lg shadow-sm border border-gray-200 overflow-hidden">
        <div className="p-6 border-b border-gray-200">
          <h3 className="text-lg font-semibold text-gray-900">Detailed Metrics</h3>
        </div>
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Metric
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Value
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Status
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Description
                </th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              <tr>
                <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">Min Latency</td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">{metrics.performance.minLatency.toFixed(2)} ms</td>
                <td className="px-6 py-4 whitespace-nowrap">
                  <span className="px-2 py-1 text-xs font-semibold rounded-full bg-green-100 text-green-800">
                    Best Case
                  </span>
                </td>
                <td className="px-6 py-4 text-sm text-gray-500">Fastest response time</td>
              </tr>
              <tr>
                <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">Average Latency</td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">{metrics.performance.avgLatency.toFixed(2)} ms</td>
                <td className="px-6 py-4 whitespace-nowrap">
                  <span className={`px-2 py-1 text-xs font-semibold rounded-full ${
                    metrics.performance.avgLatency < 100 ? 'bg-green-100 text-green-800' :
                    metrics.performance.avgLatency < 200 ? 'bg-blue-100 text-blue-800' :
                    'bg-yellow-100 text-yellow-800'
                  }`}>
                    {metrics.performance.avgLatency < 100 ? 'Excellent' :
                     metrics.performance.avgLatency < 200 ? 'Good' : 'Fair'}
                  </span>
                </td>
                <td className="px-6 py-4 text-sm text-gray-500">Mean response time</td>
              </tr>
              <tr>
                <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">P50 (Median)</td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">{metrics.performance.p50.toFixed(2)} ms</td>
                <td className="px-6 py-4 whitespace-nowrap">
                  <span className="px-2 py-1 text-xs font-semibold rounded-full bg-blue-100 text-blue-800">
                    Typical
                  </span>
                </td>
                <td className="px-6 py-4 text-sm text-gray-500">50% of requests faster than this</td>
              </tr>
              <tr>
                <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">P95</td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">{metrics.performance.p95.toFixed(2)} ms</td>
                <td className="px-6 py-4 whitespace-nowrap">
                  <span className="px-2 py-1 text-xs font-semibold rounded-full bg-yellow-100 text-yellow-800">
                    High Load
                  </span>
                </td>
                <td className="px-6 py-4 text-sm text-gray-500">95% of requests faster than this</td>
              </tr>
              <tr>
                <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">P99</td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">{metrics.performance.p99.toFixed(2)} ms</td>
                <td className="px-6 py-4 whitespace-nowrap">
                  <span className="px-2 py-1 text-xs font-semibold rounded-full bg-orange-100 text-orange-800">
                    Edge Case
                  </span>
                </td>
                <td className="px-6 py-4 text-sm text-gray-500">99% of requests faster than this</td>
              </tr>
              <tr>
                <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">Max Latency</td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">{metrics.performance.maxLatency.toFixed(2)} ms</td>
                <td className="px-6 py-4 whitespace-nowrap">
                  <span className="px-2 py-1 text-xs font-semibold rounded-full bg-red-100 text-red-800">
                    Worst Case
                  </span>
                </td>
                <td className="px-6 py-4 text-sm text-gray-500">Slowest response time</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};
