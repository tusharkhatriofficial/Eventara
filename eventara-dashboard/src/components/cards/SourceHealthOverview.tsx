import { SourceMetrics } from '../../types';

interface SourceHealthOverviewProps {
  eventsBySource: Record<string, SourceMetrics>;
}

export const SourceHealthOverview: React.FC<SourceHealthOverviewProps> = ({ eventsBySource }) => {
  const sources = Object.entries(eventsBySource);
  
  const healthCounts = {
    healthy: sources.filter(([, s]) => s.health === 'healthy').length,
    degraded: sources.filter(([, s]) => s.health === 'degraded').length,
    down: sources.filter(([, s]) => s.health === 'down').length,
  };

  const totalSources = sources.length;

  return (
    <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
      {/* Total Sources */}
      <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-200">
        <div className="flex items-center justify-between">
          <div>
            <p className="text-sm font-medium text-gray-600">Total Sources</p>
            <p className="text-3xl font-bold text-gray-900 mt-2">{totalSources}</p>
          </div>
          <div className="p-3 bg-blue-50 rounded-lg">
            <svg className="w-8 h-8 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 12h14M5 12a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v4a2 2 0 01-2 2M5 12a2 2 0 00-2 2v4a2 2 0 002 2h14a2 2 0 002-2v-4a2 2 0 00-2-2m-2-4h.01M17 16h.01" />
            </svg>
          </div>
        </div>
      </div>

      {/* Healthy Sources */}
      <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-200">
        <div className="flex items-center justify-between">
          <div>
            <p className="text-sm font-medium text-gray-600">Healthy</p>
            <p className="text-3xl font-bold text-green-600 mt-2">{healthCounts.healthy}</p>
            <p className="text-xs text-gray-500 mt-1">
              {totalSources > 0 ? Math.round((healthCounts.healthy / totalSources) * 100) : 0}%
            </p>
          </div>
          <div className="p-3 bg-green-50 rounded-lg">
            <svg className="w-8 h-8 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          </div>
        </div>
      </div>

      {/* Degraded Sources */}
      <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-200">
        <div className="flex items-center justify-between">
          <div>
            <p className="text-sm font-medium text-gray-600">Degraded</p>
            <p className="text-3xl font-bold text-yellow-600 mt-2">{healthCounts.degraded}</p>
            <p className="text-xs text-gray-500 mt-1">
              {totalSources > 0 ? Math.round((healthCounts.degraded / totalSources) * 100) : 0}%
            </p>
          </div>
          <div className="p-3 bg-yellow-50 rounded-lg">
            <svg className="w-8 h-8 text-yellow-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
            </svg>
          </div>
        </div>
      </div>

      {/* Down Sources */}
      <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-200">
        <div className="flex items-center justify-between">
          <div>
            <p className="text-sm font-medium text-gray-600">Down</p>
            <p className="text-3xl font-bold text-red-600 mt-2">{healthCounts.down}</p>
            <p className="text-xs text-gray-500 mt-1">
              {totalSources > 0 ? Math.round((healthCounts.down / totalSources) * 100) : 0}%
            </p>
          </div>
          <div className="p-3 bg-red-50 rounded-lg">
            <svg className="w-8 h-8 text-red-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          </div>
        </div>
      </div>
    </div>
  );
};
