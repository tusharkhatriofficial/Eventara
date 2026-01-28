interface CircularGaugeProps {
  label: string;
  value: number;
  max: number;
  unit?: string;
  color?: string;
}

export const CircularGauge: React.FC<CircularGaugeProps> = ({
  label,
  value,
  max,
  unit = '',
  color = '#6366f1'
}) => {
  const percentage = Math.min((value / max) * 100, 100);
  const circumference = 2 * Math.PI * 54; // radius = 54
  const strokeDashoffset = circumference - (percentage / 100) * circumference;

  // Determine status based on percentage
  const getStatusColor = () => {
    if (percentage >= 90) return '#f43f5e'; // Error
    if (percentage >= 75) return '#f59e0b'; // Warning
    return color;
  };

  const activeColor = getStatusColor();

  return (
    <div className="card p-6 flex flex-col items-center group">
      {/* Gauge */}
      <div className="relative w-36 h-36 mb-4">
        <svg className="w-full h-full transform -rotate-90" viewBox="0 0 120 120">
          {/* Background circle */}
          <circle
            cx="60"
            cy="60"
            r="54"
            fill="none"
            stroke="currentColor"
            strokeWidth="8"
            className="text-slate-100"
          />
          {/* Progress circle */}
          <circle
            cx="60"
            cy="60"
            r="54"
            fill="none"
            stroke={activeColor}
            strokeWidth="8"
            strokeLinecap="round"
            strokeDasharray={circumference}
            strokeDashoffset={strokeDashoffset}
            className="transition-all duration-700 ease-out"
            style={{
              filter: `drop-shadow(0 0 6px ${activeColor}40)`
            }}
          />
        </svg>

        {/* Center content */}
        <div className="absolute inset-0 flex flex-col items-center justify-center">
          <span className="text-3xl font-bold text-slate-900 tracking-tight">
            {typeof value === 'number' ? value.toFixed(1) : value}
          </span>
          {unit && (
            <span className="text-sm text-slate-500 mt-0.5">{unit}</span>
          )}
        </div>
      </div>

      {/* Label */}
      <p className="text-sm font-medium text-slate-600">{label}</p>

      {/* Progress bar alternative representation */}
      <div className="w-full mt-4">
        <div className="flex justify-between text-xs text-slate-400 mb-1">
          <span>0</span>
          <span>{max}</span>
        </div>
        <div className="h-1.5 bg-slate-100 rounded-full overflow-hidden">
          <div
            className="h-full rounded-full transition-all duration-700 ease-out"
            style={{
              width: `${percentage}%`,
              backgroundColor: activeColor,
            }}
          />
        </div>
      </div>
    </div>
  );
};
