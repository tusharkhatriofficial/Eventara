interface SystemHealthBadgeProps {
  health: 'healthy' | 'degraded' | 'critical' | string;
}

export const SystemHealthBadge: React.FC<SystemHealthBadgeProps> = ({ health }) => {
  const getHealthConfig = () => {
    switch (health.toLowerCase()) {
      case 'healthy':
        return {
          label: 'Healthy',
          bgColor: 'bg-success-50',
          textColor: 'text-success-700',
          dotColor: 'bg-success-500',
          ringColor: 'ring-success-500/20',
        };
      case 'degraded':
        return {
          label: 'Degraded',
          bgColor: 'bg-warning-50',
          textColor: 'text-warning-700',
          dotColor: 'bg-warning-500',
          ringColor: 'ring-warning-500/20',
        };
      case 'critical':
        return {
          label: 'Critical',
          bgColor: 'bg-error-50',
          textColor: 'text-error-700',
          dotColor: 'bg-error-500',
          ringColor: 'ring-error-500/20',
        };
      default:
        return {
          label: health,
          bgColor: 'bg-slate-50',
          textColor: 'text-slate-700',
          dotColor: 'bg-slate-500',
          ringColor: 'ring-slate-500/20',
        };
    }
  };

  const config = getHealthConfig();

  return (
    <div className={`
      inline-flex items-center gap-2 px-4 py-2 rounded-full
      ${config.bgColor} ring-1 ${config.ringColor}
      transition-all duration-200 hover:shadow-sm
    `}>
      <span className={`
        relative flex h-2.5 w-2.5
      `}>
        <span className={`
          animate-ping absolute inline-flex h-full w-full rounded-full opacity-75
          ${config.dotColor}
        `}></span>
        <span className={`
          relative inline-flex rounded-full h-2.5 w-2.5
          ${config.dotColor}
        `}></span>
      </span>
      <span className={`text-sm font-semibold ${config.textColor}`}>
        {config.label}
      </span>
    </div>
  );
};
