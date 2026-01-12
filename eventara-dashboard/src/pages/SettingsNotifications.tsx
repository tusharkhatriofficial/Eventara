import React from 'react';

export const SettingsNotifications: React.FC = () => {
  return (
    <div>
      <h1 className="text-2xl font-bold">Notifications & Integrations</h1>
      <p className="text-sm text-gray-500 mt-1">Configure notification channels (Slack, Email, Webhooks) used by alert rules.</p>

      <div className="mt-6 bg-white border rounded p-6">
        <div className="text-gray-700">No notification channels configured yet.</div>
        <div className="mt-4">
          <button className="px-4 py-2 bg-blue-600 text-white rounded">Add channel</button>
        </div>
      </div>
    </div>
  );
};
