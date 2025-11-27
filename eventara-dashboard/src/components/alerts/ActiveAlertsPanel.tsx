import { AnomalyAlert } from '../../types';
import { useState } from 'react';

interface ActiveAlertsPanelProps {
  anomalies: AnomalyAlert[];
}

export const ActiveAlertsPanel: React.FC<ActiveAlertsPanelProps> = ({ anomalies }) => {
  const [filterSeverity, setFilterSeverity] = useState<'all' | 'critical' | 'warning' | 'info'>('all');

  const filteredAnomalies = filterSeverity === 'all'
    ? anomalies
    : anomalies.filter(a => a.severity === filterSeverity);

  const severityConfig = {
    critical: {
      bg: 'bg-red-50',
      border: 'border-red-300',
      text: 'text-red-700',
      badge: 'bg-red-600 text-white',
      icon: '🔴'
    },
    warning: {
      bg: 'bg-yellow-50',
      border: 'border-yellow-300',
      text: 'text-yellow-700',
      badge: 'bg-yellow-600 text-white',
      icon: '🟡'
    },
    info: {
      bg: 'bg-blue-50',
      border: 'border-blue-300',
      text: 'text-blue-700',
      badge: 'bg-blue-600 text-white',
      icon: '🔵'
    }
  };

  return (
    <div className="bg-white rounded-lg shadow-sm border border-gray-200">
      {/* Header with Filters */}
      <div className="p-6 border-b border-gray-200">
        <div className="flex items-center justify-between">
          <div>
            <h3 className="text-lg font-semibold text-gray-900">Active Alerts</h3>
            <p className="text-sm text-gray-500 mt-1">
              Real-time anomaly detection and system alerts
            </p>
          </div>
          <div className="flex items-center gap-2">
            <button
              onClick={() => setFilterSeverity('all')}
              className={`px-3 py-1 text-xs font-semibold rounded-full ${
                filterSeverity === 'all'
                  ? 'bg-gray-900 text-white'
                  : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
              }`}
            >
              All
            </button>
            <button
              onClick={() => setFilterSeverity('critical')}
              className={`px-3 py-1 text-xs font-semibold rounded-full ${
                filterSeverity === 'critical'
                  ? 'bg-red-600 text-white'
                  : 'bg-red-100 text-red-600 hover:bg-red-200'
              }`}
            >
              Critical
            </button>
            <button
              onClick={() => setFilterSeverity('warning')}
              className={`px-3 py-1 text-xs font-semibold rounded-full ${
                filterSeverity === 'warning'
                  ? 'bg-yellow-600 text-white'
                  : 'bg-yellow-100 text-yellow-600 hover:bg-yellow-200'
              }`}
            >
              Warning
            </button>
            <button
              onClick={() => setFilterSeverity('info')}
              className={`px-3 py-1 text-xs font-semibold rounded-full ${
                filterSeverity === 'info'
                  ? 'bg-blue-600 text-white'
                  : 'bg-blue-100 text-blue-600 hover:bg-blue-200'
              }`}
            >
              Info
            </button>
          </div>
        </div>
      </div>

      {/* Alerts List */}
      <div className="divide-y divide-gray-200 max-h-[600px] overflow-y-auto">
        {filteredAnomalies.length > 0 ? (
          filteredAnomalies.map((anomaly, index) => {
            const config = severityConfig[anomaly.severity];
            return (
              <div key={index} className={`p-6 hover:bg-gray-50 ${config.bg} border-l-4 ${config.border}`}>
                <div className="flex items-start gap-4">
                  {/* Icon */}
                  <div className="text-3xl flex-shrink-0">
                    {config.icon}
                  </div>

                  {/* Content */}
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-3 mb-2">
                      <span className={`px-3 py-1 text-xs font-bold rounded-full uppercase ${config.badge}`}>
                        {anomaly.severity}
                      </span>
                      <span className="px-3 py-1 text-xs font-semibold rounded-full bg-gray-100 text-gray-700 uppercase">
                        {anomaly.type.replace(/_/g, ' ')}
                      </span>
                      <span className="text-xs text-gray-500">
                        {new Date(anomaly.detectedAt).toLocaleString()}
                      </span>
                    </div>

                    <p className={`text-sm font-semibold ${config.text} mb-3`}>
                      {anomaly.message}
                    </p>

                    <div className="grid grid-cols-2 gap-4">
                      <div className="bg-white p-3 rounded border border-gray-200">
                        <p className="text-xs text-gray-600 mb-1">Threshold</p>
                        <p className="text-lg font-bold text-gray-900">
                          {anomaly.threshold}
                        </p>
                      </div>
                      <div className="bg-white p-3 rounded border border-gray-200">
                        <p className="text-xs text-gray-600 mb-1">Current Value</p>
                        <p className={`text-lg font-bold ${config.text}`}>
                          {anomaly.currentValue.toFixed(2)}
                        </p>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            );
          })
        ) : (
          <div className="p-12 text-center">
            <svg className="w-16 h-16 text-gray-300 mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <p className="text-gray-500 font-medium">
              {filterSeverity === 'all' ? 'No active alerts' : `No ${filterSeverity} alerts`}
            </p>
            <p className="text-sm text-gray-400 mt-1">
              System is running normally
            </p>
          </div>
        )}
      </div>

      {/* Footer */}
      {filteredAnomalies.length > 0 && (
        <div className="p-4 bg-gray-50 border-t border-gray-200 text-center">
          <p className="text-sm text-gray-600">
            Showing {filteredAnomalies.length} of {anomalies.length} alerts
          </p>
        </div>
      )}
    </div>
  );
};
