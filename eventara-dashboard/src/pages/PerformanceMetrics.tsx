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
          <div className="w-16 h-16 rounded-2xl bg-gradient-to-br from-purple-500 to-purple-700 flex items-center justify-center mb-4 shadow-lg shadow-purple-500/30 animate-pulse">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-white"></div>
          </div>
          <p className="text-dark-600 font-medium">Loading performance metrics...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6 animate-fade-in">
      {/* Page Title */}
      <div className="card-gradient p-8">
        <div className="flex items-center gap-4">
          <div className="w-12 h-12 rounded-2xl bg-gradient-to-br from-purple-500 to-purple-700 flex items-center justify-center shadow-lg shadow-purple-500/30">
            <svg className="w-7 h-7 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
            </svg>
          </div>
          <div>
            <h1 className="text-3xl font-bold text-gradient">Performance Metrics</h1>
            <p className="text-sm text-dark-600 mt-1">
              Detailed latency analysis and performance optimization insights
            </p>
          </div>
        </div>
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
      <div className="card-gradient p-8">
        <h3 className="text-2xl font-bold text-dark-900 mb-6 flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-purple-500 to-purple-700 flex items-center justify-center">
            <svg className="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
            </svg>
          </div>
          Performance Summary
        </h3>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <div className="stat-card">
            <p className="text-sm font-semibold text-dark-600 mb-3">Latency Range</p>
            <p className="text-3xl font-bold text-gradient">
              {metrics.performance.minLatency.toFixed(1)} - {metrics.performance.maxLatency.toFixed(1)} ms
            </p>
            <p className="text-xs text-dark-500 mt-2">Min to Max</p>
          </div>
          <div className="stat-card">
            <p className="text-sm font-semibold text-dark-600 mb-3">Latency Variance</p>
            <p className="text-3xl font-bold text-gradient">
              {(metrics.performance.maxLatency - metrics.performance.minLatency).toFixed(1)} ms
            </p>
            <p className="text-xs text-dark-500 mt-2">Max - Min</p>
          </div>
          <div className="stat-card">
            <p className="text-sm font-semibold text-dark-600 mb-3">P95/P50 Ratio</p>
            <p className="text-3xl font-bold text-gradient">
              {metrics.performance.p50 > 0 
                ? (metrics.performance.p95 / metrics.performance.p50).toFixed(2) 
                : 'N/A'}x
            </p>
            <p className="text-xs text-dark-500 mt-2">Consistency indicator</p>
          </div>
        </div>
      </div>

      {/* Key Metrics Table */}
      <div className="card-gradient overflow-hidden">
        <div className="p-8 border-b border-dark-100">
          <h3 className="text-2xl font-bold text-dark-900 flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-primary-500 to-primary-700 flex items-center justify-center">
              <svg className="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
              </svg>
            </div>
            Detailed Metrics
          </h3>
        </div>
        <div className="overflow-x-auto">
          <table className="table-modern">
            <thead>
              <tr>
                <th>Metric</th>
                <th>Value</th>
                <th>Status</th>
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
