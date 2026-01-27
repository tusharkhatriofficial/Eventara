import React, { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { createRule, getRule, updateRule, testRule } from '../utils/api/rules';
import { CreateRuleRequest, UpdateRuleRequest, TestRuleRequest, RuleResponse, MetricType, Condition } from '../types/rules';
import { listChannels } from '../utils/api/notifications';
import type { NotificationChannel } from '../types/notifications';

// Options derived from backend enums (kept in sync with src/main/java/com/eventara/rule/enums)
const METRIC_TYPES: MetricType[] = [
  'ERROR_RATE', 'TOTAL_ERRORS', 'AVG_LATENCY', 'P50_LATENCY', 'P95_LATENCY', 'P99_LATENCY', 'MAX_LATENCY', 'MIN_LATENCY',
  'EVENTS_PER_SECOND', 'EVENTS_PER_MINUTE', 'EVENTS_PER_HOUR', 'EVENTS_PER_DAY', 'PEAK_THROUGHPUT', 'AVG_THROUGHPUT_1H', 'AVG_THROUGHPUT_24H',
  'EVENTS_LAST_1_MINUTE', 'EVENTS_LAST_5_MINUTES', 'EVENTS_LAST_15_MINUTES', 'EVENTS_LAST_1_HOUR', 'EVENTS_LAST_24_HOURS', 'TOTAL_EVENTS',
  'UNIQUE_SOURCES', 'UNIQUE_EVENT_TYPES', 'UNIQUE_USERS', 'SYSTEM_HEALTH', 'ACTIVE_USERS_LAST_1_HOUR', 'ACTIVE_USERS_LAST_24_HOURS', 'TOTAL_UNIQUE_USERS'
];

const CONDITIONS: Condition[] = [
  'GREATER_THAN', 'LESS_THAN', 'EQUALS', 'GREATER_THAN_OR_EQUAL', 'LESS_THAN_OR_EQUAL', 'NOT_EQUALS', 'BETWEEN', 'NOT_BETWEEN'
];
import { RuleTestModal } from '../components/rules/RuleTestModal';

export const RuleEditor: React.FC = () => {
  const { id } = useParams<{ id?: string }>();
  const isEdit = !!id && id !== 'new';
  const navigate = useNavigate();

  const [form, setForm] = useState<CreateRuleRequest | UpdateRuleRequest>({
    name: '',
    description: '',
    ruleType: 'THRESHOLD',
    ruleConfig: {},
    severity: 'CRITICAL',
    priority: 0,
  } as any);
  const [jsonMode, setJsonMode] = useState(false);
  const [jsonText, setJsonText] = useState('');
  const [loading, setLoading] = useState(false);
  const [testResult, setTestResult] = useState<any>(null);
  const [error, setError] = useState<string | null>(null);
  const [isTestOpen, setIsTestOpen] = useState(false);
  const [channels, setChannels] = useState<NotificationChannel[]>([]);

  useEffect(() => {
    if (isEdit && id) fetchRule(Number(id));
    loadChannels();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  async function loadChannels() {
    try {
      const data = await listChannels({ enabled: true });
      setChannels(data);
    } catch (e) {
      console.error('Failed to load notification channels:', e);
    }
  }

  async function fetchRule(id: number) {
    try {
      setLoading(true);
      const r: RuleResponse = await getRule(id);
      setForm({
        name: r.name,
        description: r.description,
        ruleType: r.ruleType,
        ruleConfig: r.ruleConfig,
        severity: r.severity,
        priority: r.priority,
        notificationChannels: r.notificationChannels,
        notificationConfig: r.notificationConfig,
        suppressionWindowMinutes: r.suppressionWindowMinutes,
        maxAlertsPerHour: r.maxAlertsPerHour,
      } as any);
      setJsonText(JSON.stringify(r.ruleConfig || {}, null, 2));
    } catch (e: any) {
      setError(e?.message || 'Failed to load rule');
    } finally {
      setLoading(false);
    }
  }

  async function onTest() {
    try {
      setError(null);

      const cfg = jsonMode ? JSON.parse(jsonText) : (form as any).ruleConfig;
      // Client-side validation that mirrors backend requirements
      if (!cfg || !cfg.metricType) throw new Error('metricType is required');
      if (!cfg.condition) throw new Error('condition is required');
      if (cfg.thresholdValue === undefined || cfg.thresholdValue === '') throw new Error('thresholdValue is required');
      if (cfg.timeWindowMinutes !== undefined && (!Number.isInteger(cfg.timeWindowMinutes) || cfg.timeWindowMinutes <= 0)) {
        throw new Error('timeWindowMinutes must be a positive integer');
      }

      const payload: TestRuleRequest = {
        ruleType: (form as any).ruleType,
        ruleConfig: cfg,
        severity: (form as any).severity,
        priority: (form as any).priority,
        name: (form as any).name,
      };
      const res = await testRule(payload);
      setTestResult(res);
    } catch (e: any) {
      setError(e?.message || 'Test failed');
    }
  }

  async function onSave() {
    try {
      setLoading(true);
      setError(null);

      const cfg = jsonMode ? JSON.parse(jsonText) : (form as any).ruleConfig;
      // Basic validation before save
      if (!cfg || !cfg.metricType) throw new Error('metricType is required');
      if (!cfg.condition) throw new Error('condition is required');
      if (cfg.thresholdValue === undefined || cfg.thresholdValue === '') throw new Error('thresholdValue is required');
      if (cfg.timeWindowMinutes !== undefined && (!Number.isInteger(cfg.timeWindowMinutes) || cfg.timeWindowMinutes <= 0)) {
        throw new Error('timeWindowMinutes must be a positive integer');
      }

      const payload: CreateRuleRequest | UpdateRuleRequest = {
        ...(form as any),
        ruleConfig: cfg,
      } as any;

      if (isEdit && id) {
        await updateRule(Number(id), payload as UpdateRuleRequest);
      } else {
        await createRule(payload as CreateRuleRequest);
      }
      navigate('/alerts/rules');
    } catch (e: any) {
      setError(e?.message || 'Save failed');
    } finally {
      setLoading(false);
    }
  }

  function onField<K extends keyof typeof form>(key: K, value: any) {
    setForm((f: any) => {
      const next = { ...f, [key]: value } as any;
      // Keep JSON editor in sync when editing the form mode
      if (key === 'ruleConfig' && !jsonMode) {
        try {
          setJsonText(JSON.stringify(next.ruleConfig || {}, null, 2));
        } catch (e) {
          // ignore
        }
      }
      return next;
    });
  }

  return (
    <div className="space-y-6 animate-fade-in">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <div className="flex items-center gap-3">
            <button
              onClick={() => navigate('/alerts/rules')}
              className="p-2 hover:bg-dark-100 rounded-xl transition-colors"
            >
              <svg className="w-5 h-5 text-dark-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
              </svg>
            </button>
            <div>
              <h1 className="text-3xl font-bold text-gradient">{isEdit ? 'Edit Rule' : 'Create New Rule'}</h1>
              <p className="text-sm text-dark-600 mt-1">Define the rule criteria, thresholds, and notification behavior</p>
            </div>
          </div>
        </div>
        <div className="flex items-center gap-3">
          <button onClick={onTest} className="btn-secondary flex items-center gap-2">
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            Test Rule
          </button>
          <button
            onClick={onSave}
            disabled={loading}
            className="btn-primary flex items-center gap-2"
          >
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
            </svg>
            {loading ? 'Saving...' : 'Save Rule'}
          </button>
        </div>
      </div>

      {error && (
        <div className="bg-gradient-to-r from-red-50 to-rose-50 border-l-4 border-red-500 p-6 rounded-xl flex items-start gap-4 animate-slide-down">
          <svg className="w-6 h-6 text-red-500 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          <div>
            <h3 className="font-semibold text-red-900">Error</h3>
            <p className="text-red-700 mt-1">{error}</p>
          </div>
          <button onClick={() => setError(null)} className="ml-auto text-red-400 hover:text-red-600">
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Basic Info Card */}
        <div className="card-gradient p-8 space-y-6">
          <div className="flex items-center gap-3 pb-6 border-b border-dark-100">
            <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-primary-500 to-primary-700 flex items-center justify-center text-white">
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            </div>
            <h3 className="text-lg font-semibold text-dark-900">Basic Information</h3>
          </div>

          <div>
            <label className="block text-sm font-semibold text-dark-700 mb-2">Rule Name *</label>
            <input
              className="input-modern"
              placeholder="e.g., High Error Rate Alert"
              value={(form as any).name}
              onChange={(e) => onField('name', e.target.value)}
            />
          </div>

          <div>
            <label className="block text-sm font-semibold text-dark-700 mb-2">Description</label>
            <textarea
              className="input-modern min-h-[100px] resize-none"
              placeholder="Describe what this rule monitors and when it triggers"
              value={(form as any).description}
              onChange={(e) => onField('description', e.target.value)}
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-semibold text-dark-700 mb-2">Rule Type *</label>
              <select
                className="input-modern"
                value={(form as any).ruleType}
                onChange={(e) => onField('ruleType', e.target.value)}
              >
                <option value="THRESHOLD">Threshold</option>
                <option value="PATTERN">Pattern</option>
                <option value="ANOMALY">Anomaly</option>
                <option value="CEP">CEP</option>
              </select>
            </div>

            <div>
              <label className="block text-sm font-semibold text-dark-700 mb-2">Severity *</label>
              <select
                className="input-modern"
                value={(form as any).severity}
                onChange={(e) => onField('severity', e.target.value)}
              >
                <option value="CRITICAL">Critical</option>
                <option value="WARNING">Warning</option>
                <option value="INFO">Info</option>
              </select>
            </div>
          </div>

          <div>
            <label className="block text-sm font-semibold text-dark-700 mb-2">Priority</label>
            <input
              type="number"
              className="input-modern"
              placeholder="0 = highest priority"
              value={(form as any).priority ?? 0}
              onChange={(e) => onField('priority', Number(e.target.value))}
            />
            <p className="text-xs text-dark-500 mt-2">Lower numbers have higher priority (0 is highest)</p>
          </div>

          {/* Notification Channels  */}
          <div>
            <label className="block text-sm font-semibold text-dark-700 mb-2">
              Notification Channels
            </label>
            {channels.length === 0 ? (
              <div className="input-modern bg-gray-50 text-gray-500 flex items-center justify-between">
                <span>No channels configured</span>
                <a
                  href="/settings/notifications"
                  className="text-primary-600 text-sm font-medium hover:underline"
                >
                  Add Channel
                </a>
              </div>
            ) : (
              <div className="space-y-3">
                {channels.map((channel) => (
                  <label
                    key={channel.id}
                    className="flex items-center gap-3 p-3 border border-gray-200 rounded-lg hover:bg-gray-50 cursor-pointer transition-colors"
                  >
                    <input
                      type="checkbox"
                      checked={((form as any).notificationChannels || []).includes(channel.name)}
                      onChange={(e) => {
                        const current = (form as any).notificationChannels || [];
                        const updated = e.target.checked
                          ? [...current, channel.name]
                          : current.filter((n: string) => n !== channel.name);
                        onField('notificationChannels', updated);
                      }}
                      className="w-4 h-4 text-primary-600 border-gray-300 rounded focus:ring-primary-500"
                    />
                    <div className="flex-1">
                      <div className="flex items-center gap-2">
                        <span className="font-medium text-gray-900">{channel.name}</span>
                        <span className="px-2 py-0.5 text-xs font-medium bg-blue-100 text-blue-700 rounded">
                          {channel.channelType}
                        </span>
                      </div>
                      {channel.description && (
                        <p className="text-xs text-gray-600 mt-1">{channel.description}</p>
                      )}
                    </div>
                  </label>
                ))}
              </div>
            )}
            <p className="text-xs text-dark-500 mt-2">
              Select channels to receive notifications when this rule triggers
            </p>
          </div>
        </div>

        {/* Rule Configuration Card */}
        <div className="card-gradient p-8 space-y-6">
          <div className="flex items-center justify-between pb-6 border-b border-dark-100">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-purple-500 to-purple-700 flex items-center justify-center text-white">
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
                </svg>
              </div>
              <h3 className="text-lg font-semibold text-dark-900">Configuration</h3>
            </div>
            <div className="flex items-center gap-2">
              <button
                onClick={() => setJsonMode(false)}
                className={`px-4 py-2 text-sm font-medium rounded-lg transition-all ${!jsonMode
                  ? 'bg-primary-600 text-white shadow-lg shadow-primary-500/30'
                  : 'bg-dark-100 text-dark-600 hover:bg-dark-200'
                  }`}
              >
                Form
              </button>
              <button
                onClick={() => setJsonMode(true)}
                className={`px-4 py-2 text-sm font-medium rounded-lg transition-all ${jsonMode
                  ? 'bg-primary-600 text-white shadow-lg shadow-primary-500/30'
                  : 'bg-dark-100 text-dark-600 hover:bg-dark-200'
                  }`}
              >
                JSON
              </button>
            </div>
          </div>

          {!jsonMode && (
            <div className="space-y-5">
              <div>
                <label className="block text-sm font-semibold text-dark-700 mb-2">Metric Type *</label>
                <select
                  className="input-modern"
                  value={(form as any).ruleConfig?.metricType || ''}
                  onChange={(e) => onField('ruleConfig', { ...(form as any).ruleConfig, metricType: e.target.value })}
                >
                  <option value="">Select metric to monitor</option>
                  {METRIC_TYPES.map((m) => (
                    <option key={m} value={m}>{m.replaceAll('_', ' ')}</option>
                  ))}
                </select>
                {!((form as any).ruleConfig?.metricType) && (
                  <p className="text-xs text-red-600 mt-2 flex items-center gap-1">
                    <svg className="w-3 h-3" fill="currentColor" viewBox="0 0 20 20">
                      <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
                    </svg>
                    Metric type is required
                  </p>
                )}
              </div>

              <div>
                <label className="block text-sm font-semibold text-dark-700 mb-2">Condition *</label>
                <select
                  className="input-modern"
                  value={(form as any).ruleConfig?.condition || ''}
                  onChange={(e) => onField('ruleConfig', { ...(form as any).ruleConfig, condition: e.target.value })}
                >
                  <option value="">Select condition</option>
                  {CONDITIONS.map((c) => (
                    <option key={c} value={c}>{c.replaceAll('_', ' ')}</option>
                  ))}
                </select>
                {!((form as any).ruleConfig?.condition) && (
                  <p className="text-xs text-red-600 mt-2 flex items-center gap-1">
                    <svg className="w-3 h-3" fill="currentColor" viewBox="0 0 20 20">
                      <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
                    </svg>
                    Condition is required
                  </p>
                )}
              </div>

              <div>
                <label className="block text-sm font-semibold text-dark-700 mb-2">Threshold Value *</label>
                <input
                  className="input-modern"
                  placeholder="e.g., 100, 5.0, or 'high'"
                  value={(form as any).ruleConfig?.thresholdValue || ''}
                  onChange={(e) => onField('ruleConfig', { ...(form as any).ruleConfig, thresholdValue: e.target.value })}
                />
                {((form as any).ruleConfig?.thresholdValue === undefined || (form as any).ruleConfig?.thresholdValue === '') && (
                  <p className="text-xs text-dark-500 mt-2">The threshold value that triggers the alert</p>
                )}
              </div>

              <div>
                <label className="block text-sm font-semibold text-dark-700 mb-2">Time Window (minutes)</label>
                <input
                  type="number"
                  min={1}
                  className="input-modern"
                  placeholder="e.g., 5"
                  value={(form as any).ruleConfig?.timeWindowMinutes || ''}
                  onChange={(e) => onField('ruleConfig', { ...(form as any).ruleConfig, timeWindowMinutes: e.target.value ? Number(e.target.value) : undefined })}
                />
                {((form as any).ruleConfig?.timeWindowMinutes !== undefined && ((form as any).ruleConfig?.timeWindowMinutes <= 0 || !Number.isInteger((form as any).ruleConfig?.timeWindowMinutes))) && (
                  <p className="text-xs text-red-600 mt-2 flex items-center gap-1">
                    <svg className="w-3 h-3" fill="currentColor" viewBox="0 0 20 20">
                      <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
                    </svg>
                    Must be a positive integer
                  </p>
                )}
                {(!((form as any).ruleConfig?.timeWindowMinutes) || ((form as any).ruleConfig?.timeWindowMinutes > 0 && Number.isInteger((form as any).ruleConfig?.timeWindowMinutes))) && (
                  <p className="text-xs text-dark-500 mt-2">Evaluation time window in minutes</p>
                )}
              </div>
            </div>
          )}

          {jsonMode && (
            <div>
              <label className="block text-sm font-semibold text-dark-700 mb-2">Rule Config (JSON)</label>
              <textarea
                className="input-modern font-mono text-sm min-h-[400px] resize-none"
                placeholder='{\n  "metricType": "ERROR_RATE",\n  "condition": "GREATER_THAN",\n  "thresholdValue": "5.0",\n  "timeWindowMinutes": 5\n}'
                value={jsonText}
                onChange={(e) => setJsonText(e.target.value)}
              />
            </div>
          )}
        </div>
      </div>

      {/* Test Result */}
      {testResult && (
        <div className="card-gradient p-8 animate-scale-in">
          <div className="flex items-start gap-4">
            <div className={`w-12 h-12 rounded-xl flex items-center justify-center flex-shrink-0 ${testResult.success
              ? 'bg-gradient-to-br from-emerald-500 to-green-600 shadow-lg shadow-emerald-500/30'
              : 'bg-gradient-to-br from-red-500 to-rose-600 shadow-lg shadow-red-500/30'
              }`}>
              {testResult.success ? (
                <svg className="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
              ) : (
                <svg className="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
              )}
            </div>
            <div className="flex-1">
              <h3 className="text-lg font-semibold text-dark-900">Test Result: {testResult.success ? 'Success' : 'Failed'}</h3>
              {testResult.message && <p className="text-sm text-dark-600 mt-2">{testResult.message}</p>}
              {testResult.errors && (
                <pre className="mt-3 p-4 bg-red-50 border border-red-200 rounded-xl text-xs text-red-700 overflow-x-auto scrollbar-thin">
                  {JSON.stringify(testResult.errors, null, 2)}
                </pre>
              )}
              <button
                onClick={() => setIsTestOpen(true)}
                className="btn-secondary mt-4"
              >
                View Detailed Results
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Test result modal */}
      <RuleTestModal isOpen={isTestOpen} onClose={() => setIsTestOpen(false)} result={testResult} />
    </div>
  );
};
