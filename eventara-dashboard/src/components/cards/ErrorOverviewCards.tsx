import { ErrorAnalysisMetrics } from '../../types';

interface ErrorOverviewCardsProps {
  errorAnalysis: ErrorAnalysisMetrics;
  totalEvents: number;
}

export const ErrorOverviewCards: React.FC<ErrorOverviewCardsProps> = ({ errorAnalysis, totalEvents }) => {
  const successfulEvents = totalEvents - errorAnalysis.totalErrors;
  const successRate = totalEvents > 0 ? ((successfulEvents / totalEvents) * 100) : 100;

  const getErrorRateStatus = (rate: number) => {
    if (rate < 1) return { color: 'green', label: 'Excellent', icon: '✓' };
    if (rate < 5) return { color: 'yellow', label: 'Warning', icon: '!' };
    return { color: 'red', label: 'Critical', icon: '⚠' };
  };

  const status = getErrorRateStatus(errorAnalysis.errorRate);

  return (
    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
      {/* Total Errors */}
      <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-200">
        <div className="flex items-center justify-between">
          <div>
            <p className="text-sm font-medium text-gray-600">Total Errors</p>
            <p className="text-3xl font-bold text-red-600 mt-2">
              {errorAnalysis.totalErrors.toLocaleString()}
            </p>
            <p className="text-xs text-gray-500 mt-1">All time</p>
          </div>
          <div className="p-3 bg-red-50 rounded-lg">
            <svg className="w-8 h-8 text-red-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          </div>
        </div>
      </div>

      {/* Error Rate */}
      <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-200">
        <div className="flex items-center justify-between">
          <div>
            <p className="text-sm font-medium text-gray-600">Error Rate</p>
            <p className={`text-3xl font-bold mt-2 text-${status.color}-600`}>
              {errorAnalysis.errorRate.toFixed(2)}%
            </p>
            <p className={`text-xs mt-1 text-${status.color}-600 font-semibold`}>
              {status.icon} {status.label}
            </p>
          </div>
          <div className={`p-3 bg-${status.color}-50 rounded-lg`}>
            <svg className={`w-8 h-8 text-${status.color}-600`} fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
            </svg>
          </div>
        </div>
      </div>

      {/* Success Rate */}
      <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-200">
        <div className="flex items-center justify-between">
          <div>
            <p className="text-sm font-medium text-gray-600">Success Rate</p>
            <p className="text-3xl font-bold text-green-600 mt-2">
              {successRate.toFixed(2)}%
            </p>
            <p className="text-xs text-gray-500 mt-1">
              {successfulEvents.toLocaleString()} successful
            </p>
          </div>
          <div className="p-3 bg-green-50 rounded-lg">
            <svg className="w-8 h-8 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          </div>
        </div>
      </div>

      {/* Error Types */}
      <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-200">
        <div className="flex items-center justify-between">
          <div>
            <p className="text-sm font-medium text-gray-600">Error Types</p>
            <p className="text-3xl font-bold text-gray-900 mt-2">
              {errorAnalysis.errorsByType.length}
            </p>
            <p className="text-xs text-gray-500 mt-1">Unique types</p>
          </div>
          <div className="p-3 bg-purple-50 rounded-lg">
            <svg className="w-8 h-8 text-purple-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 12V7a4 4 0 014-4z" />
            </svg>
          </div>
        </div>
      </div>
    </div>
  );
};
