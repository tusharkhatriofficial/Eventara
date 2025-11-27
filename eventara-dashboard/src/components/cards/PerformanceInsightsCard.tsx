import { PerformanceMetrics } from '../../types';

interface PerformanceInsightsCardProps {
  performance: PerformanceMetrics;
}

export const PerformanceInsightsCard: React.FC<PerformanceInsightsCardProps> = ({ performance }) => {
  const insights = [];

  // Generate insights based on metrics
  if (performance.avgLatency < 50) {
    insights.push({
      type: 'success',
      icon: '✓',
      title: 'Excellent Performance',
      message: 'Average latency is excellent (< 50ms). System is performing optimally.',
    });
  } else if (performance.avgLatency > 500) {
    insights.push({
      type: 'error',
      icon: '⚠',
      title: 'High Latency Detected',
      message: 'Average latency exceeds 500ms. Consider performance optimization.',
    });
  }

  if (performance.p99 > performance.avgLatency * 5) {
    insights.push({
      type: 'warning',
      icon: '!',
      title: 'P99 Spike',
      message: 'P99 latency is significantly higher than average. Some requests experiencing delays.',
    });
  }

  if (performance.maxLatency > 1000) {
    insights.push({
      type: 'warning',
      icon: '!',
      title: 'High Max Latency',
      message: `Peak latency reached ${performance.maxLatency.toFixed(0)}ms. Investigate slow operations.`,
    });
  }

  const variance = performance.maxLatency - performance.minLatency;
  if (variance < 100) {
    insights.push({
      type: 'success',
      icon: '✓',
      title: 'Consistent Performance',
      message: 'Low latency variance indicates stable system performance.',
    });
  }

  if (insights.length === 0) {
    insights.push({
      type: 'info',
      icon: 'ℹ',
      title: 'Performance Normal',
      message: 'All metrics within acceptable ranges.',
    });
  }

  const getInsightStyle = (type: string) => {
    switch (type) {
      case 'success':
        return 'bg-green-50 border-green-200 text-green-800';
      case 'warning':
        return 'bg-yellow-50 border-yellow-200 text-yellow-800';
      case 'error':
        return 'bg-red-50 border-red-200 text-red-800';
      default:
        return 'bg-blue-50 border-blue-200 text-blue-800';
    }
  };

  return (
    <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-200">
      <h3 className="text-lg font-semibold text-gray-900 mb-4">Performance Insights</h3>
      <div className="space-y-3">
        {insights.map((insight, index) => (
          <div
            key={index}
            className={`p-4 rounded-lg border ${getInsightStyle(insight.type)}`}
          >
            <div className="flex items-start gap-3">
              <span className="text-2xl">{insight.icon}</span>
              <div>
                <h4 className="font-semibold text-sm mb-1">{insight.title}</h4>
                <p className="text-xs">{insight.message}</p>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};
