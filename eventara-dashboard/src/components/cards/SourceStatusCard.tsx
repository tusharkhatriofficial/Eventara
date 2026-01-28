import { SourceMetrics } from '../../types';

interface SourceStatusCardProps {
  sourceName: string;
  metrics: SourceMetrics;
}

export const SourceStatusCard: React.FC<SourceStatusCardProps> = ({
  sourceName,
  metrics
}) => {
  const isHealthy = metrics.count > 0;

  return (
    <div className="card p-5 group hover:shadow-glass-lg transition-all duration-300">
      <div className="flex items-start gap-4">
        {/* Status indicator */}
        <div className={`
          w-10 h-10 rounded-xl flex items-center justify-center flex-shrink-0
          ${isHealthy
            ? 'bg-gradient-to-br from-success-500 to-success-600 shadow-lg shadow-success-500/25'
            : 'bg-gradient-to-br from-slate-400 to-slate-500 shadow-lg shadow-slate-400/25'
          }
        `}>
          <svg className="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 12h14M5 12a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v4a2 2 0 01-2 2M5 12a2 2 0 00-2 2v4a2 2 0 002 2h14a2 2 0 002-2v-4a2 2 0 00-2-2m-2-4h.01M17 16h.01" />
          </svg>
        </div>

        {/* Content */}
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2 mb-1">
            <h4 className="font-semibold text-slate-900 truncate">{sourceName}</h4>
            <span className={`
              inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium
              ${isHealthy
                ? 'bg-success-50 text-success-700 ring-1 ring-success-500/20'
                : 'bg-slate-100 text-slate-600 ring-1 ring-slate-200'
              }
            `}>
              {isHealthy ? (
                <>
                  <span className="w-1.5 h-1.5 rounded-full bg-success-500 animate-pulse"></span>
                  Active
                </>
              ) : (
                'Inactive'
              )}
            </span>
          </div>

          <div className="grid grid-cols-2 gap-3 mt-3">
            <div className="p-2.5 rounded-lg bg-slate-50">
              <p className="text-xs text-slate-500">Events</p>
              <p className="text-lg font-bold text-slate-900">{metrics.count.toLocaleString()}</p>
            </div>
            <div className="p-2.5 rounded-lg bg-slate-50">
              <p className="text-xs text-slate-500">Avg Latency</p>
              <p className="text-lg font-bold text-slate-900">{metrics.avgLatency.toFixed(1)}ms</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
