import { AnomalyAlert } from '../../types';

interface ActiveAnomaliesAlertProps {
  anomalies: AnomalyAlert[];
}

const severityConfig = {
  critical: {
    dot: 'bg-error-500',
    text: 'text-error-700',
    badge: 'bg-error-100 text-error-700',
  },
  warning: {
    dot: 'bg-warning-500',
    text: 'text-warning-700',
    badge: 'bg-warning-100 text-warning-700',
  },
  info: {
    dot: 'bg-blue-500',
    text: 'text-blue-700',
    badge: 'bg-blue-100 text-blue-700',
  },
};

export const ActiveAnomaliesAlert: React.FC<ActiveAnomaliesAlertProps> = ({ anomalies }) => {
  if (anomalies.length === 0) {
    return (
      <div className="card p-8">
        <div className="flex flex-col items-center justify-center text-center">
          <div className="w-16 h-16 rounded-2xl bg-success-50 flex items-center justify-center mb-4">
            <svg className="w-8 h-8 text-success-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          </div>
          <p className="text-lg font-semibold text-slate-900">All Systems Normal</p>
          <p className="text-sm text-slate-500 mt-1">No active anomalies detected</p>
        </div>
      </div>
    );
  }

  const criticalCount = anomalies.filter(a => a.severity === 'critical').length;
  const warningCount = anomalies.filter(a => a.severity === 'warning').length;

  return (
    <div className="card overflow-hidden">
      <div className="p-6 border-b border-slate-100">
        <div className="flex items-center justify-between">
          <h3 className="text-lg font-semibold text-slate-900 flex items-center gap-2">
            <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-error-500 to-error-600 flex items-center justify-center text-white shadow-lg shadow-error-500/25">
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
              </svg>
            </div>
            Active Anomalies
          </h3>
          <div className="flex items-center gap-2">
            {criticalCount > 0 && (
              <span className="badge badge-error">
                {criticalCount} Critical
              </span>
            )}
            {warningCount > 0 && (
              <span className="badge badge-warning">
                {warningCount} Warning
              </span>
            )}
          </div>
        </div>
      </div>

      <div className="divide-y divide-slate-100 max-h-96 overflow-y-auto scrollbar-thin">
        {anomalies.map((anomaly, index) => {
          const config = severityConfig[anomaly.severity] || severityConfig.info;
          return (
            <div key={index} className="p-4 hover:bg-slate-50 transition-colors">
              <div className="flex items-start gap-3">
                <div className={`mt-1.5 w-2.5 h-2.5 rounded-full ${config.dot} flex-shrink-0`}></div>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2 mb-1">
                    <span className={`text-xs font-semibold uppercase ${config.text}`}>
                      {anomaly.severity}
                    </span>
                    <span className="text-xs text-slate-400">
                      {new Date(anomaly.detectedAt).toLocaleTimeString()}
                    </span>
                  </div>
                  <p className="text-sm font-medium text-slate-900">{anomaly.message}</p>
                  <div className="flex flex-wrap items-center gap-3 mt-2 text-xs text-slate-500">
                    <span>Type: <span className="font-medium text-slate-700">{anomaly.type}</span></span>
                    <span>Threshold: <span className="font-medium text-slate-700">{anomaly.threshold}</span></span>
                    <span>Current: <span className="font-medium text-slate-700">{anomaly.currentValue.toFixed(2)}</span></span>
                  </div>
                </div>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};
