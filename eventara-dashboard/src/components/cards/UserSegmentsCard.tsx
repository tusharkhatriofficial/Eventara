import { UserActivity } from '../../types';

interface UserSegmentsCardProps {
  topActiveUsers: UserActivity[];
}

export const UserSegmentsCard: React.FC<UserSegmentsCardProps> = ({ topActiveUsers }) => {
  const segments = {
    powerUsers: topActiveUsers.filter(u => u.eventCount > 100).length,
    activeUsers: topActiveUsers.filter(u => u.eventCount > 20 && u.eventCount <= 100).length,
    casualUsers: topActiveUsers.filter(u => u.eventCount <= 20).length,
  };

  const total = topActiveUsers.length;

  const SegmentBar = ({ label, count, color }: { label: string; count: number; color: string }) => {
    const percentage = total > 0 ? (count / total) * 100 : 0;
    return (
      <div className="mb-4">
        <div className="flex items-center justify-between mb-2">
          <span className="text-sm font-medium text-gray-700">{label}</span>
          <span className="text-sm font-semibold text-gray-900">{count} ({percentage.toFixed(1)}%)</span>
        </div>
        <div className="w-full bg-gray-200 rounded-full h-3">
          <div
            className={`${color} h-3 rounded-full transition-all duration-500`}
            style={{ width: `${percentage}%` }}
          ></div>
        </div>
      </div>
    );
  };

  return (
    <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-200">
      <h3 className="text-lg font-semibold text-gray-900 mb-4">User Segments</h3>
      <p className="text-sm text-gray-600 mb-6">Users categorized by activity level</p>
      
      <SegmentBar
        label="Power Users (>100 events)"
        count={segments.powerUsers}
        color="bg-purple-600"
      />
      
      <SegmentBar
        label="Active Users (20-100 events)"
        count={segments.activeUsers}
        color="bg-blue-600"
      />
      
      <SegmentBar
        label="Casual Users (<20 events)"
        count={segments.casualUsers}
        color="bg-gray-400"
      />

      <div className="mt-6 pt-6 border-t border-gray-200">
        <div className="flex items-center justify-between">
          <span className="text-sm font-medium text-gray-600">Total Users</span>
          <span className="text-2xl font-bold text-gray-900">{total}</span>
        </div>
      </div>
    </div>
  );
};
