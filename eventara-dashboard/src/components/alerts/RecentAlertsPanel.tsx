import { AnomalyAlert } from '../../types';

interface RecentAlertsPanelProps {
  alerts: AnomalyAlert[];
}

const severityConfig = {
  critical: {
    bg: 'bg-error-50',
    text: 'text-error-700',
    border: 'border-l-error-500',
    badge: 'bg-error-100 text-error-700 ring-error-500/20',
  },
  warning: {
    bg: 'bg-warning-50',
    text: 'text-warning-700',
    border: 'border-l-warning-500',
    badge: 'bg-warning-100 text-warning-700 ring-warning-500/20',
  },
  info: {
    bg: 'bg-blue-50',
    text: 'text-blue-700',
    border: 'border-l-blue-500',
    badge: 'bg-blue-100 text-blue-700 ring-blue-500/20',
  }
};

export const RecentAlertsPanel: React.FC<RecentAlertsPanelProps> = ({ alerts }) => {
  const topAlerts = alerts.slice(0, 3);

  return (
    <div className="card p-6">
      <div className="flex items-center justify-between mb-4">
        <h3 className="text-lg font-semibold text-slate-900 flex items-center gap-2">
          <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-warning-500 to-warning-600 flex items-center justify-center text-white shadow-lg shadow-warning-500/25">
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
            </svg>
          </div>
          Recent Alerts
        </h3>
        <span className="badge badge-neutral">{alerts.length} active</span>
      </div>

      {topAlerts.length > 0 ? (
        <div className="space-y-3">
          {topAlerts.map((alert, index) => {
            const config = severityConfig[alert.severity] || severityConfig.info;
            return (
              <div
                key={index}
                className={`
                  p-4 rounded-xl border-l-4 ${config.bg} ${config.border}
                  transition-all duration-200 hover:shadow-sm
                `}
              >
                <div className="flex items-start justify-between gap-3">
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 mb-1.5">
                      <span className={`
                        px-2 py-0.5 rounded-full text-xs font-semibold uppercase
                        ring-1 ${config.badge}
                      `}>
                        {alert.severity}
                      </span>
                      <span className="text-xs text-slate-500">
                        {new Date(alert.detectedAt).toLocaleTimeString()}
                      </span>
                    </div>
                    <p className="text-sm font-medium text-slate-900 line-clamp-1">{alert.message}</p>
                    <p className="text-xs text-slate-500 mt-1">
                      Threshold: {alert.threshold} • Current: {alert.currentValue.toFixed(2)}
                    </p>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      ) : (
        <div className="empty-state py-8">
          <div className="empty-state-icon">
            <svg className="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          </div>
          <p className="text-sm text-slate-500">No active alerts</p>
        </div>
      )}
    </div>
  );
};
