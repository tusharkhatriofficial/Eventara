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
          <div className="w-16 h-16 rounded-2xl bg-gradient-to-br from-red-500 to-rose-600 flex items-center justify-center mb-4 shadow-lg shadow-red-500/30 animate-pulse">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-white"></div>
          </div>
          <p className="text-dark-600 font-medium">Loading error analysis...</p>
        </div>
      </div>
    );
  }


  const hasErrors = metrics.errorAnalysis.totalErrors > 0;


  return (
    <div className="space-y-6 animate-fade-in">
      {/* Page Title */}
      <div className="card-gradient p-8">
        <div className="flex items-center gap-4">
          <div className="w-12 h-12 rounded-2xl bg-gradient-to-br from-red-500 to-rose-600 flex items-center justify-center shadow-lg shadow-red-500/30">
            <svg className="w-7 h-7 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          </div>
          <div>
            <h1 className="text-3xl font-bold text-gradient">Error Analysis</h1>
            <p className="text-sm text-dark-600 mt-1">
              Comprehensive error tracking, debugging, and resolution insights
            </p>
          </div>
        </div>
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
        <div className="card-gradient p-8">
          <h3 className="text-2xl font-bold text-dark-900 mb-6 flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-red-500 to-rose-600 flex items-center justify-center">
              <svg className="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
              </svg>
            </div>
            Error Rate Analysis
          </h3>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            <div className="stat-card text-center">
              <p className="text-sm font-semibold text-dark-600 mb-3">Total Errors</p>
              <p className="text-4xl font-bold text-gradient">
                {metrics.errorAnalysis.totalErrors.toLocaleString()}
              </p>
            </div>
            <div className="stat-card text-center">
              <p className="text-sm font-semibold text-dark-600 mb-3">Error Rate</p>
              <p className="text-4xl font-bold text-gradient">
                {metrics.errorAnalysis.errorRate.toFixed(2)}%
              </p>
            </div>
            <div className="stat-card text-center">
              <p className="text-sm font-semibold text-dark-600 mb-3">Unique Error Types</p>
              <p className="text-4xl font-bold text-gradient">
                {metrics.errorAnalysis.errorsByType.length}
              </p>
            </div>
          </div>
        </div>
      )}


      {/* No Errors State */}
      {!hasErrors && (
        <div className="card-gradient p-12 text-center animate-scale-in">
          <div className="w-24 h-24 rounded-3xl bg-gradient-to-br from-emerald-500 to-green-600 flex items-center justify-center mx-auto mb-6 shadow-2xl shadow-emerald-500/40 animate-pulse">
            <svg className="w-14 h-14 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          </div>
          <h3 className="text-3xl font-bold text-gradient mb-3">No Errors Detected</h3>
          <p className="text-dark-600 mb-6 text-lg">
            System is running smoothly with no errors at this time
          </p>
          <div className="inline-flex items-center gap-3 px-6 py-3 bg-gradient-to-r from-emerald-50 to-green-50 rounded-2xl border border-emerald-200">
            <div className="w-3 h-3 bg-emerald-500 rounded-full animate-pulse shadow-lg shadow-emerald-500/50"></div>
            <div className="text-left">
              <div className="text-sm font-semibold text-emerald-800">Error Monitoring Active</div>
              <div className="text-xs text-emerald-600 mt-1">All systems operational</div>
            </div>
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
                <span className="text-2xl">🔍</span>
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
                <span className="text-2xl">📊</span>
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
                <span className="text-2xl">🔧</span>
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
                <span className="text-2xl">🔔</span>
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
