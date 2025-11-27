import { ErrorBreakdown } from '../../types';
import { useState } from 'react';

interface ErrorDetailsTableProps {
  errorsByType: ErrorBreakdown[];
  errorsBySource: ErrorBreakdown[];
}

export const ErrorDetailsTable: React.FC<ErrorDetailsTableProps> = ({ errorsByType, errorsBySource }) => {
  const [activeTab, setActiveTab] = useState<'type' | 'source'>('type');

  const currentData = activeTab === 'type' ? errorsByType : errorsBySource;

  return (
    <div className="bg-white rounded-lg shadow-sm border border-gray-200">
      {/* Tabs */}
      <div className="border-b border-gray-200">
        <nav className="flex -mb-px">
          <button
            onClick={() => setActiveTab('type')}
            className={`px-6 py-4 text-sm font-medium border-b-2 ${
              activeTab === 'type'
                ? 'border-blue-500 text-blue-600'
                : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
            }`}
          >
            By Error Type
          </button>
          <button
            onClick={() => setActiveTab('source')}
            className={`px-6 py-4 text-sm font-medium border-b-2 ${
              activeTab === 'source'
                ? 'border-blue-500 text-blue-600'
                : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
            }`}
          >
            By Source
          </button>
        </nav>
      </div>

      {/* Table */}
      <div className="overflow-x-auto">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Rank
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                {activeTab === 'type' ? 'Error Type' : 'Source'}
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Error Count
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Percentage
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Impact
              </th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {currentData.length > 0 ? (
              currentData.map((error, index) => {
                const rank = index + 1;
                const impactLevel = error.percentage > 50 ? 'Critical' :
                                   error.percentage > 25 ? 'High' :
                                   error.percentage > 10 ? 'Medium' : 'Low';
                const impactColor = error.percentage > 50 ? 'text-red-700 bg-red-100' :
                                   error.percentage > 25 ? 'text-orange-700 bg-orange-100' :
                                   error.percentage > 10 ? 'text-yellow-700 bg-yellow-100' :
                                   'text-gray-700 bg-gray-100';

                return (
                  <tr key={error.name} className="hover:bg-gray-50">
                    <td className="px-6 py-4 whitespace-nowrap">
                      <span className="inline-flex items-center justify-center w-8 h-8 rounded-full bg-gray-100 text-gray-700 font-semibold text-sm">
                        {rank}
                      </span>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="flex items-center gap-2">
                        <div className="w-2 h-2 rounded-full bg-red-500"></div>
                        <span className="text-sm font-medium text-gray-900">{error.name}</span>
                      </div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <span className="text-sm font-bold text-red-600">
                        {error.count.toLocaleString()}
                      </span>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="flex items-center gap-2">
                        <span className="text-sm font-semibold text-gray-900">
                          {error.percentage.toFixed(1)}%
                        </span>
                        <div className="w-24 bg-gray-200 rounded-full h-2">
                          <div
                            className="bg-red-500 h-2 rounded-full"
                            style={{ width: `${Math.min(error.percentage, 100)}%` }}
                          ></div>
                        </div>
                      </div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <span className={`px-3 py-1 inline-flex text-xs leading-5 font-semibold rounded-full ${impactColor}`}>
                        {impactLevel}
                      </span>
                    </td>
                  </tr>
                );
              })
            ) : (
              <tr>
                <td colSpan={5} className="px-6 py-8 text-center text-gray-500">
                  No error data available
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {currentData.length > 0 && (
        <div className="px-6 py-3 bg-gray-50 border-t border-gray-200">
          <p className="text-sm text-gray-600">
            Showing {currentData.length} error {activeTab === 'type' ? 'types' : 'sources'}
          </p>
        </div>
      )}
    </div>
  );
};
