import { Bar } from 'react-chartjs-2';
import { SourceMetrics } from '../../types';

interface SourceLatencyChartProps {
  eventsBySource: Record<string, SourceMetrics>;
}

export const SourceLatencyChart: React.FC<SourceLatencyChartProps> = ({ eventsBySource }) => {
  const sources = Object.entries(eventsBySource).sort(([, a], [, b]) => b.avgLatency - a.avgLatency);

  const data = {
    labels: sources.map(([name]) => name),
    datasets: [
      {
        label: 'Average Latency (ms)',
        data: sources.map(([, metrics]) => metrics.avgLatency),
        backgroundColor: sources.map(([, metrics]) =>
          metrics.avgLatency < 100 ? 'rgba(16, 185, 129, 0.8)' :
          metrics.avgLatency < 500 ? 'rgba(245, 158, 11, 0.8)' :
          'rgba(239, 68, 68, 0.8)'
        ),
        borderColor: sources.map(([, metrics]) =>
          metrics.avgLatency < 100 ? 'rgb(16, 185, 129)' :
          metrics.avgLatency < 500 ? 'rgb(245, 158, 11)' :
          'rgb(239, 68, 68)'
        ),
        borderWidth: 2,
      },
    ],
  };

  const options = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        display: false,
      },
      tooltip: {
        callbacks: {
          label: function (context: any) {
            return `${context.parsed.y.toFixed(2)} ms`;
          },
        },
      },
    },
    scales: {
      y: {
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
        Latency by Source
      </h3>
      <div className="h-80">
        {sources.length > 0 ? (
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
