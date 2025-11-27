import { SourceMetrics } from '../../types';

interface DetailedSourceCardProps {
  sourceName: string;
  metrics: SourceMetrics;
}

export const DetailedSourceCard: React.FC<DetailedSourceCardProps> = ({
  sourceName,
  metrics
}) => {
  const healthConfig = {
    healthy: {
      bg: 'bg-green-50',
      border: 'border-green-200',
      text: 'text-green-700',
      badge: 'bg-green-100 text-green-800',
      dot: 'bg-green-500'
    },
    degraded: {
      bg: 'bg-yellow-50',
      border: 'border-yellow-200',
      text: 'text-yellow-700',
      badge: 'bg-yellow-100 text-yellow-800',
      dot: 'bg-yellow-500'
    },
    down: {
      bg: 'bg-red-50',
      border: 'border-red-200',
      text: 'text-red-700',
      badge: 'bg-red-100 text-red-800',
      dot: 'bg-red-500'
    }
  };

  const config = healthConfig[metrics.health];

  return (
    <div className={`bg-white rounded-lg shadow-sm border-2 ${config.border} hover:shadow-md transition-shadow`}>
      {/* Header */}
      <div className={`p-4 border-b ${config.border} ${config.bg}`}>
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className={`w-3 h-3 rounded-full ${config.dot} animate-pulse`}></div>
            <h3 className="font-semibold text-gray-900">{sourceName}</h3>
          </div>
          <span className={`px-3 py-1 text-xs font-semibold rounded-full uppercase ${config.badge}`}>
            {metrics.health}
          </span>
        </div>
      </div>

      {/* Metrics Grid */}
      <div className="p-6">
        <div className="grid grid-cols-2 gap-4">
          {/* Event Count */}
          <div>
            <p className="text-xs text-gray-600 mb-1">Total Events</p>
            <p className="text-2xl font-bold text-gray-900">{metrics.count.toLocaleString()}</p>
          </div>

          {/* Average Latency */}
          <div>
            <p className="text-xs text-gray-600 mb-1">Avg Latency</p>
            <p className={`text-2xl font-bold ${
              metrics.avgLatency < 100 ? 'text-green-600' :
              metrics.avgLatency < 500 ? 'text-yellow-600' :
              'text-red-600'
            }`}>
              {metrics.avgLatency.toFixed(1)} <span className="text-sm">ms</span>
            </p>
          </div>

          {/* Error Count */}
          <div>
            <p className="text-xs text-gray-600 mb-1">Errors</p>
            <p className="text-2xl font-bold text-red-600">{metrics.errorCount.toLocaleString()}</p>
          </div>

          {/* Error Rate */}
          <div>
            <p className="text-xs text-gray-600 mb-1">Error Rate</p>
            <p className={`text-2xl font-bold ${
              metrics.errorRate < 1 ? 'text-green-600' :
              metrics.errorRate < 5 ? 'text-yellow-600' :
              'text-red-600'
            }`}>
              {metrics.errorRate.toFixed(1)} <span className="text-sm">%</span>
            </p>
          </div>
        </div>

        {/* Progress Bar */}
        <div className="mt-6">
          <div className="flex items-center justify-between text-xs text-gray-600 mb-2">
            <span>Success Rate</span>
            <span>{(100 - metrics.errorRate).toFixed(1)}%</span>
          </div>
          <div className="w-full bg-gray-200 rounded-full h-2">
            <div
              className={`h-2 rounded-full ${
                metrics.errorRate < 1 ? 'bg-green-500' :
                metrics.errorRate < 5 ? 'bg-yellow-500' :
                'bg-red-500'
              }`}
              style={{ width: `${Math.max(0, 100 - metrics.errorRate)}%` }}
            ></div>
          </div>
        </div>
      </div>
    </div>
  );
};
