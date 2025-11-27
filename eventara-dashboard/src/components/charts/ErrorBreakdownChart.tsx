import { Doughnut } from 'react-chartjs-2';
import { ErrorBreakdown } from '../../types';

interface ErrorBreakdownChartProps {
  errors: ErrorBreakdown[];
  title: string;
}

export const ErrorBreakdownChart: React.FC<ErrorBreakdownChartProps> = ({ errors, title }) => {
  const topErrors = errors.slice(0, 8);

  const data = {
    labels: topErrors.map(error => error.name),
    datasets: [
      {
        data: topErrors.map(error => error.count),
        backgroundColor: [
          'rgba(239, 68, 68, 0.8)',
          'rgba(251, 146, 60, 0.8)',
          'rgba(245, 158, 11, 0.8)',
          'rgba(234, 179, 8, 0.8)',
          'rgba(132, 204, 22, 0.8)',
          'rgba(34, 197, 94, 0.8)',
          'rgba(20, 184, 166, 0.8)',
          'rgba(6, 182, 212, 0.8)',
        ],
        borderColor: [
          'rgb(239, 68, 68)',
          'rgb(251, 146, 60)',
          'rgb(245, 158, 11)',
          'rgb(234, 179, 8)',
          'rgb(132, 204, 22)',
          'rgb(34, 197, 94)',
          'rgb(20, 184, 166)',
          'rgb(6, 182, 212)',
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
        position: 'right' as const,
        labels: {
          boxWidth: 12,
          padding: 15,
          font: {
            size: 11,
          },
        },
      },
      tooltip: {
        callbacks: {
          label: function (context: any) {
            const label = context.label || '';
            const value = context.parsed || 0;
            const percentage = context.dataset.data
              ? ((value / context.dataset.data.reduce((a: number, b: number) => a + b, 0)) * 100).toFixed(1)
              : '0';
            return `${label}: ${value.toLocaleString()} (${percentage}%)`;
          },
        },
      },
    },
  };

  return (
    <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-200">
      <h3 className="text-lg font-semibold text-gray-900 mb-4">{title}</h3>
      <div className="h-80">
        {topErrors.length > 0 ? (
          <Doughnut data={data} options={options} />
        ) : (
          <div className="flex items-center justify-center h-full text-gray-400">
            No errors detected
          </div>
        )}
      </div>
    </div>
  );
};
