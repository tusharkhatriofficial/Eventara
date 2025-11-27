import { ErrorBreakdown } from '../../types';

interface CriticalErrorsAlertProps {
  errorsByType: ErrorBreakdown[];
  threshold?: number;
}

export const CriticalErrorsAlert: React.FC<CriticalErrorsAlertProps> = ({ 
  errorsByType, 
  threshold = 25 
}) => {
  const criticalErrors = errorsByType.filter(error => error.percentage >= threshold);

  if (criticalErrors.length === 0) {
    return (
      <div className="bg-green-50 border border-green-200 rounded-lg p-6">
        <div className="flex items-center gap-3">
          <svg className="w-12 h-12 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          <div>
            <h3 className="text-lg font-semibold text-green-900">No Critical Errors</h3>
            <p className="text-sm text-green-700 mt-1">
              All errors are below the {threshold}% threshold
            </p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="bg-red-50 border-2 border-red-300 rounded-lg p-6">
      <div className="flex items-start gap-3 mb-4">
        <svg className="w-8 h-8 text-red-600 flex-shrink-0 mt-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
        </svg>
        <div className="flex-1">
          <h3 className="text-lg font-semibold text-red-900">
            {criticalErrors.length} Critical Error{criticalErrors.length > 1 ? 's' : ''} Detected
          </h3>
          <p className="text-sm text-red-700 mt-1">
            The following errors are affecting more than {threshold}% of total events
          </p>
        </div>
      </div>

      <div className="space-y-3">
        {criticalErrors.map((error, index) => (
          <div key={index} className="bg-white p-4 rounded-lg border border-red-200">
            <div className="flex items-center justify-between mb-2">
              <span className="text-sm font-semibold text-gray-900">{error.name}</span>
              <span className="px-3 py-1 bg-red-600 text-white text-xs font-bold rounded-full">
                {error.percentage.toFixed(1)}%
              </span>
            </div>
            <div className="flex items-center gap-4 text-xs text-gray-600">
              <span>{error.count.toLocaleString()} occurrences</span>
              <span>•</span>
              <span className="text-red-700 font-semibold">Immediate attention required</span>
            </div>
          </div>
        ))}
      </div>

      <div className="mt-4 pt-4 border-t border-red-200">
        <p className="text-xs text-red-800 font-semibold">
          Recommended Actions:
        </p>
        <ul className="text-xs text-red-700 mt-2 space-y-1">
          <li>• Review error logs for root cause analysis</li>
          <li>• Check related services and dependencies</li>
          <li>• Consider implementing circuit breakers</li>
          <li>• Alert on-call engineers if errors persist</li>
        </ul>
      </div>
    </div>
  );
};
