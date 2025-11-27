import { Doughnut } from 'react-chartjs-2';
import { UserMetrics } from '../../types';

interface UserEngagementGaugeProps {
  userMetrics: UserMetrics;
}

export const UserEngagementGauge: React.FC<UserEngagementGaugeProps> = ({ userMetrics }) => {
  const activeNow = userMetrics.activeUsersLast1Hour;
  const activeLast24h = userMetrics.activeUsersLast24Hours - activeNow;
  const inactive = userMetrics.totalUniqueUsers - userMetrics.activeUsersLast24Hours;

  const data = {
    labels: ['Active Now (1h)', 'Active Last 24h', 'Inactive'],
    datasets: [
      {
        data: [activeNow, activeLast24h, inactive],
        backgroundColor: [
          'rgba(16, 185, 129, 0.8)',  // green
          'rgba(59, 130, 246, 0.8)',   // blue
          'rgba(209, 213, 219, 0.8)',  // gray
        ],
        borderColor: [
          'rgb(16, 185, 129)',
          'rgb(59, 130, 246)',
          'rgb(209, 213, 219)',
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
        position: 'bottom' as const,
        labels: {
          padding: 15,
          font: {
            size: 12,
          },
        },
      },
      tooltip: {
        callbacks: {
          label: function (context: any) {
            const label = context.label || '';
            const value = context.parsed || 0;
            const total = userMetrics.totalUniqueUsers;
            const percentage = total > 0 ? ((value / total) * 100).toFixed(1) : '0';
            return `${label}: ${value} (${percentage}%)`;
          },
        },
      },
    },
  };

  return (
    <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-200">
      <h3 className="text-lg font-semibold text-gray-900 mb-4">
        User Engagement Breakdown
      </h3>
      <div className="h-80">
        <Doughnut data={data} options={options} />
      </div>
      <div className="mt-4 grid grid-cols-3 gap-4 text-center">
        <div>
          <p className="text-2xl font-bold text-green-600">{activeNow}</p>
          <p className="text-xs text-gray-600 mt-1">Active Now</p>
        </div>
        <div>
          <p className="text-2xl font-bold text-blue-600">{activeLast24h}</p>
          <p className="text-xs text-gray-600 mt-1">Last 24h</p>
        </div>
        <div>
          <p className="text-2xl font-bold text-gray-600">{inactive}</p>
          <p className="text-xs text-gray-600 mt-1">Inactive</p>
        </div>
      </div>
    </div>
  );
};
