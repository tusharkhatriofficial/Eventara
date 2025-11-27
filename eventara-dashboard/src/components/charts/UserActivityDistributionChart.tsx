import { Bar } from 'react-chartjs-2';
import { UserActivity } from '../../types';

interface UserActivityDistributionChartProps {
  topActiveUsers: UserActivity[];
}

export const UserActivityDistributionChart: React.FC<UserActivityDistributionChartProps> = ({
  topActiveUsers
}) => {
  const topUsers = topActiveUsers.slice(0, 10);

  const data = {
    labels: topUsers.map(user => user.userId),
    datasets: [
      {
        label: 'Event Count',
        data: topUsers.map(user => user.eventCount),
        backgroundColor: 'rgba(59, 130, 246, 0.8)',
        borderColor: 'rgb(59, 130, 246)',
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
            return `${context.parsed.y.toLocaleString()} events`;
          },
        },
      },
    },
    scales: {
      y: {
        beginAtZero: true,
        title: {
          display: true,
          text: 'Number of Events',
        },
        ticks: {
          precision: 0,
        },
      },
      x: {
        title: {
          display: true,
          text: 'User ID',
        },
      },
    },
  };

  return (
    <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-200">
      <h3 className="text-lg font-semibold text-gray-900 mb-4">
        User Activity Distribution
      </h3>
      <div className="h-80">
        {topUsers.length > 0 ? (
          <Bar data={data} options={options} />
        ) : (
          <div className="flex items-center justify-center h-full text-gray-400">
            No user data available
          </div>
        )}
      </div>
    </div>
  );
};
