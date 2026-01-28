import { TimeWindowMetrics } from '../../types';
import { Bar } from 'react-chartjs-2';

interface EventsOverTimeChartProps {
  timeWindows: TimeWindowMetrics;
}

export const EventsOverTimeChart: React.FC<EventsOverTimeChartProps> = ({ timeWindows }) => {
  const data = {
    labels: ['1 min', '5 min', '15 min', '1 hour', '24 hours'],
    datasets: [
      {
        label: 'Events',
        data: [
          timeWindows.last1Minute,
          timeWindows.last5Minutes,
          timeWindows.last15Minutes,
          timeWindows.last1Hour,
          timeWindows.last24Hours
        ],
        backgroundColor: [
          'rgba(99, 102, 241, 0.8)',
          'rgba(99, 102, 241, 0.7)',
          'rgba(99, 102, 241, 0.6)',
          'rgba(99, 102, 241, 0.5)',
          'rgba(99, 102, 241, 0.4)',
        ],
        borderColor: [
          'rgb(99, 102, 241)',
          'rgb(99, 102, 241)',
          'rgb(99, 102, 241)',
          'rgb(99, 102, 241)',
          'rgb(99, 102, 241)',
        ],
        borderWidth: 1,
        borderRadius: 8,
        borderSkipped: false,
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
        backgroundColor: 'rgba(15, 23, 42, 0.9)',
        titleColor: '#fff',
        bodyColor: '#fff',
        borderColor: 'rgba(99, 102, 241, 0.5)',
        borderWidth: 1,
        padding: 12,
        cornerRadius: 8,
        displayColors: false,
      },
    },
    scales: {
      x: {
        grid: {
          display: false,
        },
        ticks: {
          color: '#94a3b8',
          font: {
            size: 11,
          },
        },
      },
      y: {
        beginAtZero: true,
        grid: {
          color: 'rgba(148, 163, 184, 0.1)',
        },
        ticks: {
          color: '#94a3b8',
          font: {
            size: 11,
          },
        },
      },
    },
  };

  return (
    <div className="card p-6">
      <div className="flex items-center justify-between mb-4">
        <div>
          <h3 className="text-lg font-semibold text-slate-900">Events Over Time</h3>
          <p className="text-sm text-slate-500 mt-0.5">Event counts by time window</p>
        </div>
      </div>
      <div className="h-64">
        <Bar data={data} options={options} />
      </div>
    </div>
  );
};
