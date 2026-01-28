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
      <div className="flex items-center justify-center h-[60vh]">
        <div className="text-center">
          <div className="w-12 h-12 rounded-full border-4 border-slate-200 border-t-primary-600 animate-spin mx-auto mb-4"></div>
          <p className="text-slate-600">Loading channels...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="page-header">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          <div className="flex items-center gap-4">
            <div className="page-icon">
              <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
              </svg>
            </div>
            <div>
              <h1 className="page-title">Notifications & Integrations</h1>
              <p className="page-subtitle">Configure notification channels used by alert rules</p>
            </div>
          </div>
          <button onClick={handleCreateChannel} className="btn-primary">
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
            </svg>
            Add Channel
          </button>
        </div>
      </div>

      {/* Stats Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4 lg:gap-6">
        <div className="stat-card">
          <div className="flex items-center gap-4">
            <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-primary-500 to-primary-600 flex items-center justify-center shadow-lg shadow-primary-500/25">
              <svg className="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
              </svg>
            </div>
            <div>
              <div className="text-2xl font-bold text-slate-900">{stats.total}</div>
              <div className="text-sm text-slate-500">Total Channels</div>
            </div>
          </div>
        </div>

        <div className="stat-card">
          <div className="flex items-center gap-4">
            <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-success-500 to-success-600 flex items-center justify-center shadow-lg shadow-success-500/25">
              <svg className="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            </div>
            <div>
              <div className="text-2xl font-bold text-slate-900">{stats.enabled}</div>
              <div className="text-sm text-slate-500">Active Channels</div>
            </div>
          </div>
        </div>

        <div className="stat-card">
          <div className="flex items-center gap-4">
            <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-violet-500 to-violet-600 flex items-center justify-center shadow-lg shadow-violet-500/25">
              <svg className="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
              </svg>
            </div>
            <div>
              <div className="text-2xl font-bold text-slate-900">{stats.webhooks}</div>
              <div className="text-sm text-slate-500">Webhooks</div>
            </div>
          </div>
        </div>
      </div>

      {/* Channels List */}
      <div className="card p-6">
        <h2 className="text-lg font-semibold text-slate-900 mb-6">Notification Channels</h2>

        {channels.length === 0 ? (
          <div className="empty-state py-12">
            <div className="empty-state-icon">
              <svg className="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
              </svg>
            </div>
            <h3 className="empty-state-title">No notification channels configured</h3>
            <p className="empty-state-description">Add your first channel to start receiving alerts</p>
            <button onClick={handleCreateChannel} className="btn-primary">
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
              </svg>
              Add Channel
            </button>
          </div>
        ) : (
          <div className="space-y-4">
            {channels.map((channel) => (
              <div
                key={channel.id}
                className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 p-5 bg-slate-50 rounded-xl border border-slate-100 hover:bg-slate-100 transition-colors"
              >
                <div className="flex items-center gap-4">
                  <div className={`w-10 h-10 rounded-lg flex items-center justify-center text-lg ${channel.channelType === 'WEBHOOK' ? 'bg-violet-100 text-violet-600' :
                      channel.channelType === 'EMAIL' ? 'bg-blue-100 text-blue-600' :
                        channel.channelType === 'SLACK' ? 'bg-success-100 text-success-600' :
                          'bg-slate-100 text-slate-600'
                    }`}>
                    {channel.channelType === 'WEBHOOK' && '🔗'}
                    {channel.channelType === 'EMAIL' && '📧'}
                    {channel.channelType === 'SLACK' && '💬'}
                    {channel.channelType === 'SMS' && '📱'}
                  </div>
                  <div>
                    <div className="flex flex-wrap items-center gap-2">
                      <h3 className="font-semibold text-slate-900">{channel.name}</h3>
                      <span className={`badge ${channel.enabled ? 'badge-success' : 'badge-neutral'}`}>
                        {channel.enabled ? 'Active' : 'Disabled'}
                      </span>
                      <span className="badge badge-info">{channel.channelType}</span>
                    </div>
                    <p className="text-sm text-slate-500 mt-1">{channel.description || 'No description'}</p>
                    <div className="flex flex-wrap items-center gap-4 mt-2 text-xs text-slate-400">
                      <span>📤 {channel.totalSent || 0} sent</span>
                      <span>❌ {channel.totalFailed || 0} failed</span>
                      {channel.lastUsedAt && (
                        <span>🕒 Last used: {new Date(channel.lastUsedAt).toLocaleString()}</span>
                      )}
                    </div>
                  </div>
                </div>

                <div className="flex items-center gap-2 sm:flex-shrink-0">
                  <button
                    onClick={() => handleTest(channel.id)}
                    className="btn-secondary btn-sm"
                  >
                    Test
                  </button>
                  <button
                    onClick={() => handleDelete(channel.id)}
                    className="btn btn-sm text-error-600 border-2 border-error-200 hover:bg-error-50"
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
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" onClick={(e) => e.stopPropagation()}>
        <form onSubmit={handleSubmit}>
          {/* Modal Header */}
          <div className="modal-header">
            <h2 className="text-xl font-bold text-slate-900">Add Notification Channel</h2>
            <p className="text-sm text-slate-500 mt-1">Configure a new notification channel for alerts</p>
          </div>

          {/* Modal Body */}
          <div className="modal-body space-y-5">
            {/* Channel Type */}
            <div>
              <label className="input-label">Channel Type</label>
              <select
                value={channelType}
                onChange={(e) => setChannelType(e.target.value as ChannelType)}
                className="select"
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
              <label className="input-label">Channel Name</label>
              <input
                type="text"
                value={name}
                onChange={(e) => setName(e.target.value)}
                className="input"
                placeholder="production-alerts"
                required
              />
            </div>

            {/* Description */}
            <div>
              <label className="input-label">Description (Optional)</label>
              <textarea
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                className="textarea"
                placeholder="Send critical alerts to Slack"
                rows={2}
              />
            </div>

            {/* Webhook-specific fields */}
            {channelType === 'WEBHOOK' && (
              <>
                <div>
                  <label className="input-label">Webhook URL</label>
                  <input
                    type="url"
                    value={webhookUrl}
                    onChange={(e) => setWebhookUrl(e.target.value)}
                    className="input"
                    placeholder="https://hooks.slack.com/services/..."
                    required={channelType === 'WEBHOOK'}
                  />
                  <p className="input-hint">The URL that will receive POST requests when alerts fire</p>
                </div>

                <div>
                  <label className="input-label">Auth Token (Optional)</label>
                  <input
                    type="password"
                    value={authToken}
                    onChange={(e) => setAuthToken(e.target.value)}
                    className="input"
                    placeholder="Bearer token for authentication"
                  />
                  <p className="input-hint">Will be sent as Authorization: Bearer token header</p>
                </div>
              </>
            )}
          </div>

          {/* Modal Footer */}
          <div className="modal-footer">
            <button
              type="button"
              onClick={onClose}
              className="btn-secondary"
              disabled={submitting}
            >
              Cancel
            </button>
            <button
              type="submit"
              className="btn-primary"
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
