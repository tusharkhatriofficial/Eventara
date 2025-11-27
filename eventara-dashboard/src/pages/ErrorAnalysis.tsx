import { ComprehensiveMetrics } from '../types';
import { ErrorOverviewCards } from '../components/cards/ErrorOverviewCards';
import { ErrorBreakdownChart } from '../components/charts/ErrorBreakdownChart';
import { ErrorDetailsTable } from '../components/tables/ErrorDetailsTable';
import { CriticalErrorsAlert } from '../components/alerts/CriticalErrorsAlert';

interface ErrorAnalysisProps {
  metrics: ComprehensiveMetrics | null;
}

export const ErrorAnalysis: React.FC<ErrorAnalysisProps> = ({ metrics }) => {
  if (!metrics) {
    return (
      <div className="flex items-center justify-center h-full">
        <div className="text-center">
          <div className="inline-block animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mb-4"></div>
          <p className="text-gray-600">Loading error analysis...</p>
        </div>
      </div>
    );
  }

  const hasErrors = metrics.errorAnalysis.totalErrors > 0;

  return (
    <div className="space-y-6">
      {/* Page Title */}
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Error Analysis</h1>
        <p className="text-sm text-gray-500 mt-1">
          Comprehensive error tracking, debugging, and resolution insights
        </p>
      </div>

      {/* Overview Cards */}
      <ErrorOverviewCards 
        errorAnalysis={metrics.errorAnalysis}
        totalEvents={metrics.summary.totalEvents}
      />

      {/* Critical Errors Alert */}
      {hasErrors && (
        <CriticalErrorsAlert 
          errorsByType={metrics.errorAnalysis.errorsByType}
          threshold={25}
        />
      )}

      {/* Error Breakdown Charts */}
      {hasErrors && (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <ErrorBreakdownChart 
            errors={metrics.errorAnalysis.errorsByType}
            title="Errors by Type"
          />
          <ErrorBreakdownChart 
            errors={metrics.errorAnalysis.errorsBySource}
            title="Errors by Source"
          />
        </div>
      )}

      {/* Error Details Table */}
      {hasErrors && (
        <ErrorDetailsTable 
          errorsByType={metrics.errorAnalysis.errorsByType}
          errorsBySource={metrics.errorAnalysis.errorsBySource}
        />
      )}

      {/* Error Rate Trend */}
      {hasErrors && (
        <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-200">
          <h3 className="text-lg font-semibold text-gray-900 mb-4">Error Rate Analysis</h3>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            <div className="text-center p-4 bg-gray-50 rounded-lg">
              <p className="text-sm text-gray-600 mb-2">Total Errors</p>
              <p className="text-3xl font-bold text-red-600">
                {metrics.errorAnalysis.totalErrors.toLocaleString()}
              </p>
            </div>
            <div className="text-center p-4 bg-gray-50 rounded-lg">
              <p className="text-sm text-gray-600 mb-2">Error Rate</p>
              <p className="text-3xl font-bold text-orange-600">
                {metrics.errorAnalysis.errorRate.toFixed(2)}%
              </p>
            </div>
            <div className="text-center p-4 bg-gray-50 rounded-lg">
              <p className="text-sm text-gray-600 mb-2">Unique Error Types</p>
              <p className="text-3xl font-bold text-gray-900">
                {metrics.errorAnalysis.errorsByType.length}
              </p>
            </div>
          </div>
        </div>
      )}

      {/* No Errors State */}
      {!hasErrors && (
        <div className="bg-green-50 border border-green-200 rounded-lg p-12 text-center">
          <svg className="w-20 h-20 text-green-400 mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          <h3 className="text-2xl font-bold text-green-900 mb-2">No Errors Detected</h3>
          <p className="text-green-700 mb-6">
            Your system is running smoothly with a 100% success rate
          </p>
          <div className="inline-flex items-center gap-2 px-4 py-2 bg-green-100 rounded-lg">
            <svg className="w-5 h-5 text-green-600" fill="currentColor" viewBox="0 0 20 20">
              <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" />
            </svg>
            <span className="text-sm font-medium text-green-800">All systems operational</span>
          </div>
        </div>
      )}

      {/* Debugging Tips */}
      {hasErrors && (
        <div className="bg-blue-50 border border-blue-200 rounded-lg p-6">
          <h3 className="text-lg font-semibold text-blue-900 mb-4">Debugging Tips</h3>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="bg-white p-4 rounded-lg">
              <div className="flex items-start gap-3">
                <span className="text-2xl"></span>
                <div>
                  <h4 className="font-semibold text-gray-900 text-sm mb-1">Identify Patterns</h4>
                  <p className="text-xs text-gray-600">
                    Look for correlations between error types, sources, and time periods
                  </p>
                </div>
              </div>
            </div>
            <div className="bg-white p-4 rounded-lg">
              <div className="flex items-start gap-3">
                <span className="text-2xl"></span>
                <div>
                  <h4 className="font-semibold text-gray-900 text-sm mb-1">Monitor Trends</h4>
                  <p className="text-xs text-gray-600">
                    Track error rates over time to identify spikes or degradation
                  </p>
                </div>
              </div>
            </div>
            <div className="bg-white p-4 rounded-lg">
              <div className="flex items-start gap-3">
                <span className="text-2xl"></span>
                <div>
                  <h4 className="font-semibold text-gray-900 text-sm mb-1">Fix Root Causes</h4>
                  <p className="text-xs text-gray-600">
                    Address the underlying issues rather than symptoms
                  </p>
                </div>
              </div>
            </div>
            <div className="bg-white p-4 rounded-lg">
              <div className="flex items-start gap-3">
                <span className="text-2xl"></span>
                <div>
                  <h4 className="font-semibold text-gray-900 text-sm mb-1">Set Up Alerts</h4>
                  <p className="text-xs text-gray-600">
                    Configure alerts for critical error thresholds
                  </p>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
