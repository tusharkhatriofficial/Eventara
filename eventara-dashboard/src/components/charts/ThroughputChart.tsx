import { Line } from 'react-chartjs-2';
import { useEffect, useState } from 'react';
import { ThroughputMetrics } from '../../types';

interface ThroughputChartProps {
  throughput: ThroughputMetrics;
}

interface ThroughputPoint {
  label: string;
  value: number;
}

export const ThroughputChart: React.FC<ThroughputChartProps> = ({ throughput }) => {
  const [history, setHistory] = useState<ThroughputPoint[]>([]);

  useEffect(() => {
    const now = new Date();
    const timeLabel = now.toLocaleTimeString();

    setHistory(prev => {
      const newHistory = [
        ...prev,
        {
          label: timeLabel,
          value: throughput.current.perSecond
        }
      ];

      // Keep only last 30 data points
      return newHistory.slice(-30);
    });
  }, [throughput.current.perSecond]);

  const data = {
    labels: history.map(point => point.label),
    datasets: [
      {
        label: 'Events/Second',
        data: history.map(point => point.value),
        borderColor: 'rgb(99, 102, 241)',
        backgroundColor: 'rgba(99, 102, 241, 0.1)',
        fill: true,
        tension: 0.4,
        pointRadius: 2,
        pointHoverRadius: 6,
        pointBackgroundColor: 'rgb(99, 102, 241)',
        pointBorderColor: '#fff',
        pointBorderWidth: 2,
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
          maxTicksLimit: 6,
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
          precision: 2,
        },
      },
    },
    animation: {
      duration: 500,
    },
    interaction: {
      intersect: false,
      mode: 'index' as const,
    },
  };

  return (
    <div className="card p-6">
      <div className="flex items-center justify-between mb-4">
        <div>
          <h3 className="text-lg font-semibold text-slate-900">Throughput Trend</h3>
          <p className="text-sm text-slate-500 mt-0.5">Events processed over time</p>
        </div>
        <div className="flex items-center gap-4 text-sm">
          <div className="flex items-center gap-2">
            <span className="w-3 h-3 rounded-full bg-success-500"></span>
            <span className="text-slate-600">Current: <span className="font-semibold text-slate-900">{throughput.current.perSecond.toFixed(2)}</span></span>
          </div>
          <div className="flex items-center gap-2">
            <span className="w-3 h-3 rounded-full bg-primary-500"></span>
            <span className="text-slate-600">Peak: <span className="font-semibold text-slate-900">{throughput.peak.value.toFixed(2)}</span></span>
          </div>
        </div>
      </div>
      <div className="h-64">
        <Line data={data} options={options} />
      </div>
    </div>
  );
};
