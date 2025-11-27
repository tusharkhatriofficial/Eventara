import { Line } from 'react-chartjs-2';
import { PerformanceMetrics } from '../../types';

interface PercentileComparisonChartProps {
  performance: PerformanceMetrics;
}

export const PercentileComparisonChart: React.FC<PercentileComparisonChartProps> = ({ performance }) => {
  const data = {
    labels: ['0%', '50%', '95%', '99%', '100%'],
    datasets: [
      {
        label: 'Latency',
        data: [
          performance.minLatency,
          performance.p50,
          performance.p95,
          performance.p99,
          performance.maxLatency,
        ],
        borderColor: 'rgb(59, 130, 246)',
        backgroundColor: 'rgba(59, 130, 246, 0.1)',
        fill: true,
        tension: 0.4,
        pointRadius: 6,
        pointHoverRadius: 8,
        pointBackgroundColor: [
          'rgb(16, 185, 129)',
          'rgb(59, 130, 246)',
          'rgb(245, 158, 11)',
          'rgb(251, 146, 60)',
          'rgb(239, 68, 68)',
        ],
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
            return `${context.parsed.y.toFixed(2)} ms at ${context.label}`;
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
      x: {
        title: {
          display: true,
          text: 'Percentile',
        },
      },
    },
  };

  return (
    <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-200">
      <h3 className="text-lg font-semibold text-gray-900 mb-4">
        Percentile Curve
      </h3>
      <div className="h-80">
        <Line data={data} options={options} />
      </div>
    </div>
  );
};
