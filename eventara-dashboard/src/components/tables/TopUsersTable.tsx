import { UserActivity } from '../../types';

interface TopUsersTableProps {
  topActiveUsers: UserActivity[];
}

export const TopUsersTable: React.FC<TopUsersTableProps> = ({ topActiveUsers }) => {
  const getMedalEmoji = (rank: number) => {
    switch (rank) {
      case 1: return '🥇';
      case 2: return '🥈';
      case 3: return '🥉';
      default: return rank;
    }
  };

  return (
    <div className="bg-white rounded-lg shadow-sm border border-gray-200">
      <div className="p-6 border-b border-gray-200">
        <h3 className="text-lg font-semibold text-gray-900">Top Active Users</h3>
        <p className="text-sm text-gray-500 mt-1">Users with highest event activity</p>
      </div>
      
      <div className="overflow-x-auto">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider w-20">
                Rank
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                User ID
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Event Count
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Activity Level
              </th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {topActiveUsers.length > 0 ? (
              topActiveUsers.map((user, index) => {
                const rank = index + 1;
                const activityLevel = user.eventCount > 100 ? 'Very High' :
                                     user.eventCount > 50 ? 'High' :
                                     user.eventCount > 20 ? 'Medium' : 'Low';
                const activityColor = user.eventCount > 100 ? 'text-purple-700 bg-purple-100' :
                                     user.eventCount > 50 ? 'text-blue-700 bg-blue-100' :
                                     user.eventCount > 20 ? 'text-green-700 bg-green-100' :
                                     'text-gray-700 bg-gray-100';
                
                return (
                  <tr key={user.userId} className="hover:bg-gray-50">
                    <td className="px-6 py-4 whitespace-nowrap">
                      <span className="text-2xl">
                        {getMedalEmoji(rank)}
                      </span>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="flex items-center">
                        <div className="flex-shrink-0 h-10 w-10 bg-blue-100 rounded-full flex items-center justify-center">
                          <span className="text-blue-600 font-semibold text-sm">
                            {user.userId.substring(0, 2).toUpperCase()}
                          </span>
                        </div>
                        <div className="ml-4">
                          <div className="text-sm font-medium text-gray-900">{user.userId}</div>
                        </div>
                      </div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="text-sm font-bold text-gray-900">
                        {user.eventCount.toLocaleString()}
                      </div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <span className={`px-3 py-1 inline-flex text-xs leading-5 font-semibold rounded-full ${activityColor}`}>
                        {activityLevel}
                      </span>
                    </td>
                  </tr>
                );
              })
            ) : (
              <tr>
                <td colSpan={4} className="px-6 py-8 text-center text-gray-500">
                  No user activity data available
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {topActiveUsers.length > 0 && (
        <div className="px-6 py-3 bg-gray-50 border-t border-gray-200">
          <p className="text-sm text-gray-600">
            Showing top {topActiveUsers.length} most active users
          </p>
        </div>
      )}
    </div>
  );
};
