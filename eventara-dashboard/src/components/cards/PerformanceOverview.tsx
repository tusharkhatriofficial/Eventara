import { PerformanceMetrics } from '../../types';

interface PerformanceOverviewProps {
  performance: PerformanceMetrics;
}

export const PerformanceOverview: React.FC<PerformanceOverviewProps> = ({ performance }) => {
  const getLatencyStatus = (latency: number) => {
    if (latency < 50) return { color: 'green', label: 'Excellent' };
    if (latency < 200) return { color: 'blue', label: 'Good' };
    if (latency < 500) return { color: 'yellow', label: 'Fair' };
    return { color: 'red', label: 'Poor' };
  };

  const avgStatus = getLatencyStatus(performance.avgLatency);

  return (
    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-6">
      {/* Average Latency */}
      <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-200">
        <div className="flex items-center justify-between mb-2">
          <p className="text-sm font-medium text-gray-600">Average Latency</p>
          <span className={`px-2 py-1 text-xs font-semibold rounded-full bg-${avgStatus.color}-100 text-${avgStatus.color}-800`}>
            {avgStatus.label}
          </span>
        </div>
        <p className="text-3xl font-bold text-gray-900 mt-2">
          {performance.avgLatency.toFixed(1)}
          <span className="text-lg text-gray-500 ml-1">ms</span>
        </p>
      </div>

      {/* P50 */}
      <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-200">
        <p className="text-sm font-medium text-gray-600">P50 (Median)</p>
        <p className="text-3xl font-bold text-blue-600 mt-2">
          {performance.p50.toFixed(1)}
          <span className="text-lg text-gray-500 ml-1">ms</span>
        </p>
        <p className="text-xs text-gray-500 mt-1">50th percentile</p>
      </div>

      {/* P95 */}
      <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-200">
        <p className="text-sm font-medium text-gray-600">P95</p>
        <p className="text-3xl font-bold text-yellow-600 mt-2">
          {performance.p95.toFixed(1)}
          <span className="text-lg text-gray-500 ml-1">ms</span>
        </p>
        <p className="text-xs text-gray-500 mt-1">95th percentile</p>
      </div>

      {/* P99 */}
      <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-200">
        <p className="text-sm font-medium text-gray-600">P99</p>
        <p className="text-3xl font-bold text-orange-600 mt-2">
          {performance.p99.toFixed(1)}
          <span className="text-lg text-gray-500 ml-1">ms</span>
        </p>
        <p className="text-xs text-gray-500 mt-1">99th percentile</p>
      </div>

      {/* Max Latency */}
      <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-200">
        <p className="text-sm font-medium text-gray-600">Max Latency</p>
        <p className="text-3xl font-bold text-red-600 mt-2">
          {performance.maxLatency.toFixed(1)}
          <span className="text-lg text-gray-500 ml-1">ms</span>
        </p>
        <p className="text-xs text-gray-500 mt-1">Worst case</p>
      </div>
    </div>
  );
};
