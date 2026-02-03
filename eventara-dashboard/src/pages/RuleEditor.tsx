import React, { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { createRule, getRule, updateRule, testRule } from '../utils/api/rules';
import { CreateRuleRequest, UpdateRuleRequest, TestRuleRequest, RuleResponse, MetricType, Condition } from '../types/rules';
import { listChannels } from '../utils/api/notifications';
import type { NotificationChannel } from '../types/notifications';

// Options derived from backend enums (kept in sync with src/main/java/com/eventara/rule/enums)
const METRIC_TYPES: MetricType[] = [
  // Error Metrics
  'ERROR_RATE', 'TOTAL_ERRORS', 'SOURCE_ERROR_RATE',
  // Performance Metrics
  'AVG_LATENCY', 'P50_LATENCY', 'P95_LATENCY', 'P99_LATENCY', 'MAX_LATENCY', 'MIN_LATENCY',
  // Throughput Metrics
  'EVENTS_PER_SECOND', 'EVENTS_PER_MINUTE', 'EVENTS_PER_HOUR', 'EVENTS_PER_DAY', 'PEAK_THROUGHPUT', 'AVG_THROUGHPUT_1H', 'AVG_THROUGHPUT_24H',
  // Time Window Metrics
  'EVENTS_LAST_1_MINUTE', 'EVENTS_LAST_5_MINUTES', 'EVENTS_LAST_15_MINUTES', 'EVENTS_LAST_1_HOUR', 'EVENTS_LAST_24_HOURS', 'TOTAL_EVENTS',
  // Summary Metrics
  'UNIQUE_SOURCES', 'UNIQUE_EVENT_TYPES', 'UNIQUE_USERS', 'SYSTEM_HEALTH',
  // User Metrics
  'ACTIVE_USERS_LAST_1_HOUR', 'ACTIVE_USERS_LAST_24_HOURS', 'TOTAL_UNIQUE_USERS',
  // Phase 1: Source/Type Specific
  'EVENT_TYPE_COUNT',
  // Phase 2: Ratios
  'EVENT_RATIO',
  // Phase 3: Rate of Change
  'ERROR_RATE_CHANGE', 'LATENCY_CHANGE', 'THROUGHPUT_CHANGE', 'SPIKE_DETECTION'
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
  const [advancedMode, setAdvancedMode] = useState<'simple' | 'composite' | 'ratio' | 'change'>('simple');

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

      // Check if this is a composite rule
      if (cfg && cfg.conditions) {
        // Validate composite rule
        if (!cfg.operator) throw new Error('operator is required for composite rules');
        if (!Array.isArray(cfg.conditions)) throw new Error('conditions must be an array');
        if (cfg.conditions.length === 0) throw new Error('conditions array cannot be empty');

        // Validate each condition
        cfg.conditions.forEach((cond: any, idx: number) => {
          if (!cond.metricType) throw new Error(`metricType is required in condition ${idx + 1}`);
          if (!cond.condition) throw new Error(`condition is required in condition ${idx + 1}`);
          if (cond.value === undefined || cond.value === '') throw new Error(`value is required in condition ${idx + 1}`);
        });
      }
      // Check if this is an event ratio rule
      else if (cfg && cfg.metricType === 'EVENT_RATIO') {
        if (!cfg.numeratorEventType) throw new Error('numeratorEventType is required for EVENT_RATIO');
        if (!cfg.denominatorEventType) throw new Error('denominatorEventType is required for EVENT_RATIO');
        if (!cfg.condition) throw new Error('condition is required');
        if (cfg.thresholdValue === undefined || cfg.thresholdValue === '') throw new Error('thresholdValue is required');
      }
      // Simple threshold rule
      else {
        if (!cfg || !cfg.metricType) throw new Error('metricType is required');
        if (!cfg.condition) throw new Error('condition is required');
        if (cfg.thresholdValue === undefined || cfg.thresholdValue === '') throw new Error('thresholdValue is required');
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

      // Check if this is a composite rule
      if (cfg && cfg.conditions) {
        // Validate composite rule
        if (!cfg.operator) throw new Error('operator is required for composite rules');
        if (!Array.isArray(cfg.conditions)) throw new Error('conditions must be an array');
        if (cfg.conditions.length === 0) throw new Error('conditions array cannot be empty');

        // Validate each condition
        cfg.conditions.forEach((cond: any, idx: number) => {
          if (!cond.metricType) throw new Error(`metricType is required in condition ${idx + 1}`);
          if (!cond.condition) throw new Error(`condition is required in condition ${idx + 1}`);
          if (cond.value === undefined || cond.value === '') throw new Error(`value is required in condition ${idx + 1}`);
        });
      }
      // Check if this is an event ratio rule
      else if (cfg && cfg.metricType === 'EVENT_RATIO') {
        if (!cfg.numeratorEventType) throw new Error('numeratorEventType is required for EVENT_RATIO');
        if (!cfg.denominatorEventType) throw new Error('denominatorEventType is required for EVENT_RATIO');
        if (!cfg.condition) throw new Error('condition is required');
        if (cfg.thresholdValue === undefined || cfg.thresholdValue === '') throw new Error('thresholdValue is required');
      }
      // Simple threshold rule
      else {
        if (!cfg || !cfg.metricType) throw new Error('metricType is required');
        if (!cfg.condition) throw new Error('condition is required');
        if (cfg.thresholdValue === undefined || cfg.thresholdValue === '') throw new Error('thresholdValue is required');
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

  // Helper functions for composite conditions
  function addCondition() {
    const conditions = (form as any).ruleConfig?.conditions || [];
    onField('ruleConfig', {
      ...(form as any).ruleConfig,
      conditions: [...conditions, { metricType: '', condition: '', value: 0 }]
    });
  }

  function removeCondition(index: number) {
    const conditions = (form as any).ruleConfig?.conditions || [];
    onField('ruleConfig', {
      ...(form as any).ruleConfig,
      conditions: conditions.filter((_: any, i: number) => i !== index)
    });
  }

  function updateCondition(index: number, field: string, value: any) {
    const conditions = [...((form as any).ruleConfig?.conditions || [])];
    conditions[index] = { ...conditions[index], [field]: value };
    onField('ruleConfig', {
      ...(form as any).ruleConfig,
      conditions
    });
  }

  // Helper function to add/remove items from arrays (for filters)
  function addToArray(field: string, value: string) {
    const current = (form as any).ruleConfig?.[field] || [];
    if (value && !current.includes(value)) {
      onField('ruleConfig', {
        ...(form as any).ruleConfig,
        [field]: [...current, value]
      });
    }
  }

  function removeFromArray(field: string, value: string) {
    const current = (form as any).ruleConfig?.[field] || [];
    onField('ruleConfig', {
      ...(form as any).ruleConfig,
      [field]: current.filter((v: string) => v !== value)
    });
  }

  // Detect advanced mode from loaded config
  useEffect(() => {
    const config = (form as any).ruleConfig;
    if (config) {
      if (config.conditions && config.operator) {
        setAdvancedMode('composite');
      } else if (config.metricType === 'EVENT_RATIO') {
        setAdvancedMode('ratio');
      } else if (
        config.metricType === 'ERROR_RATE_CHANGE' ||
        config.metricType === 'LATENCY_CHANGE' ||
        config.metricType === 'THROUGHPUT_CHANGE' ||
        config.metricType === 'SPIKE_DETECTION'
      ) {
        setAdvancedMode('change');
      } else {
        setAdvancedMode('simple');
      }
    }
  }, [(form as any).ruleConfig?.metricType, (form as any).ruleConfig?.operator]);


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

        {/* Alert Suppression & Rate Limiting */}
        <div className="card-gradient p-8 space-y-6">
          <div className="flex items-center gap-3 pb-6 border-b border-dark-100">
            <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-orange-500 to-red-600 flex items-center justify-center text-white">
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M18.364 18.364A9 9 0 005.636 5.636m12.728 12.728A9 9 0 015.636 5.636m12.728 12.728L5.636 5.636" />
              </svg>
            </div>
            <h3 className="text-lg font-semibold text-dark-900">Alert Suppression & Rate Limiting</h3>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-semibold text-dark-700 mb-2">Suppression Window (minutes)</label>
              <input
                type="number"
                min="0"
                className="input-modern"
                placeholder="30"
                value={(form as any).suppressionWindowMinutes ?? ''}
                onChange={(e) => onField('suppressionWindowMinutes', e.target.value ? Number(e.target.value) : null)}
              />
              <p className="text-xs text-dark-500 mt-2">
                Time window to suppress duplicate alerts. Set to 0 to disable suppression. Default: 30 minutes
              </p>
            </div>

            <div>
              <label className="block text-sm font-semibold text-dark-700 mb-2">Max Alerts Per Hour</label>
              <input
                type="number"
                min="1"
                className="input-modern"
                placeholder="10"
                value={(form as any).maxAlertsPerHour ?? ''}
                onChange={(e) => onField('maxAlertsPerHour', e.target.value ? Number(e.target.value) : null)}
              />
              <p className="text-xs text-dark-500 mt-2">
                Maximum alerts this rule can trigger per hour. Default: 10
              </p>
            </div>
          </div>
        </div>

        {/* Advanced Notification Settings (Collapsible) */}
        <div className="card-gradient p-8">
          <details className="group">
            <summary className="cursor-pointer flex items-center gap-3 list-none">
              <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-indigo-500 to-purple-600 flex items-center justify-center text-white">
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6V4m0 2a2 2 0 100 4m0-4a2 2 0 110 4m-6 8a2 2 0 100-4m0 4a2 2 0 110-4m0 4v2m0-6V4m6 6v10m6-2a2 2 0 100-4m0 4a2 2 0 110-4m0 4v2m0-6V4" />
                </svg>
              </div>
              <div className="flex-1">
                <h3 className="text-lg font-semibold text-dark-900">Advanced Notification Settings</h3>
                <p className="text-xs text-dark-500 mt-1">Optional: Custom templates, retry policies, and escalation</p>
              </div>
              <svg className="w-5 h-5 text-dark-400 transition-transform group-open:rotate-180" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
              </svg>
            </summary>

            <div className="mt-6 pt-6 border-t border-dark-100 space-y-4">
              <div>
                <label className="block text-sm font-semibold text-dark-700 mb-2">Custom Message Template</label>
                <textarea
                  className="input-modern min-h-[100px] resize-none font-mono text-sm"
                  placeholder="Override default alert message. Use placeholders: {ruleName}, {severity}, {thresholdValue}, {actualValue}, {triggeredAt}"
                  value={(form as any).notificationConfig?.messageTemplate ?? ''}
                  onChange={(e) => onField('notificationConfig', {
                    ...(form as any).notificationConfig,
                    messageTemplate: e.target.value
                  })}
                />
                <p className="text-xs text-dark-500 mt-2">
                  💡 Available placeholders: <code className="text-primary-600 bg-primary-50 px-1 rounded">{'{ruleName}'}</code>, <code className="text-primary-600 bg-primary-50 px-1 rounded">{'{severity}'}</code>, <code className="text-primary-600 bg-primary-50 px-1 rounded">{'{thresholdValue}'}</code>, <code className="text-primary-600 bg-primary-50 px-1 rounded">{'{actualValue}'}</code>, <code className="text-primary-600 bg-primary-50 px-1 rounded">{'{triggeredAt}'}</code>
                </p>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-semibold text-dark-700 mb-2">Retry Attempts</label>
                  <input
                    type="number"
                    min="0"
                    max="5"
                    className="input-modern"
                    placeholder="3"
                    value={(form as any).notificationConfig?.retryAttempts ?? ''}
                    onChange={(e) => onField('notificationConfig', {
                      ...(form as any).notificationConfig,
                      retryAttempts: e.target.value ? Number(e.target.value) : null
                    })}
                  />
                  <p className="text-xs text-dark-500 mt-2">Number of retries if notification fails (0-5)</p>
                </div>

                <div>
                  <label className="block text-sm font-semibold text-dark-700 mb-2">Escalation Delay (minutes)</label>
                  <input
                    type="number"
                    min="0"
                    className="input-modern"
                    placeholder="15"
                    value={(form as any).notificationConfig?.escalationDelayMinutes ?? ''}
                    onChange={(e) => onField('notificationConfig', {
                      ...(form as any).notificationConfig,
                      escalationDelayMinutes: e.target.value ? Number(e.target.value) : null
                    })}
                  />
                  <p className="text-xs text-dark-500 mt-2">Delay before escalating to secondary channels</p>
                </div>
              </div>
            </div>
          </details>
        </div>

        {/* Metadata */}
        <div className="card-gradient p-8">
          <div>
            <label className="block text-sm font-semibold text-dark-700 mb-2">Created By (Optional)</label>
            <input
              type="text"
              className="input-modern"
              placeholder="username or email"
              value={(form as any).createdBy ?? ''}
              onChange={(e) => onField('createdBy', e.target.value)}
            />
            <p className="text-xs text-dark-500 mt-2">
              Track who created this rule (auto-filled if you have authentication)
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
              {/* Advanced Mode Selector */}
              <div>
                <label className="block text-sm font-semibold text-dark-700 mb-2">Rule Mode</label>
                <select
                  className="input-modern"
                  value={advancedMode}
                  onChange={(e) => {
                    setAdvancedMode(e.target.value as any);
                    // Reset config when changing modes with appropriate defaults
                    if (e.target.value === 'composite') {
                      // Initialize composite mode with operator and empty conditions
                      onField('ruleConfig', { operator: 'AND', conditions: [] });
                    } else {
                      onField('ruleConfig', {});
                    }
                  }}
                >
                  <option value="simple">Simple Threshold</option>
                  <option value="composite">Composite (AND/OR)</option>
                  <option value="ratio">Event Ratio (Conversion Rate)</option>
                  <option value="change">Rate of Change</option>
                </select>
                <p className="text-xs text-dark-500 mt-2">
                  {advancedMode === 'simple' && 'Monitor a single metric against a threshold'}
                  {advancedMode === 'composite' && 'Combine multiple conditions with AND/OR operators'}
                  {advancedMode === 'ratio' && 'Compare two event types (e.g., success/total for conversion rate)'}
                  {advancedMode === 'change' && 'Detect % change vs previous window (spike/trend detection)'}
                </p>
              </div>

              {/* SIMPLE MODE */}
              {advancedMode === 'simple' && (
                <>
                  <div>
                    <label className="block text-sm font-semibold text-dark-700 mb-2">Metric Type *</label>
                    <select
                      className="input-modern"
                      value={(form as any).ruleConfig?.metricType || ''}
                      onChange={(e) => onField('ruleConfig', { ...(form as any).ruleConfig, metricType: e.target.value })}
                    >
                      <option value="">Select metric to monitor</option>
                      {METRIC_TYPES.filter(m => m !== 'EVENT_RATIO' && !m.includes('_CHANGE') && m !== 'SPIKE_DETECTION').map((m) => (
                        <option key={m} value={m}>{m.replaceAll('_', ' ')}</option>
                      ))}
                    </select>
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
                  </div>

                  <div>
                    <label className="block text-sm font-semibold text-dark-700 mb-2">Threshold Value *</label>
                    <input
                      type="number"
                      step="any"
                      className="input-modern"
                      placeholder="e.g., 5.0"
                      value={(form as any).ruleConfig?.thresholdValue || ''}
                      onChange={(e) => onField('ruleConfig', { ...(form as any).ruleConfig, thresholdValue: parseFloat(e.target.value) || 0 })}
                    />
                  </div>

                  {/* Source Filter (Phase 1) */}
                  <div>
                    <label className="block text-sm font-semibold text-dark-700 mb-2">
                      Filter by Source (Optional) <span className="text-xs text-primary-600 font-normal">• Phase 1</span>
                    </label>
                    <div className="space-y-2">
                      <div className="flex gap-2">
                        <input
                          type="text"
                          className="input-modern flex-1"
                          placeholder="e.g., auth-service, payment-service"
                          onKeyDown={(e) => {
                            if (e.key === 'Enter') {
                              e.preventDefault();
                              const value = (e.target as HTMLInputElement).value.trim();
                              if (value) {
                                addToArray('sourceFilter', value);
                                (e.target as HTMLInputElement).value = '';
                              }
                            }
                          }}
                        />
                      </div>
                      {((form as any).ruleConfig?.sourceFilter || []).length > 0 && (
                        <div className="flex flex-wrap gap-2">
                          {((form as any).ruleConfig?.sourceFilter || []).map((source: string) => (
                            <span
                              key={source}
                              className="inline-flex items-center gap-1 px-3 py-1 bg-primary-100 text-primary-700 rounded-lg text-sm"
                            >
                              {source}
                              <button
                                onClick={() => removeFromArray('sourceFilter', source)}
                                className="hover:text-primary-900"
                              >
                                ×
                              </button>
                            </span>
                          ))}
                        </div>
                      )}
                    </div>
                    <p className="text-xs text-dark-500 mt-2">
                      Press Enter to add. Leave empty to monitor all sources. Enables source-specific threshold evaluation.
                    </p>
                  </div>

                  {/* Event Type Filter (Phase 1) */}
                  <div>
                    <label className="block text-sm font-semibold text-dark-700 mb-2">
                      Filter by Event Type (Optional) <span className="text-xs text-primary-600 font-normal">• Phase 1</span>
                    </label>
                    <div className="space-y-2">
                      <div className="flex gap-2">
                        <input
                          type="text"
                          className="input-modern flex-1"
                          placeholder="e.g., user.login, payment.failed"
                          onKeyDown={(e) => {
                            if (e.key === 'Enter') {
                              e.preventDefault();
                              const value = (e.target as HTMLInputElement).value.trim();
                              if (value) {
                                addToArray('eventTypeFilter', value);
                                (e.target as HTMLInputElement).value = '';
                              }
                            }
                          }}
                        />
                      </div>
                      {((form as any).ruleConfig?.eventTypeFilter || []).length > 0 && (
                        <div className="flex flex-wrap gap-2">
                          {((form as any).ruleConfig?.eventTypeFilter || []).map((type: string) => (
                            <span
                              key={type}
                              className="inline-flex items-center gap-1 px-3 py-1 bg-purple-100 text-purple-700 rounded-lg text-sm"
                            >
                              {type}
                              <button
                                onClick={() => removeFromArray('eventTypeFilter', type)}
                                className="hover:text-purple-900"
                              >
                                ×
                              </button>
                            </span>
                          ))}
                        </div>
                      )}
                    </div>
                    <p className="text-xs text-dark-500 mt-2">
                      Press Enter to add. Leave empty to monitor all event types.
                    </p>
                  </div>
                </>
              )}

              {/* COMPOSITE MODE (Phase 2) */}
              {advancedMode === 'composite' && (
                <>
                  <div>
                    <label className="block text-sm font-semibold text-dark-700 mb-2">
                      Operator <span className="text-xs text-primary-600 font-normal">• Phase 2</span>
                    </label>
                    <select
                      className="input-modern"
                      value={(form as any).ruleConfig?.operator || 'AND'}
                      onChange={(e) => onField('ruleConfig', { ...(form as any).ruleConfig, operator: e.target.value })}
                    >
                      <option value="AND">AND (all conditions must be true)</option>
                      <option value="OR">OR (any condition can be true)</option>
                    </select>
                  </div>

                  <div>
                    <label className="block text-sm font-semibold text-dark-700 mb-3">Conditions</label>
                    <div className="space-y-3">
                      {((form as any).ruleConfig?.conditions || []).map((cond: any, idx: number) => (
                        <div key={idx} className="p-4 bg-dark-50 rounded-xl space-y-3">
                          <div className="flex items-center justify-between mb-2">
                            <span className="text-xs font-semibold text-dark-600">Condition {idx + 1}</span>
                            <button
                              className="text-red-600 hover:text-red-700 text-sm"
                              onClick={() => removeCondition(idx)}
                            >
                              Remove
                            </button>
                          </div>
                          <div className="grid grid-cols-3 gap-3">
                            <div>
                              <label className="block text-xs font-medium text-dark-600 mb-1">Metric</label>
                              <select
                                className="input-modern"
                                value={cond.metricType || ''}
                                onChange={(e) => updateCondition(idx, 'metricType', e.target.value)}
                              >
                                <option value="">Select</option>
                                {METRIC_TYPES.filter(m => m !== 'EVENT_RATIO' && !m.includes('_CHANGE') && m !== 'SPIKE_DETECTION').map(m => (
                                  <option key={m} value={m}>{m.replaceAll('_', ' ')}</option>
                                ))}
                              </select>
                            </div>
                            <div>
                              <label className="block text-xs font-medium text-dark-600 mb-1">Condition</label>
                              <select
                                className="input-modern"
                                value={cond.condition || ''}
                                onChange={(e) => updateCondition(idx, 'condition', e.target.value)}
                              >
                                <option value="">Select</option>
                                {CONDITIONS.map(c => (
                                  <option key={c} value={c}>{c.replaceAll('_', ' ')}</option>
                                ))}
                              </select>
                            </div>
                            <div>
                              <label className="block text-xs font-medium text-dark-600 mb-1">Value</label>
                              <input
                                type="number"
                                step="any"
                                className="input-modern"
                                placeholder="0"
                                value={cond.value || ''}
                                onChange={(e) => updateCondition(idx, 'value', parseFloat(e.target.value) || 0)}
                              />
                            </div>
                          </div>
                        </div>
                      ))}
                    </div>
                    <button
                      className="btn-secondary mt-3 w-full"
                      onClick={addCondition}
                    >
                      + Add Condition
                    </button>
                  </div>

                  {/* Source Filter for Composite */}
                  <div>
                    <label className="block text-sm font-semibold text-dark-700 mb-2">
                      Apply to Sources (Optional)
                    </label>
                    <div className="space-y-2">
                      <div className="flex gap-2">
                        <input
                          type="text"
                          className="input-modern flex-1"
                          placeholder="e.g., auth-service"
                          onKeyDown={(e) => {
                            if (e.key === 'Enter') {
                              e.preventDefault();
                              const value = (e.target as HTMLInputElement).value.trim();
                              if (value) {
                                addToArray('sourceFilter', value);
                                (e.target as HTMLInputElement).value = '';
                              }
                            }
                          }}
                        />
                      </div>
                      {((form as any).ruleConfig?.sourceFilter || []).length > 0 && (
                        <div className="flex flex-wrap gap-2">
                          {((form as any).ruleConfig?.sourceFilter || []).map((source: string) => (
                            <span
                              key={source}
                              className="inline-flex items-center gap-1 px-3 py-1 bg-primary-100 text-primary-700 rounded-lg text-sm"
                            >
                              {source}
                              <button
                                onClick={() => removeFromArray('sourceFilter', source)}
                                className="hover:text-primary-900"
                              >
                                ×
                              </button>
                            </span>
                          ))}
                        </div>
                      )}
                    </div>
                    <p className="text-xs text-dark-500 mt-2">
                      Filters apply to ALL conditions above
                    </p>
                  </div>
                </>
              )}

              {/* EVENT RATIO MODE (Phase 2) */}
              {advancedMode === 'ratio' && (
                <>
                  <div>
                    <label className="block text-sm font-semibold text-dark-700 mb-2">
                      Numerator Event Type * <span className="text-xs text-primary-600 font-normal">• Phase 2</span>
                    </label>
                    <input
                      type="text"
                      className="input-modern"
                      placeholder="e.g., user.login.success"
                      value={(form as any).ruleConfig?.numeratorEventType || ''}
                      onChange={(e) => onField('ruleConfig', {
                        ...(form as any).ruleConfig,
                        metricType: 'EVENT_RATIO',
                        numeratorEventType: e.target.value
                      })}
                    />
                  </div>

                  <div>
                    <label className="block text-sm font-semibold text-dark-700 mb-2">Denominator Event Type *</label>
                    <input
                      type="text"
                      className="input-modern"
                      placeholder="e.g., user.login.attempted"
                      value={(form as any).ruleConfig?.denominatorEventType || ''}
                      onChange={(e) => onField('ruleConfig', {
                        ...(form as any).ruleConfig,
                        metricType: 'EVENT_RATIO',
                        denominatorEventType: e.target.value
                      })}
                    />
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
                  </div>

                  <div>
                    <label className="block text-sm font-semibold text-dark-700 mb-2">Ratio Threshold * (0.0 - 1.0)</label>
                    <input
                      type="number"
                      step="0.01"
                      min="0"
                      max="1"
                      className="input-modern"
                      placeholder="e.g., 0.8 for 80%"
                      value={(form as any).ruleConfig?.thresholdValue || ''}
                      onChange={(e) => onField('ruleConfig', { ...(form as any).ruleConfig, thresholdValue: parseFloat(e.target.value) || 0 })}
                    />
                    <p className="text-xs text-dark-500 mt-2">
                      Example: 0.8 = 80% success rate threshold
                    </p>
                  </div>

                  <div>
                    <label className="block text-sm font-semibold text-dark-700 mb-2">Minimum Denominator Events</label>
                    <input
                      type="number"
                      min="1"
                      className="input-modern"
                      placeholder="10"
                      value={(form as any).ruleConfig?.minDenominatorEvents || ''}
                      onChange={(e) => onField('ruleConfig', {
                        ...(form as any).ruleConfig,
                        minDenominatorEvents: parseInt(e.target.value) || 10
                      })}
                    />
                    <p className="text-xs text-dark-500 mt-2">
                      Minimum events in denominator before evaluation (prevents false positives)
                    </p>
                  </div>
                </>
              )}

              {/* RATE OF CHANGE MODE (Phase 3) */}
              {advancedMode === 'change' && (
                <>
                  <div>
                    <label className="block text-sm font-semibold text-dark-700 mb-2">
                      Change Metric Type * <span className="text-xs text-primary-600 font-normal">• Phase 3</span>
                    </label>
                    <select
                      className="input-modern"
                      value={(form as any).ruleConfig?.metricType || ''}
                      onChange={(e) => onField('ruleConfig', { ...(form as any).ruleConfig, metricType: e.target.value })}
                    >
                      <option value="">Select metric</option>
                      <option value="ERROR_RATE_CHANGE">Error Rate Change</option>
                      <option value="LATENCY_CHANGE">Latency Change</option>
                      <option value="THROUGHPUT_CHANGE">Throughput Change</option>
                      <option value="SPIKE_DETECTION">Traffic Spike Detection</option>
                    </select>
                    <p className="text-xs text-dark-500 mt-2">
                      Compares current window vs previous window of same duration
                    </p>
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
                  </div>

                  <div>
                    <label className="block text-sm font-semibold text-dark-700 mb-2">% Change Threshold *</label>
                    <input
                      type="number"
                      step="any"
                      className="input-modern"
                      placeholder="e.g., 50 for +50% increase"
                      value={(form as any).ruleConfig?.thresholdValue || ''}
                      onChange={(e) => onField('ruleConfig', { ...(form as any).ruleConfig, thresholdValue: parseFloat(e.target.value) || 0 })}
                    />
                    <p className="text-xs text-dark-500 mt-2">
                      Positive = increase detection, Negative = decrease detection
                      <br />
                      <span className="text-primary-600">Example: 50 = alert on +50% increase, -30 = alert on -30% decrease</span>
                    </p>
                  </div>

                  {/* Source Filter for Rate of Change */}
                  <div>
                    <label className="block text-sm font-semibold text-dark-700 mb-2">
                      Monitor Specific Source (Optional)
                    </label>
                    <div className="space-y-2">
                      <div className="flex gap-2">
                        <input
                          type="text"
                          className="input-modern flex-1"
                          placeholder="e.g., payment-service"
                          onKeyDown={(e) => {
                            if (e.key === 'Enter') {
                              e.preventDefault();
                              const value = (e.target as HTMLInputElement).value.trim();
                              if (value) {
                                addToArray('sourceFilter', value);
                                (e.target as HTMLInputElement).value = '';
                              }
                            }
                          }}
                        />
                      </div>
                      {((form as any).ruleConfig?.sourceFilter || []).length > 0 && (
                        <div className="flex flex-wrap gap-2">
                          {((form as any).ruleConfig?.sourceFilter || []).map((source: string) => (
                            <span
                              key={source}
                              className="inline-flex items-center gap-1 px-3 py-1 bg-primary-100 text-primary-700 rounded-lg text-sm"
                            >
                              {source}
                              <button
                                onClick={() => removeFromArray('sourceFilter', source)}
                                className="hover:text-primary-900"
                              >
                                ×
                              </button>
                            </span>
                          ))}
                        </div>
                      )}
                    </div>
                  </div>
                </>
              )}

              {/* Common Fields for All Modes - Time Window removed, rules are now evaluated in real-time */}
              <div>
                <label className="block text-sm font-semibold text-dark-700 mb-2 flex items-center gap-2">
                  Cooldown Period (minutes)
                  <span className="text-xs px-2 py-0.5 bg-green-100 text-green-700 rounded font-normal">✓ Distributed • Phase 4</span>
                </label>
                <input
                  type="number"
                  min={0}
                  className="input-modern"
                  placeholder="5"
                  value={(form as any).ruleConfig?.cooldownMinutes || ''}
                  onChange={(e) => onField('ruleConfig', {
                    ...(form as any).ruleConfig,
                    cooldownMinutes: e.target.value ? Number(e.target.value) : undefined
                  })}
                />
                <p className="text-xs text-dark-500 mt-2">
                  Minimum time between alert triggers (default: 5 minutes). Shared across all backend instances via Redis TTL.
                </p>
              </div>
            </div>
          )}

          {jsonMode && (
            <div>
              <label className="block text-sm font-semibold text-dark-700 mb-2">Rule Config (JSON)</label>
              <textarea
                className="input-modern font-mono text-sm min-h-[400px] resize-none"
                placeholder='{\n  "metricType": "ERROR_RATE",\n  "condition": "GREATER_THAN",\n  "thresholdValue": "5.0",\n  "cooldownMinutes": 5\n}'
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
