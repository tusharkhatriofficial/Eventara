import { Pie, Bar } from 'react-chartjs-2';
import { AnomalyAlert } from '../../types';

interface AlertStatisticsChartsProps {
  anomalies: AnomalyAlert[];
}

export const AlertSeverityChart: React.FC<{ anomalies: AnomalyAlert[] }> = ({ anomalies }) => {
  const criticalCount = anomalies.filter(a => a.severity === 'critical').length;
  const warningCount = anomalies.filter(a => a.severity === 'warning').length;
  const infoCount = anomalies.filter(a => a.severity === 'info').length;

  const data = {
    labels: ['Critical', 'Warning', 'Info'],
    datasets: [
      {
        data: [criticalCount, warningCount, infoCount],
        backgroundColor: [
          'rgba(239, 68, 68, 0.8)',
          'rgba(245, 158, 11, 0.8)',
          'rgba(59, 130, 246, 0.8)',
        ],
        borderColor: [
          'rgb(239, 68, 68)',
          'rgb(245, 158, 11)',
          'rgb(59, 130, 246)',
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
      },
    },
  };

  return (
    <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-200">
      <h3 className="text-lg font-semibold text-gray-900 mb-4">
        Alerts by Severity
      </h3>
      <div className="h-64">
        <Pie data={data} options={options} />
      </div>
    </div>
  );
};

export const AlertTypeChart: React.FC<{ anomalies: AnomalyAlert[] }> = ({ anomalies }) => {
  const typeCounts: Record<string, number> = {};
  anomalies.forEach(a => {
    typeCounts[a.type] = (typeCounts[a.type] || 0) + 1;
  });

  const types = Object.keys(typeCounts);
  const counts = Object.values(typeCounts);

  const data = {
    labels: types.map(t => t.replace(/_/g, ' ').toUpperCase()),
    datasets: [
      {
        label: 'Alert Count',
        data: counts,
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
    },
    scales: {
      y: {
        beginAtZero: true,
        ticks: {
          precision: 0,
        },
      },
    },
  };

  return (
    <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-200">
      <h3 className="text-lg font-semibold text-gray-900 mb-4">
        Alerts by Type
      </h3>
      <div className="h-64">
        <Bar data={data} options={options} />
      </div>
    </div>
  );
};
