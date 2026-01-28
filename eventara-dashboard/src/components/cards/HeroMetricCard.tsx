interface HeroMetricCardProps {
  title: string;
  value: string | number;
  subtitle?: string;
  icon: React.ReactNode;
  trend?: {
    value: number;
    isPositive: boolean;
  };
  accentColor?: 'primary' | 'success' | 'warning' | 'error' | 'info';
}

const accentColors = {
  primary: 'from-primary-500 to-primary-600 shadow-primary-500/25',
  success: 'from-success-500 to-success-600 shadow-success-500/25',
  warning: 'from-warning-500 to-warning-600 shadow-warning-500/25',
  error: 'from-error-500 to-error-600 shadow-error-500/25',
  info: 'from-blue-500 to-cyan-500 shadow-blue-500/25',
};

export const HeroMetricCard: React.FC<HeroMetricCardProps> = ({
  title,
  value,
  subtitle,
  icon,
  trend,
  accentColor = 'primary'
}) => {
  return (
    <div className="stat-card group">
      <div className="flex items-start justify-between">
        <div className="flex-1 min-w-0">
          <p className="text-sm font-medium text-slate-500 mb-1">{title}</p>
          <p className="text-3xl font-bold text-slate-900 tracking-tight">
            {typeof value === 'number' ? value.toLocaleString() : value}
          </p>
          {subtitle && (
            <p className="text-sm text-slate-500 mt-1 truncate">{subtitle}</p>
          )}
          {trend && (
            <div className={`
              flex items-center gap-1.5 mt-2
              ${trend.isPositive ? 'text-success-600' : 'text-error-600'}
            `}>
              <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
                <path
                  fillRule="evenodd"
                  d={trend.isPositive
                    ? "M5.293 9.707a1 1 0 010-1.414l4-4a1 1 0 011.414 0l4 4a1 1 0 01-1.414 1.414L11 7.414V15a1 1 0 11-2 0V7.414L6.707 9.707a1 1 0 01-1.414 0z"
                    : "M14.707 10.293a1 1 0 010 1.414l-4 4a1 1 0 01-1.414 0l-4-4a1 1 0 111.414-1.414L9 12.586V5a1 1 0 012 0v7.586l2.293-2.293a1 1 0 011.414 0z"
                  }
                  clipRule="evenodd"
                />
              </svg>
              <span className="text-sm font-medium">{Math.abs(trend.value)}%</span>
            </div>
          )}
        </div>
        <div className={`
          w-12 h-12 rounded-xl flex items-center justify-center
          bg-gradient-to-br ${accentColors[accentColor]}
          text-white shadow-lg
          transition-transform duration-300 group-hover:scale-105
        `}>
          <div className="w-6 h-6">
            {icon}
          </div>
        </div>
      </div>
    </div>
  );
};
