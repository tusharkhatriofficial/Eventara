interface LiveMetricDisplayProps {
  label: string;
  value: number | string;
  unit?: string;
  status?: 'normal' | 'warning' | 'critical';
  subtitle?: string;
}

const statusColors = {
  normal: {
    gradient: 'from-success-500 to-success-600',
    shadow: 'shadow-success-500/25',
    text: 'text-success-600',
    bg: 'bg-success-50',
    ring: 'ring-success-500/20',
  },
  warning: {
    gradient: 'from-warning-500 to-warning-600',
    shadow: 'shadow-warning-500/25',
    text: 'text-warning-600',
    bg: 'bg-warning-50',
    ring: 'ring-warning-500/20',
  },
  critical: {
    gradient: 'from-error-500 to-error-600',
    shadow: 'shadow-error-500/25',
    text: 'text-error-600',
    bg: 'bg-error-50',
    ring: 'ring-error-500/20',
  },
};

export const LiveMetricDisplay: React.FC<LiveMetricDisplayProps> = ({
  label,
  value,
  unit,
  status = 'normal',
  subtitle
}) => {
  const colors = statusColors[status];

  return (
    <div className="card p-6 relative overflow-hidden group">
      {/* Subtle gradient overlay on hover */}
      <div className={`
        absolute inset-0 bg-gradient-to-br ${colors.gradient} opacity-0 
        group-hover:opacity-[0.03] transition-opacity duration-300
      `}></div>

      <div className="relative">
        {/* Header with live indicator */}
        <div className="flex items-center justify-between mb-4">
          <p className="text-sm font-medium text-slate-500">{label}</p>
          <div className={`
            flex items-center gap-1.5 px-2 py-1 rounded-full
            ${colors.bg} ring-1 ${colors.ring}
          `}>
            <span className={`w-1.5 h-1.5 rounded-full ${colors.gradient} bg-gradient-to-r animate-pulse`}></span>
            <span className={`text-xs font-medium ${colors.text}`}>Live</span>
          </div>
        </div>

        {/* Value */}
        <div className="flex items-baseline gap-2">
          <span className="text-4xl font-bold text-slate-900 tracking-tight">
            {typeof value === 'number' ? value.toLocaleString() : value}
          </span>
          {unit && (
            <span className="text-lg text-slate-400 font-medium">{unit}</span>
          )}
        </div>

        {/* Subtitle */}
        {subtitle && (
          <p className="text-sm text-slate-500 mt-2">{subtitle}</p>
        )}
      </div>
    </div>
  );
};
