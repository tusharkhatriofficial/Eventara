import { Bar } from 'react-chartjs-2';
import { EventTypeMetrics } from '../../types';

interface LatencyComparisonChartProps {
  eventsByType: Record<string, EventTypeMetrics>;
  topN?: number;
}

export const LatencyComparisonChart: React.FC<LatencyComparisonChartProps> = ({
  eventsByType,
  topN = 10
}) => {
  const sortedByLatency = Object.entries(eventsByType)
    .sort(([, a], [, b]) => b.avgLatency - a.avgLatency)
    .slice(0, topN);

  const data = {
    labels: sortedByLatency.map(([type]) => type),
    datasets: [
      {
        label: 'Average Latency (ms)',
        data: sortedByLatency.map(([, metrics]) => metrics.avgLatency),
        backgroundColor: sortedByLatency.map(([, metrics]) =>
          metrics.avgLatency < 50
            ? 'rgba(16, 185, 129, 0.8)'
            : metrics.avgLatency < 200
            ? 'rgba(245, 158, 11, 0.8)'
            : 'rgba(239, 68, 68, 0.8)'
        ),
        borderColor: sortedByLatency.map(([, metrics]) =>
          metrics.avgLatency < 50
            ? 'rgb(16, 185, 129)'
            : metrics.avgLatency < 200
            ? 'rgb(245, 158, 11)'
            : 'rgb(239, 68, 68)'
        ),
        borderWidth: 2,
      },
    ],
  };

  const options = {
    responsive: true,
    maintainAspectRatio: false,
    indexAxis: 'y' as const,
    plugins: {
      legend: {
        display: false,
      },
      tooltip: {
        callbacks: {
          label: function (context: any) {
            return `${context.parsed.x.toFixed(2)} ms`;
          },
        },
      },
    },
    scales: {
      x: {
        beginAtZero: true,
        title: {
          display: true,
          text: 'Latency (ms)',
        },
      },
    },
  };

  return (
    <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-200">
      <h3 className="text-lg font-semibold text-gray-900 mb-4">
        Latency by Event Type
      </h3>
      <div className="h-96">
        {sortedByLatency.length > 0 ? (
          <Bar data={data} options={options} />
        ) : (
          <div className="flex items-center justify-center h-full text-gray-400">
            No data available
          </div>
        )}
      </div>
    </div>
  );
};
