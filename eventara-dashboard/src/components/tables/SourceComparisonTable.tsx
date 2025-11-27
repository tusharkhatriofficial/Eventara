import { SourceMetrics } from '../../types';
import { useState } from 'react';

interface SourceComparisonTableProps {
  eventsBySource: Record<string, SourceMetrics>;
}

type SortField = 'count' | 'avgLatency' | 'errorCount' | 'errorRate';
type SortDirection = 'asc' | 'desc';

export const SourceComparisonTable: React.FC<SourceComparisonTableProps> = ({ eventsBySource }) => {
  const [sortField, setSortField] = useState<SortField>('count');
  const [sortDirection, setSortDirection] = useState<SortDirection>('desc');

  const handleSort = (field: SortField) => {
    if (sortField === field) {
      setSortDirection(sortDirection === 'asc' ? 'desc' : 'asc');
    } else {
      setSortField(field);
      setSortDirection('desc');
    }
  };

  const sortedSources = Object.entries(eventsBySource).sort(([, a], [, b]) => {
    const aValue = a[sortField];
    const bValue = b[sortField];
    const multiplier = sortDirection === 'asc' ? 1 : -1;
    return (aValue - bValue) * multiplier;
  });

  const SortIcon = ({ field }: { field: SortField }) => {
    if (sortField !== field) return null;
    return (
      <svg className="w-4 h-4 inline ml-1" fill="currentColor" viewBox="0 0 20 20">
        <path
          fillRule="evenodd"
          d={sortDirection === 'asc'
            ? "M5.293 9.707a1 1 0 010-1.414l4-4a1 1 0 011.414 0l4 4a1 1 0 01-1.414 1.414L11 7.414V15a1 1 0 11-2 0V7.414L6.707 9.707a1 1 0 01-1.414 0z"
            : "M14.707 10.293a1 1 0 010 1.414l-4 4a1 1 0 01-1.414 0l-4-4a1 1 0 111.414-1.414L9 12.586V5a1 1 0 012 0v7.586l2.293-2.293a1 1 0 011.414 0z"
          }
          clipRule="evenodd"
        />
      </svg>
    );
  };

  return (
    <div className="bg-white rounded-lg shadow-sm border border-gray-200 overflow-hidden">
      <div className="overflow-x-auto">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Source
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Health
              </th>
              <th
                className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer hover:bg-gray-100"
                onClick={() => handleSort('count')}
              >
                Events <SortIcon field="count" />
              </th>
              <th
                className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer hover:bg-gray-100"
                onClick={() => handleSort('avgLatency')}
              >
                Avg Latency <SortIcon field="avgLatency" />
              </th>
              <th
                className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer hover:bg-gray-100"
                onClick={() => handleSort('errorCount')}
              >
                Errors <SortIcon field="errorCount" />
              </th>
              <th
                className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer hover:bg-gray-100"
                onClick={() => handleSort('errorRate')}
              >
                Error Rate <SortIcon field="errorRate" />
              </th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {sortedSources.map(([source, metrics]) => (
              <tr key={source} className="hover:bg-gray-50">
                <td className="px-6 py-4 whitespace-nowrap">
                  <div className="flex items-center gap-2">
                    <div className={`w-2 h-2 rounded-full ${
                      metrics.health === 'healthy' ? 'bg-green-500' :
                      metrics.health === 'degraded' ? 'bg-yellow-500' :
                      'bg-red-500'
                    }`}></div>
                    <span className="text-sm font-medium text-gray-900">{source}</span>
                  </div>
                </td>
                <td className="px-6 py-4 whitespace-nowrap">
                  <span className={`px-2 py-1 text-xs font-semibold rounded-full ${
                    metrics.health === 'healthy' ? 'bg-green-100 text-green-800' :
                    metrics.health === 'degraded' ? 'bg-yellow-100 text-yellow-800' :
                    'bg-red-100 text-red-800'
                  }`}>
                    {metrics.health}
                  </span>
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                  {metrics.count.toLocaleString()}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm">
                  <span className={
                    metrics.avgLatency < 100 ? 'text-green-600 font-semibold' :
                    metrics.avgLatency < 500 ? 'text-yellow-600 font-semibold' :
                    'text-red-600 font-semibold'
                  }>
                    {metrics.avgLatency.toFixed(1)} ms
                  </span>
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                  {metrics.errorCount.toLocaleString()}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm">
                  <span className={
                    metrics.errorRate < 1 ? 'text-green-600 font-semibold' :
                    metrics.errorRate < 5 ? 'text-yellow-600 font-semibold' :
                    'text-red-600 font-semibold'
                  }>
                    {metrics.errorRate.toFixed(1)}%
                  </span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
};
