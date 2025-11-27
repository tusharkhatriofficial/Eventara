import { Doughnut } from 'react-chartjs-2';
import { SourceMetrics } from '../../types';

interface EventsBySourceChartProps {
  eventsBySource: Record<string, SourceMetrics>;
}

export const EventsBySourceChart: React.FC<EventsBySourceChartProps> = ({ eventsBySource }) => {
  const sources = Object.entries(eventsBySource);

  const data = {
    labels: sources.map(([source]) => source),
    datasets: [
      {
        label: 'Events',
        data: sources.map(([, metrics]) => metrics.count),
        backgroundColor: [
          'rgba(59, 130, 246, 0.8)',   // blue
          'rgba(16, 185, 129, 0.8)',   // green
          'rgba(245, 158, 11, 0.8)',   // amber
          'rgba(239, 68, 68, 0.8)',    // red
          'rgba(139, 92, 246, 0.8)',   // purple
          'rgba(236, 72, 153, 0.8)',   // pink
          'rgba(34, 197, 94, 0.8)',    // lime
          'rgba(251, 146, 60, 0.8)',   // orange
        ],
        borderColor: [
          'rgb(59, 130, 246)',
          'rgb(16, 185, 129)',
          'rgb(245, 158, 11)',
          'rgb(239, 68, 68)',
          'rgb(139, 92, 246)',
          'rgb(236, 72, 153)',
          'rgb(34, 197, 94)',
          'rgb(251, 146, 60)',
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
      },
      title: {
        display: false,
      },
    },
  };

  return (
    <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-200">
      <h3 className="text-lg font-semibold text-gray-900 mb-4">
        Events by Source
      </h3>
      <div className="h-64">
        {sources.length > 0 ? (
          <Doughnut data={data} options={options} />
        ) : (
          <div className="flex items-center justify-center h-full text-gray-400">
            No data available
          </div>
        )}
      </div>
    </div>
  );
};
