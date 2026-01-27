import React, { useState, useEffect } from 'react';
import {
  listChannels,
  createChannel,
  deleteChannel,
  testChannel,
  getChannelStats
} from '../utils/api/notifications';
import type { NotificationChannel, ChannelType } from '../types/notifications';

export const SettingsNotifications: React.FC = () => {
  const [channels, setChannels] = useState<NotificationChannel[]>([]);
  const [loading, setLoading] = useState(true);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [stats, setStats] = useState({ total: 0, enabled: 0, webhooks: 0 });

  useEffect(() => {
    loadChannels();
    loadStats();
  }, []);

  const loadChannels = async () => {
    try {
      const data = await listChannels();
      setChannels(data);
    } catch (error) {
      console.error('Failed to load channels:', error);
    } finally {
      setLoading(false);
    }
  };

  const loadStats = async () => {
    try {
      const data = await getChannelStats();
      setStats(data);
    } catch (error) {
      console.error('Failed to load stats:', error);
    }
  };

  const handleCreateChannel = () => {
    setShowCreateModal(true);
  };

  const handleDelete = async (id: number) => {
    if (!confirm('Are you sure you want to delete this channel?')) return;

    try {
      await deleteChannel(id);
      await loadChannels();
      await loadStats();
    } catch (error) {
      alert('Failed to delete channel');
    }
  };

  const handleTest = async (id: number) => {
    try {
      const result = await testChannel(id);
      if (result.status === 'SENT') {
        alert('Test notification sent successfully!');
      } else {
        alert(`Test failed: ${result.errorMessage || 'Unknown error'}`);
      }
    } catch (error) {
      alert('Failed to send test notification');
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-full">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  return (
    <div className="space-y-6 animate-fade-in">
      {/* Header */}
      <div className="card-gradient p-8">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-bold text-gradient">Notifications & Integrations</h1>
            <p className="text-sm text-dark-600 mt-1">
              Configure notification channels (Slack, Email, Webhooks) used by alert rules
            </p>
          </div>
          <button
            onClick={handleCreateChannel}
            className="btn-primary"
          >
            + Add Channel
          </button>
        </div>
      </div>

      {/* Stats Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div className="card-gradient p-6">
          <div className="flex items-center gap-4">
            <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-blue-500 to-blue-600 flex items-center justify-center shadow-lg shadow-blue-500/30">
              <svg className="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2  2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
              </svg>
            </div>
            <div>
              <div className="text-2xl font-bold text-dark-900">{stats.total}</div>
              <div className="text-sm text-dark-600">Total Channels</div>
            </div>
          </div>
        </div>

        <div className="card-gradient p-6">
          <div className="flex items-center gap-4">
            <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-green-500 to-green-600 flex items-center justify-center shadow-lg shadow-green-500/30">
              <svg className="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            </div>
            <div>
              <div className="text-2xl font-bold text-dark-900">{stats.enabled}</div>
              <div className="text-sm text-dark-600">Active Channels</div>
            </div>
          </div>
        </div>

        <div className="card-gradient p-6">
          <div className="flex items-center gap-4">
            <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-purple-500 to-purple-600 flex items-center justify-center shadow-lg shadow-purple-500/30">
              <svg className="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
              </svg>
            </div>
            <div>
              <div className="text-2xl font-bold text-dark-900">{stats.webhooks}</div>
              <div className="text-sm text-dark-600">Webhooks</div>
            </div>
          </div>
        </div>
      </div>

      {/* Channels List */}
      <div className="card-gradient p-6">
        <h2 className="text-xl font-bold text-dark-900 mb-6">Notification Channels</h2>

        {channels.length === 0 ? (
          <div className="text-center py-12">
            <div className="w-16 h-16 rounded-2xl bg-gray-100 flex items-center justify-center mx-auto mb-4">
              <svg className="w-8 h-8 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
              </svg>
            </div>
            <h3 className="text-lg font-semibold text-gray-900 mb-2">No notification channels configured yet</h3>
            <p className="text-gray-600 mb-4">Add your first channel to start receiving alerts</p>
            <button onClick={handleCreateChannel} className="btn-primary">
              + Add Channel
            </button>
          </div>
        ) : (
          <div className="space-y-4">
            {channels.map((channel) => (
              <div
                key={channel.id}
                className="flex items-center justify-between p-5 bg-white rounded-xl border border-gray-200 hover:shadow-lg transition-all duration-300"
              >
                <div className="flex items-center gap-4">
                  <div className={`w-10 h-10 rounded-lg flex items-center justify-center ${channel.channelType === 'WEBHOOK' ? 'bg-purple-100 text-purple-600' :
                    channel.channelType === 'EMAIL' ? 'bg-blue-100 text-blue-600' :
                      channel.channelType === 'SLACK' ? 'bg-green-100 text-green-600' :
                        'bg-gray-100 text-gray-600'
                    }`}>
                    {channel.channelType === 'WEBHOOK' && '🔗'}
                    {channel.channelType === 'EMAIL' && '📧'}
                    {channel.channelType === 'SLACK' && '💬'}
                    {channel.channelType === 'SMS' && '📱'}
                  </div>
                  <div>
                    <div className="flex items-center gap-2">
                      <h3 className="font-semibold text-gray-900">{channel.name}</h3>
                      <span className={`px-2 py-1 rounded-full text-xs font-medium ${channel.enabled ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-600'
                        }`}>
                        {channel.enabled ? 'Active' : 'Disabled'}
                      </span>
                      <span className="px-2 py-1 rounded-full text-xs font-medium bg-blue-100 text-blue-700">
                        {channel.channelType}
                      </span>
                    </div>
                    <p className="text-sm text-gray-600 mt-1">{channel.description || 'No description'}</p>
                    <div className="flex items-center gap-4 mt-2 text-xs text-gray-500">
                      <span>📤 {channel.totalSent || 0} sent</span>
                      <span>❌ {channel.totalFailed || 0} failed</span>
                      {channel.lastUsedAt && (
                        <span>🕒 Last used: {new Date(channel.lastUsedAt).toLocaleString()}</span>
                      )}
                    </div>
                  </div>
                </div>

                <div className="flex items-center gap-2">
                  <button
                    onClick={() => handleTest(channel.id)}
                    className="px-3 py-1.5 text-sm border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors"
                  >
                    Test
                  </button>
                  <button
                    onClick={() => handleDelete(channel.id)}
                    className="px-3 py-1.5 text-sm text-red-600 border border-red-300 rounded-lg hover:bg-red-50 transition-colors"
                  >
                    Delete
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Create Modal */}
      {showCreateModal && (
        <CreateChannelModal
          onClose={() => setShowCreateModal(false)}
          onSuccess={() => {
            setShowCreateModal(false);
            loadChannels();
            loadStats();
          }}
        />
      )}
    </div>
  );
};

/* Create Channel Modal Component */
interface CreateChannelModalProps {
  onClose: () => void;
  onSuccess: () => void;
}

const CreateChannelModal: React.FC<CreateChannelModalProps> = ({ onClose, onSuccess }) => {
  const [channelType, setChannelType] = useState<ChannelType>('WEBHOOK');
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [webhookUrl, setWebhookUrl] = useState('');
  const [authToken, setAuthToken] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitting(true);

    try {
      const config: Record<string, any> = {};

      if (channelType === 'WEBHOOK') {
        config.url = webhookUrl;
        if (authToken) {
          config.authToken = authToken;
        }
      }

      await createChannel({
        channelType,
        name,
        description,
        enabled: true,
        config,
      });

      onSuccess();
    } catch (error: any) {
      console.error('Failed to create channel:', error);
      const errorMessage = error?.message || error?.error || 'Failed to create channel. Please check your inputs.';
      alert(errorMessage);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-2xl shadow-2xl max-w-lg w-full mx-4 max-h-[90vh] overflow-y-auto">
        <form onSubmit={handleSubmit}>
          {/* Modal Header */}
          <div className="p-6 border-b border-gray-200">
            <h2 className="text-2xl font-bold text-gray-900">Add Notification Channel</h2>
            <p className="text-sm text-gray-600 mt-1">Configure a new notification channel for alerts</p>
          </div>

          {/* Modal Body */}
          <div className="p-6 space-y-5">
            {/* Channel Type */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">Channel Type</label>
              <select
                value={channelType}
                onChange={(e) => setChannelType(e.target.value as ChannelType)}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                required
              >
                <option value="WEBHOOK">Webhook</option>
                <option value="EMAIL">Email (Coming Soon)</option>
                <option value="SLACK">Slack (Coming Soon)</option>
                <option value="SMS">SMS (Coming Soon)</option>
              </select>
            </div>

            {/* Name */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">Channel Name</label>
              <input
                type="text"
                value={name}
                onChange={(e) => setName(e.target.value)}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                placeholder="production-alerts"
                required
              />
            </div>

            {/* Description */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">Description (Optional)</label>
              <textarea
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                placeholder="Send critical alerts to Slack"
                rows={2}
              />
            </div>

            {/* Webhook-specific fields */}
            {channelType === 'WEBHOOK' && (
              <>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">Webhook URL</label>
                  <input
                    type="url"
                    value={webhookUrl}
                    onChange={(e) => setWebhookUrl(e.target.value)}
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                    placeholder="https://hooks.slack.com/services/..."
                    required={channelType === 'WEBHOOK'}
                  />
                  <p className="text-xs text-gray-500 mt-1">
                    The URL that will receive POST requests when alerts fire
                  </p>
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">Auth Token (Optional)</label>
                  <input
                    type="password"
                    value={authToken}
                    onChange={(e) => setAuthToken(e.target.value)}
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                    placeholder="Bearer token for authentication"
                  />
                  <p className="text-xs text-gray-500 mt-1">
                    Will be sent as Authorization: Bearer token header
                  </p>
                </div>
              </>
            )}
          </div>

          {/* Modal Footer */}
          <div className="p-6 border-t border-gray-200 flex items-center justify-end gap-3">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 text-gray-700 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors"
              disabled={submitting}
            >
              Cancel
            </button>
            <button
              type="submit"
              className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors disabled:opacity-50"
              disabled={submitting}
            >
              {submitting ? 'Creating...' : 'Create Channel'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
