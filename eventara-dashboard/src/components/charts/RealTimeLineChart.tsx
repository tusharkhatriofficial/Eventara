import { Line } from 'react-chartjs-2';
import { useEffect, useRef, useState } from 'react';

interface RealTimeLineChartProps {
  title: string;
  currentValue: number;
  color?: string;
  maxDataPoints?: number;
}

interface DataPoint {
  time: string;
  value: number;
}

export const RealTimeLineChart: React.FC<RealTimeLineChartProps> = ({
  title,
  currentValue,
  color = 'rgb(99, 102, 241)',
  maxDataPoints = 60
}) => {
  const [dataPoints, setDataPoints] = useState<DataPoint[]>([]);
  const lastValueRef = useRef<number>(currentValue);

  useEffect(() => {
    // Only update if value actually changed
    if (currentValue !== lastValueRef.current) {
      lastValueRef.current = currentValue;

      const now = new Date();
      setDataPoints(prev => {
        const newPoints = [
          ...prev,
          {
            time: now.toLocaleTimeString(),
            value: currentValue
          }
        ];
        return newPoints.slice(-maxDataPoints);
      });
    }
  }, [currentValue, maxDataPoints]);

  const data = {
    labels: dataPoints.map(p => p.time),
    datasets: [
      {
        label: title,
        data: dataPoints.map(p => p.value),
        borderColor: color,
        backgroundColor: `${color.replace('rgb', 'rgba').replace(')', ', 0.1)')}`,
        fill: true,
        tension: 0.4,
        pointRadius: 0,
        pointHoverRadius: 4,
        pointBackgroundColor: color,
        pointBorderColor: '#fff',
        pointBorderWidth: 2,
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
        backgroundColor: 'rgba(15, 23, 42, 0.9)',
        titleColor: '#fff',
        bodyColor: '#fff',
        borderColor: `${color.replace('rgb', 'rgba').replace(')', ', 0.5)')}`,
        borderWidth: 1,
        padding: 12,
        cornerRadius: 8,
        displayColors: false,
      },
    },
    scales: {
      x: {
        display: false,
      },
      y: {
        beginAtZero: true,
        grid: {
          color: 'rgba(148, 163, 184, 0.1)',
        },
        ticks: {
          color: '#94a3b8',
          font: {
            size: 10,
          },
        },
      },
    },
    animation: {
      duration: 300,
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
          <h3 className="text-lg font-semibold text-slate-900">{title}</h3>
          <p className="text-sm text-slate-500 mt-0.5">Real-time updates</p>
        </div>
        <div className="flex items-center gap-2">
          <span className="w-2 h-2 rounded-full animate-pulse" style={{ backgroundColor: color }}></span>
          <span className="text-2xl font-bold text-slate-900">{currentValue.toFixed(2)}</span>
        </div>
      </div>
      <div className="h-48">
        <Line data={data} options={options} />
      </div>
    </div>
  );
};
