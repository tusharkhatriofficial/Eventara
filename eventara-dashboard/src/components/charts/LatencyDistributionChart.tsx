import { Bar } from 'react-chartjs-2';
import { PerformanceMetrics } from '../../types';

interface LatencyDistributionChartProps {
  performance: PerformanceMetrics;
}

export const LatencyDistributionChart: React.FC<LatencyDistributionChartProps> = ({ performance }) => {
  const data = {
    labels: ['Min', 'P50', 'Avg', 'P95', 'P99', 'Max'],
    datasets: [
      {
        label: 'Latency (ms)',
        data: [
          performance.minLatency,
          performance.p50,
          performance.avgLatency,
          performance.p95,
          performance.p99,
          performance.maxLatency,
        ],
        backgroundColor: [
          'rgba(16, 185, 129, 0.8)',   // green
          'rgba(59, 130, 246, 0.8)',   // blue
          'rgba(139, 92, 246, 0.8)',   // purple
          'rgba(245, 158, 11, 0.8)',   // yellow
          'rgba(251, 146, 60, 0.8)',   // orange
          'rgba(239, 68, 68, 0.8)',    // red
        ],
        borderColor: [
          'rgb(16, 185, 129)',
          'rgb(59, 130, 246)',
          'rgb(139, 92, 246)',
          'rgb(245, 158, 11)',
          'rgb(251, 146, 60)',
          'rgb(239, 68, 68)',
        ],
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
        Latency Distribution
      </h3>
      <div className="h-80">
        <Bar data={data} options={options} />
      </div>
    </div>
  );
};
