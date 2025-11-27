import { PerformanceMetrics } from '../../types';

interface SLAComplianceCardProps {
  performance: PerformanceMetrics;
}

export const SLAComplianceCard: React.FC<SLAComplianceCardProps> = ({ performance }) => {
  const slaTargets = [
    { name: 'Excellent (< 50ms)', threshold: 50, met: performance.p95 < 50 },
    { name: 'Good (< 100ms)', threshold: 100, met: performance.p95 < 100 },
    { name: 'Acceptable (< 200ms)', threshold: 200, met: performance.p95 < 200 },
    { name: 'Needs Improvement (< 500ms)', threshold: 500, met: performance.p95 < 500 },
  ];

  const currentCompliance = slaTargets.find(target => target.met) || slaTargets[slaTargets.length - 1];

  return (
    <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-200">
      <h3 className="text-lg font-semibold text-gray-900 mb-4">SLA Compliance</h3>
      
      {/* Current Status */}
      <div className="mb-6 p-4 bg-blue-50 rounded-lg border border-blue-200">
        <div className="flex items-center justify-between mb-2">
          <span className="text-sm font-medium text-blue-900">Current P95 Latency</span>
          <span className="text-2xl font-bold text-blue-900">{performance.p95.toFixed(1)} ms</span>
        </div>
        <div className="text-xs text-blue-700">
          Meeting: <span className="font-semibold">{currentCompliance.name}</span>
        </div>
      </div>

      {/* SLA Targets */}
      <div className="space-y-3">
        {slaTargets.map((target, index) => (
          <div key={index} className="flex items-center gap-3">
            <div className={`flex-shrink-0 w-6 h-6 rounded-full flex items-center justify-center ${
              target.met ? 'bg-green-100' : 'bg-gray-100'
            }`}>
              {target.met ? (
                <svg className="w-4 h-4 text-green-600" fill="currentColor" viewBox="0 0 20 20">
                  <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
                </svg>
              ) : (
                <svg className="w-4 h-4 text-gray-400" fill="currentColor" viewBox="0 0 20 20">
                  <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clipRule="evenodd" />
                </svg>
              )}
            </div>
            <div className="flex-1">
              <div className="flex items-center justify-between">
                <span className={`text-sm ${target.met ? 'text-gray-900 font-medium' : 'text-gray-500'}`}>
                  {target.name}
                </span>
                <span className={`text-xs ${target.met ? 'text-green-600 font-semibold' : 'text-gray-400'}`}>
                  {target.met ? 'Met' : 'Not Met'}
                </span>
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* Recommendations */}
      <div className="mt-6 pt-6 border-t border-gray-200">
        <h4 className="text-sm font-semibold text-gray-900 mb-2">Recommendations</h4>
        <ul className="text-xs text-gray-600 space-y-1">
          {performance.p95 > 200 && (
            <li>• Consider caching frequently accessed data</li>
          )}
          {performance.p99 > 500 && (
            <li>• Investigate and optimize slow database queries</li>
          )}
          {performance.maxLatency > 1000 && (
            <li>• Add timeout mechanisms for long-running operations</li>
          )}
          {performance.avgLatency < 100 && (
            <li>• Performance is excellent - maintain current optimizations</li>
          )}
        </ul>
      </div>
    </div>
  );
};
