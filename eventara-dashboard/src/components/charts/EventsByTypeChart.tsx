import { EventTypeMetrics } from '../../types';
import { Doughnut } from 'react-chartjs-2';

interface EventsByTypeChartProps {
  eventsByType: Record<string, EventTypeMetrics>;
}

const chartColors = [
  'rgba(99, 102, 241, 0.85)',   // Primary
  'rgba(16, 185, 129, 0.85)',   // Success
  'rgba(245, 158, 11, 0.85)',   // Warning
  'rgba(6, 182, 212, 0.85)',    // Cyan
  'rgba(139, 92, 246, 0.85)',   // Violet
  'rgba(244, 63, 94, 0.85)',    // Error
  'rgba(20, 184, 166, 0.85)',   // Teal
  'rgba(59, 130, 246, 0.85)',   // Blue
];

export const EventsByTypeChart: React.FC<EventsByTypeChartProps> = ({ eventsByType }) => {
  const entries = Object.entries(eventsByType).slice(0, 8);

  const data = {
    labels: entries.map(([type]) => type),
    datasets: [
      {
        data: entries.map(([, metrics]) => metrics.count),
        backgroundColor: chartColors.slice(0, entries.length),
        borderColor: '#fff',
        borderWidth: 2,
        hoverBorderWidth: 3,
        hoverOffset: 8,
      },
    ],
  };

  const options = {
    responsive: true,
    maintainAspectRatio: false,
    cutout: '65%',
    plugins: {
      legend: {
        position: 'right' as const,
        labels: {
          color: '#64748b',
          font: {
            size: 12,
          },
          padding: 16,
          usePointStyle: true,
          pointStyle: 'circle',
        },
      },
      tooltip: {
        backgroundColor: 'rgba(15, 23, 42, 0.9)',
        titleColor: '#fff',
        bodyColor: '#fff',
        borderColor: 'rgba(99, 102, 241, 0.5)',
        borderWidth: 1,
        padding: 12,
        cornerRadius: 8,
      },
    },
  };

  return (
    <div className="card p-6">
      <div className="flex items-center justify-between mb-6">
        <div>
          <h3 className="text-lg font-semibold text-slate-900">Events by Type</h3>
          <p className="text-sm text-slate-500 mt-0.5">Distribution of event types</p>
        </div>
      </div>
      <div className="h-72">
        <Doughnut data={data} options={options} />
      </div>
    </div>
  );
};
