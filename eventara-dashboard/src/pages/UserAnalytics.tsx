import { ComprehensiveMetrics } from '../types';
import { UserStatsOverview } from '../components/cards/UserStatsOverview';
import { TopUsersTable } from '../components/tables/TopUsersTable';
import { UserActivityDistributionChart } from '../components/charts/UserActivityDistributionChart';
import { UserEngagementGauge } from '../components/charts/UserEngagementGauge';
import { UserSegmentsCard } from '../components/cards/UserSegmentsCard';

interface UserAnalyticsProps {
  metrics: ComprehensiveMetrics | null;
}

export const UserAnalytics: React.FC<UserAnalyticsProps> = ({ metrics }) => {
  if (!metrics) {
    return (
      <div className="flex items-center justify-center h-full">
        <div className="text-center">
          <div className="inline-block animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mb-4"></div>
          <p className="text-gray-600">Loading user analytics...</p>
        </div>
      </div>
    );
  }

  const hasUserData = metrics.userMetrics.topActiveUsers.length > 0;

  return (
    <div className="space-y-6">
      {/* Page Title */}
      <div>
        <h1 className="text-2xl font-bold text-gray-900">User Analytics</h1>
        <p className="text-sm text-gray-500 mt-1">
          Monitor user activity, engagement, and behavior patterns
        </p>
      </div>

      {/* Stats Overview */}
      <UserStatsOverview userMetrics={metrics.userMetrics} />

      {/* Charts Row */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <UserEngagementGauge userMetrics={metrics.userMetrics} />
        <UserSegmentsCard topActiveUsers={metrics.userMetrics.topActiveUsers} />
      </div>

      {/* Activity Distribution Chart */}
      {hasUserData && (
        <UserActivityDistributionChart topActiveUsers={metrics.userMetrics.topActiveUsers} />
      )}

      {/* Top Active Users Table */}
      <TopUsersTable topActiveUsers={metrics.userMetrics.topActiveUsers} />

      {/* No Data State */}
      {!hasUserData && (
        <div className="bg-blue-50 border border-blue-200 rounded-lg p-8 text-center">
          <svg className="w-16 h-16 text-blue-400 mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" />
          </svg>
          <h3 className="text-lg font-semibold text-gray-900 mb-2">No User Data Yet</h3>
          <p className="text-gray-600 mb-4">
            Send events with userId to see user analytics and activity patterns
          </p>
          <div className="bg-white p-4 rounded border border-gray-200 text-left inline-block">
            <p className="text-xs text-gray-500 mb-2">Example:</p>
            <code className="text-xs text-gray-800"> 
              curl -X POST http://localhost:8080/api/v1/events \<br/>
              &nbsp;&nbsp;-H "Content-Type: application/json" \<br/>
              &nbsp;&nbsp;-d '&#123;"eventType":"user.action","userId":"user_123"&#125;'
            </code>
          </div>
        </div>
      )}
    </div>
  );
};
